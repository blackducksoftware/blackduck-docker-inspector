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
package com.blackducksoftware.integration.hub.docker.imageinspector.api;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.bdio.model.SimpleBdioDocument;
import com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.manifest.ManifestLayerMapping;
import com.blackducksoftware.integration.hub.docker.imageinspector.lib.ImageInfoDerived;
import com.blackducksoftware.integration.hub.docker.imageinspector.lib.ImageInspector;
import com.blackducksoftware.integration.hub.docker.imageinspector.lib.OperatingSystemEnum;
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.Os;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;

@Component
public class ImageInspectorApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ImageInspector imageInspector;

    @Autowired
    private Os os;

    public SimpleBdioDocument getBdio(final String dockerTarfilePath, final String hubProjectName, final String hubProjectVersion, final String codeLocationPrefix) throws IOException, HubIntegrationException, InterruptedException {
        logger.info("getBdio()");
        return inspect(dockerTarfilePath, hubProjectName, hubProjectVersion, codeLocationPrefix);
    }

    public File getBdioFile(final String dockerTarfilePath) throws WrongInspectorOsException {
        logger.info("getBdioFile()");
        return null;
    }

    private SimpleBdioDocument inspect(final String dockerTarfilePath, final String hubProjectName, final String hubProjectVersion, final String codeLocationPrefix) throws IOException, HubIntegrationException, InterruptedException {
        final File dockerTarfile = new File(dockerTarfilePath);
        final File tempDir = createTempDirectory();
        final File workingDir = new File(tempDir, "working");
        final File outputDir = new File(tempDir, "output");
        imageInspector.init(workingDir.getAbsolutePath(), outputDir.getAbsolutePath(), codeLocationPrefix);
        final List<File> layerTars = imageInspector.extractLayerTars(dockerTarfile);
        final List<ManifestLayerMapping> tarfileMetadata = imageInspector.getLayerMappings(dockerTarfile.getName(), null, null);
        if (tarfileMetadata.size() != 1) {
            final String msg = String.format("Expected a single image tarfile, but %s has %d images", dockerTarfilePath, tarfileMetadata.size());
            throw new HubIntegrationException(msg);
        }
        final ManifestLayerMapping imageMetadata = tarfileMetadata.get(0);
        final String imageRepo = imageMetadata.getImageName();
        final String imageTag = imageMetadata.getTagName();
        /// end parse manifest
        final File targetImageFileSystemRootDir = imageInspector.extractDockerLayers(imageRepo, imageTag, layerTars, tarfileMetadata);
        final OperatingSystemEnum targetOs = imageInspector.detectOperatingSystem(targetImageFileSystemRootDir);
        final OperatingSystemEnum currentOs = os.deriveCurrentOs();
        if (!targetOs.equals(currentOs)) {
            final ImageInspectorOsEnum neededInspectorOs = getImageInspectorOsEnum(targetOs);
            final String msg = String.format("This docker tarfile needs to be inspected on %s", neededInspectorOs);
            throw new WrongInspectorOsException(neededInspectorOs, msg);
        }
        final ImageInfoDerived imageInfoDerived = imageInspector.generateBdioFromImageFilesDir(imageRepo, imageTag, tarfileMetadata, hubProjectName, hubProjectVersion, dockerTarfile, targetImageFileSystemRootDir, targetOs);
        return imageInfoDerived.getBdioDocument();
    }

    private File createTempDirectory() throws IOException {
        final File temp = File.createTempFile("ImageInspectorApi", Long.toString(System.nanoTime()));
        if (!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }
        if (!(temp.mkdir())) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
        return (temp);
    }

    private ImageInspectorOsEnum getImageInspectorOsEnum(final OperatingSystemEnum osEnum) throws HubIntegrationException {
        switch (osEnum) {
        case UBUNTU:
            return ImageInspectorOsEnum.UBUNTU;
        case CENTOS:
            return ImageInspectorOsEnum.CENTOS;
        case ALPINE:
            return ImageInspectorOsEnum.ALPINE;
        default:
            throw new HubIntegrationException("");
        }
    }
}
