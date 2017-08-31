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
import com.blackducksoftware.integration.hub.docker.linux.FileOperations;
import com.blackducksoftware.integration.hub.docker.tar.manifest.ManifestLayerMapping;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;

@SpringBootApplication
public class Application {
    private final Logger logger = LoggerFactory.getLogger(Application.class);

    // User should specify docker.tar OR docker.image
    @Value("${docker.tar}")
    private String dockerTar;

    // This may or may not have tag, like: ubuntu or ubuntu:16.04
    @Value("${docker.image}")
    private String dockerImage;

    // docker.image.repo and docker.image.tag are for selecting an image
    // from a multi-image tarfile
    @Value("${docker.image.repo}")
    private String dockerImageRepo;

    @Value("${docker.image.tag}")
    private String dockerImageTag;

    @Value("${linux.distro}")
    private String linuxDistro;

    @Value("${dev.mode}")
    private boolean devMode;

    @Value("${hub.project.name}")
    private String hubProjectName;

    @Value("${hub.project.version}")
    private String hubVersionName;

    @Value("${dry.run}")
    private boolean dryRun;

    @Autowired
    private HubClient hubClient;

    @Autowired
    private DockerImages dockerImages;

    @Autowired
    private HubDockerManager hubDockerManager;

    @Autowired
    private DockerClientManager dockerClientManager;

    @Autowired
    private ProgramVersion programVersion;

    @Autowired
    private ProgramPaths programPaths;

    public static void main(final String[] args) {
        new SpringApplicationBuilder(Application.class).logStartupInfo(false).run(args);
    }

    @PostConstruct
    public void inspectImage() {
        try {
            init();
            final File dockerTarFile = deriveDockerTarFile();

            final List<File> layerTars = hubDockerManager.extractLayerTars(dockerTarFile);
            final List<ManifestLayerMapping> layerMappings = hubDockerManager.getLayerMappings(dockerTarFile.getName(), dockerImageRepo, dockerImageTag);
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
        logger.debug(String.format("runInSubContainer(): Running subcontainer on image %s, repo %s, tag %s", dockerImage, dockerImageRepo, dockerImageTag));
        dockerClientManager.run(runOnImageName, runOnImageVersion, dockerTarFile, devMode, dockerImage, dockerImageRepo, dockerImageTag);
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
                FileOperations.copyFile(bdioFile, outputDir);
            }
        }
    }

    private void init() throws IOException, IntegrationException {
        logger.info(String.format("hub-docker-inspector %s", programVersion.getProgramVersion()));
        if (devMode) {
            logger.info("Running in development mode");
        }
        logger.trace(String.format("dockerImageTag: %s", dockerImageTag));
        initImageName();
        logger.info(String.format("Inspecting image:tag %s:%s", dockerImageRepo, dockerImageTag));
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

    private void initImageName() throws HubIntegrationException {
        logger.debug(String.format("initImageName(): dockerImage: %s, dockerTar: %s", dockerImage, dockerTar));
        if (StringUtils.isNotBlank(dockerImage)) {
            final String[] imageNameAndTag = dockerImage.split(":");
            if ((imageNameAndTag.length > 0) && (StringUtils.isNotBlank(imageNameAndTag[0]))) {
                dockerImageRepo = imageNameAndTag[0];
            }
            if ((imageNameAndTag.length > 1) && (StringUtils.isNotBlank(imageNameAndTag[1]))) {
                dockerImageTag = imageNameAndTag[1];
            } else {
                dockerImageTag = "latest";
            }
        }
        logger.debug(String.format("initImageName(): final: dockerImage: %s; dockerImageRepo: %s; dockerImageTag: %s", dockerImage, dockerImageRepo, dockerImageTag));
    }

    private File deriveDockerTarFile() throws IOException {
        File dockerTarFile = null;
        if (StringUtils.isNotBlank(dockerTar)) {
            dockerTarFile = new File(dockerTar);
        } else if (StringUtils.isNotBlank(dockerImageRepo)) {
            dockerTarFile = hubDockerManager.getTarFileFromDockerImage(dockerImageRepo, dockerImageTag);
        }
        return dockerTarFile;
    }
}
