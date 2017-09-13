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
package com.blackducksoftware.integration.hub.docker.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.docker.executor.Executor;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CopyArchiveToContainerCmd;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmd;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.SaveImageCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;

@Component
public class DockerClientManager {

    private static final String INSPECTOR_COMMAND = "hub-docker-inspector-launcher.sh";
    private static final String IMAGE_TARFILE_PROPERTY = "docker.tar";
    private static final String IMAGE_PROPERTY = "docker.image";
    private static final String IMAGE_REPO_PROPERTY = "docker.image.repo";
    private static final String IMAGE_TAG_PROPERTY = "docker.image.tag";
    private static final String ON_HOST_PROPERTY = "on.host";
    private static final String OUTPUT_INCLUDE_TARFILE_PROPERTY = "output.include.tarfile";
    private static final String OUTPUT_INCLUDE_CONTAINER_FILE_SYSTEM_TARFILE_PROPERTY = "output.include.containerfilesystem";
    private final Logger logger = LoggerFactory.getLogger(DockerClientManager.class);

    @Autowired
    private HubDockerClient hubDockerClient;

    @Autowired
    private Executor executor;

    @Autowired
    private ProgramPaths programPaths;

    @Autowired
    private HubDockerProperties hubDockerProperties;

    @Value("${hub.password}")
    private String hubPasswordProperty;

    @Value("${BD_HUB_PASSWORD:}")
    private String hubPasswordEnvVar;

    @Value("${hub.proxy.host}")
    private String hubProxyHostProperty;

    @Value("${SCAN_CLI_OPTS:}")
    private String scanCliOptsEnvVar;

    public File getTarFileFromDockerImage(final String imageName, final String tagName) throws IOException, HubIntegrationException {
        final File imageTarDirectory = new File(new File(programPaths.getHubDockerWorkingDirPath()), "tarDirectory");
        pullImage(imageName, tagName);
        final File imageTarFile = new File(imageTarDirectory, programPaths.getImageTarFilename(imageName, tagName));
        saveImage(imageName, tagName, imageTarFile);
        return imageTarFile;
    }

    public void pullImage(final String imageName, final String tagName) throws HubIntegrationException {
        logger.info(String.format("Pulling image %s:%s", imageName, tagName));
        final DockerClient dockerClient = hubDockerClient.getDockerClient();
        final Image alreadyPulledImage = getLocalImage(dockerClient, imageName, tagName);
        if (alreadyPulledImage == null) {
            // Only pull if we dont already have it
            final PullImageCmd pull = dockerClient.pullImageCmd(imageName).withTag(tagName);

            try {
                pull.exec(new PullImageResultCallback()).awaitSuccess();
            } catch (final NotFoundException e) {
                logger.error(String.format("Pull failed: Image %s:%s not found. Please check the image name/tag", imageName, tagName));
                throw e;
            }
        } else {
            logger.info("Image already pulled");
        }
    }

    private Image getLocalImage(final DockerClient dockerClient, final String imageName, final String tagName) {
        Image alreadyPulledImage = null;
        final List<Image> images = dockerClient.listImagesCmd().withImageNameFilter(imageName).exec();
        for (final Image image : images) {
            for (final String tag : image.getRepoTags()) {
                if (tag.contains(tagName)) {
                    alreadyPulledImage = image;
                    break;
                }
            }
            if (alreadyPulledImage != null) {
                break;
            }
        }
        return alreadyPulledImage;
    }

