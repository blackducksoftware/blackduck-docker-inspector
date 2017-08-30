/**
 * Hub Docker Inspector
 *
 * Copyright (C) 2017 Black Duck Software, Inc.
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
package com.blackducksoftware.integration.hub.docker;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.docker.client.DockerClientManager;
import com.blackducksoftware.integration.hub.docker.client.ProgramPaths;
import com.blackducksoftware.integration.hub.docker.client.ProgramVersion;
import com.blackducksoftware.integration.hub.docker.image.DockerImages;
import com.blackducksoftware.integration.hub.docker.tar.manifest.ManifestLayerMapping;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;

@SpringBootApplication
public class Application {
    private final Logger logger = LoggerFactory.getLogger(Application.class);

    @Value("${docker.tar}")
    String dockerTar;

    @Value("${docker.image}")
    String dockerImage;

    @Value("${linux.distro}")
    String linuxDistro;

    @Value("${dev.mode}")
    boolean devMode;

    @Value("${hub.project.name}")
    String hubProjectName;

    @Value("${hub.project.version}")
    String hubVersionName;

    @Value("${dry.run}")
    boolean dryRun;

    @Autowired
    public HubClient hubClient;

    @Autowired
    DockerImages dockerImages;

    @Autowired
    HubDockerManager hubDockerManager;

    @Autowired
    DockerClientManager dockerClientManager;

    @Autowired
    ProgramVersion programVersion;

    @Autowired
    ProgramPaths programPaths;

    // TODO make members private
    String dockerImageName;

    @Value("${docker.image.tag}")
    String dockerTagName;

    public static void main(final String[] args) {
        new SpringApplicationBuilder(Application.class).logStartupInfo(false).run(args);
    }

    @PostConstruct
    public void inspectImage() {
        try {
            init();
            final File dockerTarFile = deriveDockerTarFile();

            final List<File> layerTars = hubDockerManager.extractLayerTars(dockerTarFile);
            final List<ManifestLayerMapping> layerMappings = hubDockerManager.getLayerMappings(dockerTarFile.getName(), dockerImageName, dockerTagName);
            final File targetImageFileSystemRootDir = hubDockerManager.extractDockerLayers(layerTars, layerMappings);

            final OperatingSystemEnum targetOsEnum = hubDockerManager.detectOperatingSystem(linuxDistro, targetImageFileSystemRootDir);
            final OperatingSystemEnum requiredOsEnum = dockerImages.getDockerImageOs(targetOsEnum);
            final OperatingSystemEnum currentOsEnum = hubDockerManager.detectCurrentOperatingSystem();
            if (currentOsEnum == requiredOsEnum) {
                generateBdio(dockerTarFile, targetImageFileSystemRootDir, layerMappings, currentOsEnum, targetOsEnum);
            } else {
                runInSubContainer(dockerTarFile, currentOsEnum, targetOsEnum);
            }
        } catch (final Exception e) {
            logger.error(String.format("Error inspecting image: %s", e.getMessage()));
            final String trace = ExceptionUtils.getStackTrace(e);
            logger.debug(String.format("Stack trace: %s", trace));
        }
    }

    private void runInSubContainer(final File dockerTarFile, final OperatingSystemEnum currentOsEnum, final OperatingSystemEnum targetOsEnum) throws InterruptedException, IOException, HubIntegrationException {
        final String runOnImageName = dockerImages.getDockerImageName(targetOsEnum);
        final String runOnImageVersion = dockerImages.getDockerImageVersion(targetOsEnum);
        final String msg = String.format("Image inspection for %s should not be run in this %s docker container; will use docker image %s:%s", targetOsEnum.toString(), currentOsEnum.toString(), runOnImageName, runOnImageVersion);
        logger.info(msg);
        try {
            dockerClientManager.pullImage(runOnImageName, runOnImageVersion);
        } catch (final Exception e) {
            logger.warn(String.format("Unable to pull docker image %s:%s; proceeding anyway since it may already exist locally", runOnImageName, runOnImageVersion));
        }
        dockerClientManager.run(runOnImageName, runOnImageVersion, dockerTarFile, devMode, dockerImageName, dockerTagName);
    }

    private void generateBdio(final File dockerTarFile, final File targetImageFileSystemRootDir, final List<ManifestLayerMapping> layerMappings, final OperatingSystemEnum currentOsEnum, final OperatingSystemEnum targetOsEnum)
            throws IOException, InterruptedException, IntegrationException {
        final String msg = String.format("Image inspection for %s can be run in this %s docker container; tarfile: %s", targetOsEnum.toString(), currentOsEnum.toString(), dockerTarFile.getAbsolutePath());
        logger.info(msg);
        final List<File> bdioFiles = hubDockerManager.generateBdioFromImageFilesDir(layerMappings, hubProjectName, hubVersionName, dockerTarFile, targetImageFileSystemRootDir, targetOsEnum);
        if (bdioFiles.size() == 0) {
            logger.warn("No BDIO Files generated");
        } else {
            if (dryRun) {
                logger.info("Running in dry run mode; not uploading BDIO to Hub");
            } else {
                logger.info("Uploading BDIO to Hub");
                hubDockerManager.uploadBdioFiles(bdioFiles);
            }
            final File outputDir = new File(programPaths.getHubDockerOutputJsonPath());
            for (final File bdioFile : bdioFiles) {
                logger.info(String.format("BDIO file: %s", bdioFile.getName()));
                hubDockerManager.copyFile(bdioFile, outputDir);
            }
        }
    }

    private void init() throws IOException, IntegrationException {
        logger.info(String.format("hub-docker-inspector %s", programVersion.getProgramVersion()));
        if (devMode) {
            logger.info("Running in development mode");
        }
        logger.trace(String.format("dockerTagName: %s", dockerTagName));
        initImageName();
        logger.info(String.format("Inspecting image/tag %s/%s", dockerImageName, dockerTagName));
        if (!dryRun) {
            verifyHubConnection();
        }
        hubDockerManager.init();
        hubDockerManager.cleanWorkingDirectory();
    }

    private void verifyHubConnection() throws HubIntegrationException {
        hubClient.testHubConnection();
        logger.info("Your Hub configuration is valid and a successful connection to the Hub was established.");
        return;
    }

    // TODO too much logging
    private void initImageName() {
        logger.debug(String.format("initImageName(): %s", dockerImage));
        if (StringUtils.isNotBlank(dockerImage)) {
            logger.trace(String.format("initImageName(): dockerImage specified: %s", dockerImage));
            final String[] imageNameAndTag = dockerImage.split(":");
            logger.debug(String.format("initImageName(): imageNameAndTag.length: %d", imageNameAndTag.length));
            if ((imageNameAndTag.length > 0) && (StringUtils.isNotBlank(imageNameAndTag[0]))) {
                dockerImageName = imageNameAndTag[0];
                logger.trace(String.format("initImageName(): set dockerImageName: %s", dockerImageName));
            }
            if ((imageNameAndTag.length > 1) && (StringUtils.isNotBlank(imageNameAndTag[1]))) {
                logger.debug(String.format("initImageName(): imageNameAndTag[1]: %s", imageNameAndTag[1]));
                dockerTagName = imageNameAndTag[1];
                logger.trace(String.format("initImageName(): set dockerTagName: %s", dockerTagName));
            } else {
                logger.trace(String.format("initImageName(): dockerTar: %s", dockerTar));
                if (StringUtils.isBlank(dockerTar)) {
                    dockerTagName = "latest";
                    logger.trace(String.format("initImageName(): set dockerTagName: %s", dockerTagName));
                }
            }
        }
        logger.debug(String.format("initImageName(): final: dockerImageName: %s; dockerTagName: %s", dockerImage, dockerTagName));
    }

    private File deriveDockerTarFile() throws IOException {
        File dockerTarFile = null;
        if (StringUtils.isNotBlank(dockerTar)) {
            dockerTarFile = new File(dockerTar);
        } else if (StringUtils.isNotBlank(dockerImageName)) {
            dockerTarFile = hubDockerManager.getTarFileFromDockerImage(dockerImageName, dockerTagName);
        }
        return dockerTarFile;
    }
}
