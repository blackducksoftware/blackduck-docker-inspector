/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2021 Synopsys, Inc.
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
import com.synopsys.integration.bdio.BdioReader;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.blackduck.dockerinspector.blackduckclient.BlackDuckClient;
import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.ProgramPaths;
import com.synopsys.integration.blackduck.dockerinspector.dockerclient.DockerClientManager;
import com.synopsys.integration.blackduck.dockerinspector.output.ContainerFilesystemFilename;
import com.synopsys.integration.blackduck.dockerinspector.output.ImageTarWrapper;
import com.synopsys.integration.blackduck.dockerinspector.output.Output;
import com.synopsys.integration.blackduck.dockerinspector.output.OutputFiles;
import com.synopsys.integration.blackduck.dockerinspector.output.Result;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.util.NameVersion;

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
    private DockerClientManager dockerClientManager;

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

    public Result getBdio() throws IntegrationException, InterruptedException {
        ImageInspectorClient imageInspectorClient = chooseImageInspectorClient();
        try {
            output.ensureWorkingOutputDirIsWriteable();
            ImageTarWrapper finalDockerTarfile = prepareDockerTarfile(imageInspectorClient);
            String containerFileSystemFilename = containerFilesystemFilename.deriveContainerFilesystemFilename(finalDockerTarfile.getImageRepo(), finalDockerTarfile.getImageTag());
            String dockerTarFilePathInContainer = containerPaths.getContainerPathToTargetFile(finalDockerTarfile.getFile().getCanonicalPath());
            String containerFileSystemPathInContainer = null;
            if (config.isOutputIncludeContainerfilesystem() || config.isOutputIncludeSquashedImage()) {
                containerFileSystemPathInContainer = containerPaths.getContainerPathToOutputFile(containerFileSystemFilename);
            }
            String bdioString = imageInspectorClient.getBdio(finalDockerTarfile.getFile().getCanonicalPath(), dockerTarFilePathInContainer, config.getDockerImageRepo(), config.getDockerImageTag(),
                containerFileSystemPathInContainer, config.getContainerFileSystemExcludedPaths(),
                config.isOrganizeComponentsByLayer(), config.isIncludeRemovedComponents(),
                config.isCleanupWorkingDir(), config.getDockerPlatformTopLayerId(),
                config.getTargetImageLinuxDistroOverride());
            logger.trace(String.format("bdioString: %s", bdioString));
            SimpleBdioDocument bdioDocument = toBdioDocument(bdioString);
            adjustBdio(bdioDocument);
            OutputFiles outputFiles = output.addOutputToFinalOutputDir(bdioDocument, finalDockerTarfile.getImageRepo(), finalDockerTarfile.getImageTag());
            if (config.isUploadBdio()) {
                NameVersion projectAndVersion = new NameVersion(bdioDocument.getProject().name, bdioDocument.getProject().version);
                blackDuckClient.uploadBdio(outputFiles.getBdioFile(), bdioDocument.getBillOfMaterials().spdxName, projectAndVersion);
            }
            cleanup();
            Result result = Result.createResultSuccess(finalDockerTarfile.getImageRepo(), finalDockerTarfile.getImageTag(), finalDockerTarfile.getFile().getName(),
                outputFiles.getBdioFile(),
                outputFiles.getContainerFileSystemFile(),
                outputFiles.getSquashedImageFile());
            return result;
        } catch (IOException e) {
            throw new IntegrationException(e.getMessage(), e);
        }
    }

    private void adjustBdio(SimpleBdioDocument bdioDocument) {
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

    private SimpleBdioDocument toBdioDocument(String bdioString) throws IOException {
        Reader reader = new StringReader(bdioString);
        SimpleBdioDocument doc = null;
        try (BdioReader bdioReader = new BdioReader(gson, reader)) {
            doc = bdioReader.readSimpleBdioDocument();
            return doc;
        }
    }

    private ImageTarWrapper prepareDockerTarfile(ImageInspectorClient imageInspectorClient) throws IOException, IntegrationException {
        ImageTarWrapper givenDockerTarfile = dockerClientManager.deriveDockerTarFileFromConfig();
        ImageTarWrapper finalDockerTarfile = imageInspectorClient.copyTarfileToSharedDir(config, programPaths, givenDockerTarfile);
        return finalDockerTarfile;
    }

    private void cleanup() {
        if (!config.isCleanupWorkingDir()) {
            return;
        }
        logger.debug(String.format("Removing %s", programPaths.getDockerInspectorRunDirPath()));
        try {
            removeFileOrDir(programPaths.getDockerInspectorRunDirPath());
        } catch (IOException e) {
            logger.error(String.format("Error cleaning up working directories: %s", e.getMessage()));
        }
    }

    private void removeFileOrDir(String fileOrDirPath) throws IOException {
        logger.info(String.format("Removing file or dir: %s", fileOrDirPath));
        File fileOrDir = new File(fileOrDirPath);
        if (fileOrDir.exists()) {
            if (fileOrDir.isDirectory()) {
                FileUtils.deleteDirectory(fileOrDir);
            } else {
                FileUtils.deleteQuietly(fileOrDir);
            }
        }
    }

    private ImageInspectorClient chooseImageInspectorClient() throws IntegrationException {
        for (ImageInspectorClient client : imageInspectorClients) {
            if (client.isApplicable()) {
                return client;
            }
        }
        throw new IntegrationException("Invalid configuration: Need to provide URL to existing ImageInspector services, or request that containers be started as-needed");
    }
}