    public void run(final String runOnImageName, final String runOnTagName, final File dockerTarFile, final boolean copyJar, final String targetImage, final String targetImageRepo, final String targetImageTag)
            throws InterruptedException, IOException, HubIntegrationException {

        final String hubPassword = getHubPassword();
        final String imageId = String.format("%s:%s", runOnImageName, runOnTagName);
        logger.info(String.format("Running container based on image %s", imageId));
        final String extractorContainerName = deriveContainerName(runOnImageName);
        logger.debug(String.format("Container name: %s", extractorContainerName));
        final DockerClient dockerClient = hubDockerClient.getDockerClient();
        final String tarFileDirInSubContainer = programPaths.getHubDockerTargetDirPathContainer();
        final String tarFilePathInSubContainer = programPaths.getHubDockerTargetDirPathContainer() + dockerTarFile.getName();

        final String containerId = ensureContainerRunning(dockerClient, imageId, extractorContainerName, hubPassword);
        setPropertiesInSubContainer(dockerClient, containerId, tarFilePathInSubContainer, tarFileDirInSubContainer, dockerTarFile, targetImage, targetImageRepo, targetImageTag);
        if (copyJar) {
            copyFileToContainer(dockerClient, containerId, programPaths.getHubDockerJarPath(), programPaths.getHubDockerPgmDirPathContainer());
        }

        final String cmd = programPaths.getHubDockerPgmDirPathContainer() + INSPECTOR_COMMAND;
        execCommandInContainer(dockerClient, imageId, containerId, cmd, tarFilePathInSubContainer);
        copyFileFromContainer(containerId, programPaths.getHubDockerOutputPathContainer() + ".", programPaths.getHubDockerOutputPath());
    }

    private void setPropertiesInSubContainer(final DockerClient dockerClient, final String containerId, final String tarFilePathInSubContainer, final String tarFileDirInSubContainer, final File dockerTarFile, final String targetImage,
            final String targetImageRepo, final String targetImageTag) throws IOException {
        hubDockerProperties.load();
        hubDockerProperties.set(IMAGE_TARFILE_PROPERTY, tarFilePathInSubContainer);
        hubDockerProperties.set(IMAGE_PROPERTY, targetImage);
        hubDockerProperties.set(IMAGE_REPO_PROPERTY, targetImageRepo);
        hubDockerProperties.set(IMAGE_TAG_PROPERTY, targetImageTag);
        hubDockerProperties.set(OUTPUT_INCLUDE_TARFILE_PROPERTY, "false");
        hubDockerProperties.set(OUTPUT_INCLUDE_CONTAINER_FILE_SYSTEM_TARFILE_PROPERTY, "false");
        hubDockerProperties.set(ON_HOST_PROPERTY, "false");
        final String pathToPropertiesFileForSubContainer = String.format("%s%s", programPaths.getHubDockerTargetDirPath(), ProgramPaths.APPLICATION_PROPERTIES_FILENAME);
        hubDockerProperties.save(pathToPropertiesFileForSubContainer);

        copyFileToContainer(dockerClient, containerId, pathToPropertiesFileForSubContainer, programPaths.getHubDockerConfigDirPathContainer());

        logger.trace(String.format("Docker image tar file: %s", dockerTarFile.getAbsolutePath()));
        logger.trace(String.format("Docker image tar file path in sub-container: %s", tarFilePathInSubContainer));
        copyFileToContainer(dockerClient, containerId, dockerTarFile.getAbsolutePath(), tarFileDirInSubContainer);
    }

    private String ensureContainerRunning(final DockerClient dockerClient, final String imageId, final String extractorContainerName, final String hubPassword) {
        String containerId;
        boolean isContainerRunning = false;
        final List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
        final Container extractorContainer = getRunningContainer(containers, extractorContainerName);
        if (extractorContainer != null) {
            containerId = extractorContainer.getId();
            if (extractorContainer.getStatus().startsWith("Up")) {
                logger.debug("The extractor container is already running");
                isContainerRunning = true;
            }
        } else {
            logger.debug(String.format("Creating container %s from image %s", extractorContainerName, imageId));
            final CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(imageId).withStdinOpen(true).withTty(true).withName(extractorContainerName).withCmd("/bin/bash");
            if ((StringUtils.isBlank(hubProxyHostProperty)) && (!StringUtils.isBlank(scanCliOptsEnvVar))) {
                createContainerCmd.withEnv(String.format("BD_HUB_PASSWORD=%s", hubPassword), String.format("SCAN_CLI_OPTS=%s", scanCliOptsEnvVar));
            } else {
                createContainerCmd.withEnv(String.format("BD_HUB_PASSWORD=%s", hubPassword));
            }

            final CreateContainerResponse containerResponse = createContainerCmd.exec();
            containerId = containerResponse.getId();
        }
        if (!isContainerRunning) {
            dockerClient.startContainerCmd(containerId).exec();
            logger.info(String.format("Started container %s from image %s", containerId, imageId));
        }
        return containerId;
    }

