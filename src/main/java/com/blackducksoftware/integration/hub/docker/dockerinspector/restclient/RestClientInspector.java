/**
 * hub-docker-inspector
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
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
package com.blackducksoftware.integration.hub.docker.dockerinspector.restclient;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.bdio.BdioReader;
import com.blackducksoftware.integration.hub.bdio.model.SimpleBdioDocument;
import com.blackducksoftware.integration.hub.docker.dockerinspector.common.DockerTarfile;
import com.blackducksoftware.integration.hub.docker.dockerinspector.common.Inspector;
import com.blackducksoftware.integration.hub.docker.dockerinspector.config.Config;
import com.blackducksoftware.integration.hub.imageinspector.lib.DissectedImage;
import com.google.gson.Gson;

@Component
public class RestClientInspector implements Inspector {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Config config;

    @Autowired
    private DockerTarfile dockerTarfile;

    @Autowired
    private ImageInspectorClient restClient;

    @Autowired
    private ContainerPath containerPath;

    @Override
    public int getBdio(final DissectedImage dissectedImage) throws IntegrationException {
        try {
            final File dockerTarFile = dockerTarfile.deriveDockerTarFile(config);
            final String containerFileSystemFilename = dockerTarfile.deriveContainerFileSystemTarGzFilename(dockerTarFile);
            final String dockerTarFilePathInContainer = containerPath.getContainerPathToLocalFile(dockerTarFile.getCanonicalPath());
            if (StringUtils.isBlank(config.getImageInspectorUrl())) {
                throw new IntegrationException("The imageinspector URL property must be set");
            }
            final String bdioString = restClient.getBdio(config.getImageInspectorUrl(), dockerTarFilePathInContainer, containerFileSystemFilename, config.isCleanupWorkingDir());
            if (StringUtils.isNotBlank(config.getOutputPath())) {
                final File userOutputDir = new File(config.getOutputPath());

                final String outputBdioFilename = deriveOutputBdioFilename(bdioString);
                final File outputBdioFile = new File(userOutputDir, outputBdioFilename);
                logger.info(String.format("Writing BDIO to %s", outputBdioFile.getAbsolutePath()));
                FileUtils.write(outputBdioFile, bdioString, StandardCharsets.UTF_8);

                final File localPathToContainerOutputDir = new File(config.getSharedDirPathLocal(), "output");
                final File localPathToContainerFileSytemFile = new File(localPathToContainerOutputDir, containerFileSystemFilename);
                final File userContainerFileSytemFile = new File(userOutputDir, containerFileSystemFilename);
                FileUtils.copyFile(localPathToContainerFileSytemFile, userContainerFileSytemFile);
            }
            return 0;
        } catch (final IOException e) {
            throw new IntegrationException(e.getMessage(), e);
        }
    }

    private String deriveOutputBdioFilename(final String bdioString) throws IOException, IntegrationException {
        final SimpleBdioDocument bdioDocument = getSimpleBdioDocument(bdioString);
        final BdioFilename outputFilename = new BdioFilename(bdioDocument.billOfMaterials.spdxName, bdioDocument.project.name, bdioDocument.project.version, bdioDocument.project.bdioExternalIdentifier.externalIdMetaData.forge.getName());
        return outputFilename.getBdioFilename();
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
