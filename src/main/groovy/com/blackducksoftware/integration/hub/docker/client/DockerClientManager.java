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
import java.nio.charset.StandardCharsets;
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
import com.github.dockerjava.api.command.CopyArchiveFromContainerCmd;
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
    private static final String IMAGE_NAME_PROPERTY = "docker.image";
    private static final String IMAGE_TAG_PROPERTY = "docker.image.tag";
    private final Logger logger = LoggerFactory.getLogger(DockerClientManager.class);

    // TODO private members

    @Autowired
    HubDockerClient hubDockerClient;

    @Autowired
    Executor executor;

    @Autowired
    ProgramPaths programPaths;

    @Autowired
    HubDockerProperties hubDockerProperties;

    @Value("${hub.password}")
    String hubPasswordProperty;

    @Value("${BD_HUB_PASSWORD:}")
    String hubPasswordEnvVar;

    @Value("${hub.proxy.host}")
    String hubProxyHostProperty;

    @Value("${SCAN_CLI_OPTS:}")
    String scanCliOptsEnvVar;

    public File getTarFileFromDockerImage(final String imageName, final String tagName) throws IOException {
        final File imageTarDirectory = new File(new File(programPaths.getHubDockerWorkingDirPath()), "tarDirectory");
        pullImage(imageName, tagName);
        final File imageTarFile = new File(imageTarDirectory, programPaths.getImageTarFilename(imageName, tagName));
        saveImage(imageName, tagName, imageTarFile);
        return imageTarFile;
    }

    public void pullImage(final String imageName, final String tagName) {
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

    // TODO too big
    public void run(final String runOnImageName, final String runOnTagName, final File dockerTarFile, final boolean copyJar, final String targetImage, final String targetImageTag)
            throws InterruptedException, IOException, HubIntegrationException {

        String hubPassword = hubPasswordEnvVar;
        if (!StringUtils.isBlank(hubPasswordProperty)) {
            hubPassword = hubPasswordProperty;
        }

        final String imageId = String.format("%s:%s", runOnImageName, runOnTagName);
        logger.info(String.format("Running container based on image %s", imageId));
        final String extractorContainerName = deriveContainerName(runOnImageName);
        logger.debug(String.format("Container name: %s", extractorContainerName));
        final DockerClient dockerClient = hubDockerClient.getDockerClient();

        final String tarFileDirInSubContainer = programPaths.getHubDockerTargetDirPath();
        final String tarFilePathInSubContainer = programPaths.getHubDockerTargetDirPath() + dockerTarFile.getName();

        String containerId = "";
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
        hubDockerProperties.load();
        hubDockerProperties.set(IMAGE_TARFILE_PROPERTY, tarFilePathInSubContainer);
        hubDockerProperties.set(IMAGE_NAME_PROPERTY, targetImage);
        hubDockerProperties.set(IMAGE_TAG_PROPERTY, targetImageTag);
        final String pathToPropertiesFileForSubContainer = String.format("%s%s", programPaths.getHubDockerTargetDirPath(), ProgramPaths.APPLICATION_PROPERTIES_FILENAME);
        hubDockerProperties.save(pathToPropertiesFileForSubContainer);

        copyFileToContainer(dockerClient, containerId, pathToPropertiesFileForSubContainer, programPaths.getHubDockerConfigDirPath());

        logger.debug(String.format("Docker image tar file: %s", dockerTarFile.getAbsolutePath()));
        logger.debug(String.format("Docker image tar file path in sub-container: %s", tarFilePathInSubContainer));
        copyFileToContainer(dockerClient, containerId, dockerTarFile.getAbsolutePath(), tarFileDirInSubContainer);

        if (copyJar) {
            copyFileToContainer(dockerClient, containerId, programPaths.getHubDockerJarPath(), programPaths.getHubDockerPgmDirPath());
        }

        final String cmd = programPaths.getHubDockerPgmDirPath() + INSPECTOR_COMMAND;
        execCommandInContainer(dockerClient, imageId, containerId, cmd, tarFilePathInSubContainer);
        copyFileFromContainerViaShell(containerId, programPaths.getHubDockerOutputJsonPath() + ".", programPaths.getHubDockerOutputJsonPath());
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

    // TODO this is kind of a hack, and probably handles errors poorly
    private void copyFileFromContainerViaShell(final String containerId, final String fromPath, final String toPath) throws HubIntegrationException, IOException, InterruptedException {
        logger.debug(String.format("Copying %s from container to %s via shell command", fromPath, toPath));
        executor.executeCommand(String.format("docker cp %s:%s %s", containerId, fromPath, toPath));

        // TODO clean up:
        // final StringBuilder sout = new StringBuilder();
        // final StringBuilder serr = new StringBuilder();
        // final def proc = "docker cp ${containerId}:${fromPath} ${toPath}".execute();
        // proc.consumeProcessOutput(sout, serr);
        // proc.waitForOrKill(5000);
        // logger.debug("out> $sout err> $serr");
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
        dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(new ExecStartResultCallback(System.out, System.err)).awaitCompletion();
    }

    private void copyFileToContainer(final DockerClient dockerClient, final String containerId, final String srcPath, final String destPath) throws IOException {
        logger.info(String.format("Copying %s to container %s: %s", srcPath, containerId, destPath));
        final CopyArchiveToContainerCmd copyProperties = dockerClient.copyArchiveToContainerCmd(containerId).withHostResource(srcPath).withRemotePath(destPath);
        execCopyTo(copyProperties);
    }

    private void execCopyTo(final CopyArchiveToContainerCmd copyProperties) throws IOException {
        // TODO clean up
        // InputStream is = null;
        try {
            // is = copyProperties.getTarInputStream(); // TODO this is an input, not the output; how to get output??
            copyProperties.exec();

            // if (is != null) {
            // final String output = IOUtils.toString(is, StandardCharsets.UTF_8);
            // logger.debug(String.format("Output from copy command: %s", output));
            // }
        } finally {
            // if (is != null) {
            // IOUtils.closeQuietly(is);
            // }
        }
    }

    private void execCopyFrom(final CopyArchiveFromContainerCmd copyProperties, final String destPath) throws IOException {
        InputStream is = null;
        try {
            is = copyProperties.exec();
            if (is != null) {
                final String output = IOUtils.toString(is, StandardCharsets.UTF_8);
                logger.trace(String.format("Output from copy command: %s", output));
                // File targetFile = new File(destPath)
                // FileUtils.copyInputStreamToFile(is, targetFile)
            } else {
                logger.error("Copy failed (input stream returned is null");
            }
        } finally {
            if (is != null) {
                IOUtils.closeQuietly(is);
            }
        }
    }

    private void saveImage(final String imageName, final String tagName, final File imageTarFile) throws IOException {
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