    private String getHubPassword() {
        String hubPassword = hubPasswordEnvVar;
        if (!StringUtils.isBlank(hubPasswordProperty)) {
            hubPassword = hubPasswordProperty;
        }
        return hubPassword;
    }

    private Container getRunningContainer(final List<Container> containers, final String extractorContainerName) {
        Container extractorContainer = null;
        for (final Container container : containers) {
            for (final String name : container.getNames()) {
                // name prefixed with '/' for some reason
                logger.debug(String.format("Checking running container %s to see if it is %s", name, extractorContainerName));
                if (name.contains(extractorContainerName)) {
                    logger.debug("The extractor container already exists");
                    extractorContainer = container;
                    break;
                }
            }
            if (extractorContainer != null) {
                break;
            }
        }
        return extractorContainer;
    }

    // The docker api that does this corrupts the file, so we do it via a shell cmd
    private void copyFileFromContainer(final String containerId, final String fromPath, final String toPath) throws HubIntegrationException, IOException, InterruptedException {
        logger.debug(String.format("Copying %s from container to %s via shell command", fromPath, toPath));
        final File toDir = new File(toPath);
        toDir.mkdirs();
        executor.executeCommand(String.format("docker cp %s:%s %s", containerId, fromPath, toPath));
    }

    private String deriveContainerName(final String imageName) {
        String extractorContainerName;
        final int slashIndex = imageName.lastIndexOf('/');
        if (slashIndex < 0) {
            extractorContainerName = String.format("%s-extractor", imageName);
        } else {
            extractorContainerName = imageName.substring(slashIndex + 1);
        }
        return extractorContainerName;
    }

    private void execCommandInContainer(final DockerClient dockerClient, final String imageId, final String containerId, final String cmd, final String arg) throws InterruptedException {
        logger.info(String.format("Running %s on %s in container %s from image %s", cmd, arg, containerId, imageId));
        final ExecCreateCmd execCreateCmd = dockerClient.execCreateCmd(containerId).withAttachStdout(true).withAttachStderr(true);
        final String[] cmdArr = { cmd, arg };
        execCreateCmd.withCmd(cmdArr);
        final ExecCreateCmdResponse execCreateCmdResponse = execCreateCmd.exec();
        logger.info("Switching to target image appropriate container");
        dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(new ExecStartResultCallback(System.out, System.err)).awaitCompletion();
        logger.info("Returning to primary container");
    }

    private void copyFileToContainer(final DockerClient dockerClient, final String containerId, final String srcPath, final String destPath) throws IOException {
        logger.info(String.format("Copying %s to container %s: %s", srcPath, containerId, destPath));
        final CopyArchiveToContainerCmd copyProperties = dockerClient.copyArchiveToContainerCmd(containerId).withHostResource(srcPath).withRemotePath(destPath);
        execCopyTo(copyProperties);
    }

    private void execCopyTo(final CopyArchiveToContainerCmd copyProperties) throws IOException {
        copyProperties.exec();
    }

    private void saveImage(final String imageName, final String tagName, final File imageTarFile) throws IOException, HubIntegrationException {
        InputStream tarInputStream = null;
        try {
            logger.info(String.format("Saving the docker image to : %s", imageTarFile.getCanonicalPath()));
            final DockerClient dockerClient = hubDockerClient.getDockerClient();
            final String imageToSave = String.format("%s:%s", imageName, tagName);
            final SaveImageCmd saveCommand = dockerClient.saveImageCmd(imageToSave);
            tarInputStream = saveCommand.exec();
            FileUtils.copyInputStreamToFile(tarInputStream, imageTarFile);
        } finally {
            IOUtils.closeQuietly(tarInputStream);
        }
    }
}
