/**
 * blackduck-docker-inspector
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
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
package com.synopsys.integration.blackduck.dockerinspector.restclient;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import com.synopsys.integration.blackduck.dockerinspector.common.DockerTarfile;
import com.synopsys.integration.blackduck.dockerinspector.common.Output;
import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.ProgramPaths;
import com.synopsys.integration.blackduck.imageinspector.name.Names;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.bdio.BdioReader;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;

@Component
public class RestClientInspector {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Config config;

    @Autowired
    private BlackDuckClient blackDuckClient;

    @Autowired
    private ProgramPaths programPaths;

    @Autowired
    private DockerTarfile dockerTarfile;

    @Autowired
    private List<ImageInspectorClient> imageInspectorClients;

    @Autowired
    private ContainerPaths containerPaths;

    @Autowired
    private Output output;

    @Autowired
    private Gson gson;

    public int getBdio() throws IntegrationException {
        final ImageInspectorClient imageInspectorClient = chooseImageInspectorClient();
        try {
            output.ensureWriteability();
            final File finalDockerTarfile = prepareDockerTarfile(imageInspectorClient);
            final String containerFileSystemFilename = Names.getContainerFileSystemTarFilename(config.getDockerImage(), config.getDockerTar());
            final String dockerTarFilePathInContainer = containerPaths.getContainerPathToTargetFile(finalDockerTarfile.getCanonicalPath());
            String containerFileSystemPathInContainer = null;
            if (config.isOutputIncludeContainerfilesystem()) {
                containerFileSystemPathInContainer = containerPaths.getContainerPathToOutputFile(containerFileSystemFilename);
            }
            final String bdioString = imageInspectorClient.getBdio(finalDockerTarfile.getCanonicalPath(), dockerTarFilePathInContainer, config.getDockerImageRepo(), config.getDockerImageTag(), containerFileSystemPathInContainer,
                config.isOrganizeComponentsByLayer(), config.isIncludeRemovedComponents(),
                config.isCleanupWorkingDir());
            logger.debug(String.format("bdioString: %s", bdioString));
            final SimpleBdioDocument bdioDocument = toBdioDocument(bdioString);
            adjustBdio(bdioDocument);
            final File bdioFile = output.provideBdioFileOutput(bdioDocument);
            if (config.isUploadBdio()) {
                blackDuckClient.uploadBdio(bdioFile, bdioDocument.billOfMaterials.spdxName);
            }
            cleanup();
            return 0;
        } catch (final IOException e) {
            throw new IntegrationException(e.getMessage(), e);
        }
    }

    private void adjustBdio(final SimpleBdioDocument bdioDocument) {
        if (StringUtils.isNotBlank(config.getBlackDuckProjectName())) {
            bdioDocument.project.name = config.getBlackDuckProjectName();
        }
        if (StringUtils.isNotBlank(config.getBlackDuckProjectVersion())) {
            bdioDocument.project.version = config.getBlackDuckProjectVersion();
        }
        if (StringUtils.isNotBlank(config.getBlackDuckCodelocationName())) {
            bdioDocument.billOfMaterials.spdxName = config.getBlackDuckCodelocationName();
        } else if (StringUtils.isNotBlank(config.getBlackDuckCodelocationPrefix())) {
            bdioDocument.billOfMaterials.spdxName = String.format("%s_%s", config.getBlackDuckCodelocationPrefix(), bdioDocument.billOfMaterials.spdxName);
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
        final File givenDockerTarfile = dockerTarfile.deriveDockerTarFile();
        final File finalDockerTarfile = imageInspectorClient.copyTarfileToSharedDir(givenDockerTarfile);
        return finalDockerTarfile;
    }

    private void cleanup() {
        if (!config.isCleanupWorkingDir()) {
            return;
        }
        logger.debug(String.format("Removing %s", programPaths.getDockerInspectorRunDirPathHost()));
        try {
            removeFileOrDir(programPaths.getDockerInspectorRunDirPathHost());
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

    private SimpleBdioDocument getSimpleBdioDocument(final String bdioString) throws IOException {
        final InputStream bdioInputStream = new ByteArrayInputStream(bdioString.getBytes());
        SimpleBdioDocument simpleBdioDocument = null;
        try (BdioReader bdioReader = new BdioReader(new Gson(), bdioInputStream)) {
            simpleBdioDocument = bdioReader.readSimpleBdioDocument();
        }
        return simpleBdioDocument;
    }

}
