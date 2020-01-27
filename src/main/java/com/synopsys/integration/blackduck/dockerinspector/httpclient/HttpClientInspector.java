/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2020 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.dockerinspector.httpclient;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.dockerinspector.blackduckclient.BlackDuckClient;
import com.synopsys.integration.blackduck.dockerinspector.output.ContainerFilesystemFilename;
import com.synopsys.integration.blackduck.dockerinspector.output.ImageTarFilename;
import com.synopsys.integration.blackduck.dockerinspector.output.Output;
import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.ProgramPaths;
import com.synopsys.integration.blackduck.imageinspector.api.name.Names;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.bdio.BdioReader;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;

@Component
public class HttpClientInspector {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Config config;

    @Autowired
    private BlackDuckClient blackDuckClient;

    @Autowired
    private ProgramPaths programPaths;

    @Autowired
    private ImageTarFilename dockerTarfile;

    @Autowired
    private List<ImageInspectorClient> imageInspectorClients;

    @Autowired
    private ContainerPaths containerPaths;

    @Autowired
    private Output output;

    @Autowired
    private Gson gson;

    @Autowired
    private ContainerFilesystemFilename containerFilesystemFilename;

    public int getBdio() throws IntegrationException {
        final ImageInspectorClient imageInspectorClient = chooseImageInspectorClient();
        try {
            output.ensureOutputDirIsWriteable();
            final File finalDockerTarfile = prepareDockerTarfile(imageInspectorClient);
            final String containerFileSystemFilename = containerFilesystemFilename.deriveContainerFilesystemFilename();
            final String dockerTarFilePathInContainer = containerPaths.getContainerPathToTargetFile(finalDockerTarfile.getCanonicalPath());
            String containerFileSystemPathInContainer = null;
            if (config.isOutputIncludeContainerfilesystem() || config.isOutputIncludeSquashedImage()) {
                containerFileSystemPathInContainer = containerPaths.getContainerPathToOutputFile(containerFileSystemFilename);
            }
            final String bdioString = imageInspectorClient.getBdio(finalDockerTarfile.getCanonicalPath(), dockerTarFilePathInContainer, config.getDockerImageRepo(), config.getDockerImageTag(),
                containerFileSystemPathInContainer, config.getContainerFileSystemExcludedPaths(),
                config.isOrganizeComponentsByLayer(), config.isIncludeRemovedComponents(),
                config.isCleanupWorkingDir(), config.getDockerPlatformTopLayerId());
            logger.trace(String.format("bdioString: %s", bdioString));
            final SimpleBdioDocument bdioDocument = toBdioDocument(bdioString);
            adjustBdio(bdioDocument);
            final File bdioFile = output.addOutputToOutputDir(bdioDocument);
            if (config.isUploadBdio()) {
                blackDuckClient.uploadBdio(bdioFile, bdioDocument.getBillOfMaterials().spdxName);
            }
            cleanup();
            return 0;
        } catch (final IOException e) {
            throw new IntegrationException(e.getMessage(), e);
        }
    }

    private void adjustBdio(final SimpleBdioDocument bdioDocument) {
        if (StringUtils.isNotBlank(config.getBlackDuckProjectName())) {
            bdioDocument.getProject().name = config.getBlackDuckProjectName();
        }
        if (StringUtils.isNotBlank(config.getBlackDuckProjectVersion())) {
            bdioDocument.getProject().version = config.getBlackDuckProjectVersion();
        }
        if (StringUtils.isNotBlank(config.getBlackDuckCodelocationName())) {
            bdioDocument.getBillOfMaterials().spdxName = config.getBlackDuckCodelocationName();
        } else if (StringUtils.isNotBlank(config.getBlackDuckCodelocationPrefix())) {
            bdioDocument.getBillOfMaterials().spdxName = String.format("%s_%s", config.getBlackDuckCodelocationPrefix(), bdioDocument.getBillOfMaterials().spdxName);
        }
    }

    private SimpleBdioDocument toBdioDocument(final String bdioString) throws IOException {
        final Reader reader = new StringReader(bdioString);
        SimpleBdioDocument doc = null;
        try (BdioReader bdioReader = new BdioReader(gson, reader)) {
            doc = bdioReader.readSimpleBdioDocument();
            return doc;
        }
    }

    private File prepareDockerTarfile(final ImageInspectorClient imageInspectorClient) throws IOException, IntegrationException {
        final File givenDockerTarfile = dockerTarfile.deriveDockerTarFileFromConfig();
        final File finalDockerTarfile = imageInspectorClient.copyTarfileToSharedDir(givenDockerTarfile);
        return finalDockerTarfile;
    }

    private void cleanup() {
        if (!config.isCleanupWorkingDir()) {
            return;
        }
        logger.debug(String.format("Removing %s", programPaths.getDockerInspectorRunDirPath()));
        try {
            removeFileOrDir(programPaths.getDockerInspectorRunDirPath());
        } catch (final IOException e) {
            logger.error(String.format("Error cleaning up working directories: %s", e.getMessage()));
        }
    }

    private void removeFileOrDir(final String fileOrDirPath) throws IOException {
        logger.info(String.format("Removing file or dir: %s", fileOrDirPath));
        final File fileOrDir = new File(fileOrDirPath);
        if (fileOrDir.exists()) {
            if (fileOrDir.isDirectory()) {
                FileUtils.deleteDirectory(fileOrDir);
            } else {
                FileUtils.deleteQuietly(fileOrDir);
            }
        }
    }

    private ImageInspectorClient chooseImageInspectorClient() throws IntegrationException {
        for (final ImageInspectorClient client : imageInspectorClients) {
            if (client.isApplicable()) {
                return client;
            }
        }
        throw new IntegrationException("Invalid configuration: Need to provide URL to existing ImageInspector services, or request that containers be started as-needed");
    }
}
