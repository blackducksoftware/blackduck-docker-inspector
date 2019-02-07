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
package com.synopsys.integration.blackduck.dockerinspector.dockerclient;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.command.RemoveImageCmd;
import com.github.dockerjava.api.command.SaveImageCmd;
import com.github.dockerjava.api.command.StopContainerCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.Version;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig.Builder;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.synopsys.integration.blackduck.dockerinspector.output.ImageTarFilename;
import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.exception.DisabledException;
import com.synopsys.integration.blackduck.exception.BlackDuckIntegrationException;
import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import com.synopsys.integration.blackduck.imageinspector.api.name.ImageNameResolver;
import com.synopsys.integration.exception.IntegrationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DockerClientManager {
    private static final String CONTAINER_APPNAME_LABEL_KEY = "app";
    private static final String CONTAINER_OS_LABEL_KEY = "os";
    private final Logger logger = LoggerFactory.getLogger(DockerClientManager.class);

    @Autowired
    private Config config;

    @Autowired
    private ImageTarFilename dockerTarfile;

    private DockerClient dockerClient;

    private DockerClient getDockerClient() {
        if (dockerClient == null) {
            final Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder();
            final DockerClientConfig config = builder.build();
            dockerClient = DockerClientBuilder.getInstance(config).build();
        }
        return dockerClient;
    }

    public String getDockerJavaLibraryVersion() {
        try {
            final Version version = getDockerClient().versionCmd().exec();
            return String.format("docker-java library version: %s; API version: %s", version.getApiVersion(), version.getVersion());
        } catch (Exception e) {
            return String.format("docker-java library version: <unknown>; Error getting version: %s", e.getMessage());
        }
    }

    public File getTarFileFromDockerImageById(final String imageId, final File imageTarDirectory) throws IntegrationException, IOException {

        final DockerClient dockerClient = getDockerClient();
        final InspectImageCmd inspectImageCmd = dockerClient.inspectImageCmd(imageId);
        final InspectImageResponse imageDetails = inspectImageCmd.exec();
        final List<String> repoTags = imageDetails.getRepoTags();
        if (repoTags.size() == 0) {
            throw new BlackDuckIntegrationException(String.format("Unable to get image name:tag for image ID %s", imageId));
        }

        final ImageNameResolver resolver = new ImageNameResolver(repoTags.get(0));
        final String imageName = resolver.getNewImageRepo().get();
        final String tagName = resolver.getNewImageTag().get();
        logger.debug(String.format("Converted image ID %s to image name:tag %s:%s", imageId, imageName, tagName));
        final File imageTarFile = saveImageToDir(imageTarDirectory, dockerTarfile.deriveImageTarFilenameFromImageTag(imageName, tagName), imageName, tagName);
        return imageTarFile;
    }

    public File getTarFileFromDockerImage(final String imageName, final String tagName, final File imageTarDirectory) throws IntegrationException, IOException {
        Optional<String> targetImageId = Optional.empty();
        try {
            targetImageId = Optional.ofNullable(pullImage(imageName, tagName));
        } catch (final DisabledException disabledException) {
            logger.info("Image pulling is disabled in offline mode");
        } catch (final Exception e) {
            logger.info(String.format("Unable to pull %s:%s; Proceeding anyway since the image might be in local docker image cache. Error on pull: %s", imageName, tagName, e.getMessage()));
        }
        final File imageTarFile = saveImageToDir(imageTarDirectory, dockerTarfile.deriveImageTarFilenameFromImageTag(imageName, tagName), imageName, tagName);
        if (config.isCleanupTargetImage() && targetImageId.isPresent()) {
            try {
                removeImage(targetImageId.get());
            } catch (final BlackDuckIntegrationException e) {
                logger.warn(String.format("Unable to remove target image with ID %s: %s", targetImageId.get(), e.getMessage()));
            }
        }
        return imageTarFile;
    }

    public String pullImage(final String imageName, final String tagName) throws IntegrationException {
        if (config.isOfflineMode()) {
            throw new DisabledException("Image pulling is disabled in offline mode");
        }
        logger.info(String.format("Pulling image %s:%s", imageName, tagName));
        final DockerClient dockerClient = getDockerClient();
        final PullImageCmd pull = dockerClient.pullImageCmd(imageName).withTag(tagName);
        try {
            pull.exec(new PullImageResultCallback()).awaitCompletion();
        } catch (final NotFoundException e) {
            throw new BlackDuckIntegrationException(String.format("Pull failed: Image %s:%s not found. Please check the image name/tag. Error: %s", imageName, tagName, e.getMessage()), e);
        } catch (final InterruptedException e) {
            throw new BlackDuckIntegrationException(String.format("Pull was interrupted: Image %s:%s not found. Error: %s", imageName, tagName, e.getMessage()), e);
        }
        final Image justPulledImage = getLocalImage(dockerClient, imageName, tagName);
        if (justPulledImage == null) {
            final String msg = String.format("Pulled image %s:%s not found in image list.", imageName, tagName);
            logger.error(msg);
            throw new BlackDuckIntegrationException(msg);
        }
        return justPulledImage.getId();
    }

    public void removeImage(final String imageId) throws IntegrationException {
        if (imageId == null) {
            logger.debug("removeImage(): given imageId is null; not doing anything");
            return;
        }
        try {
            final DockerClient dockerClient = getDockerClient();
            final RemoveImageCmd rmCmd = dockerClient.removeImageCmd(imageId);
            logger.info(String.format("Removing image %s", imageId));
            rmCmd.exec();
            logger.debug(String.format("Image %s removed", imageId));
        } catch (final Throwable e) {
            logger.warn(String.format("Unable to remove image with ID %s: %s", imageId, e.getMessage()));
        }
    }

    public String startContainerAsService(final String runOnImageName, final String runOnTagName, final String containerName, final ImageInspectorOsEnum inspectorOs, final int containerPort, final int hostPort,
            final String appNameLabelValue,
            final String jarPath,
            final String inspectorUrlAlpine, final String inspectorUrlCentos, final String inspectorUrlUbuntu) {
        final String imageNameTag = String.format("%s:%s", runOnImageName, runOnTagName);
        logger.info(String.format("Starting container: %s", containerName));
        logger.debug(String.format("\timageNameTag: %s", imageNameTag));
        final DockerClient dockerClient = getDockerClient();
        stopRemoveContainerIfExists(dockerClient, containerName);

        logger.debug(String.format("Creating container %s from image %s", containerName, imageNameTag));
        final String imageInspectorOsName = inspectorOs.name();
        final String cmd = String.format("java -jar %s --logging.level.com.synopsys=%s --server.port=%d --current.linux.distro=%s --inspector.url.alpine=%s --inspector.url.centos=%s --inspector.url.ubuntu=%s",
                jarPath,
                getLoggingLevelString(),
                containerPort,
                imageInspectorOsName, inspectorUrlAlpine, inspectorUrlCentos, inspectorUrlUbuntu);
        logger.debug(String.format("Starting service with cmd: %s", cmd));
        final Map<String, String> labels = new HashMap<>(1);
        labels.put(CONTAINER_APPNAME_LABEL_KEY, appNameLabelValue);
        labels.put(CONTAINER_OS_LABEL_KEY, imageInspectorOsName);
        final Bind bindMount = createBindMount(config.getSharedDirPathLocal(), config.getSharedDirPathImageInspector());
        final ExposedPort exposedPort = new ExposedPort(containerPort);
        final Ports portBindings = new Ports();
        portBindings.bind(exposedPort, Binding.bindPort(hostPort));
        HostConfig hostConfig = HostConfig.newHostConfig().withPortBindings(portBindings).withBinds(bindMount);
        final CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(imageNameTag)
            .withName(containerName)
            .withLabels(labels)
            .withExposedPorts(exposedPort)
            .withHostConfig(hostConfig)
            .withCmd(cmd.split(" "));

        final List<String> envAssignments = new ArrayList<>();
        if (StringUtils.isBlank(config.getBlackDuckProxyHost()) && !StringUtils.isBlank(config.getScanCliOptsEnvVar())) {
            envAssignments.add(String.format("SCAN_CLI_OPTS=%s", config.getScanCliOptsEnvVar()));
        }
        createContainerCmd.withEnv(envAssignments);
        final CreateContainerResponse containerResponse = createContainerCmd.exec();
        final String containerId = containerResponse.getId();

        dockerClient.startContainerCmd(containerId).exec();
        logger.debug(String.format("Started container %s from image %s", containerId, imageNameTag));

        return containerId;
    }

    public void stopRemoveContainer(final String containerId) throws IntegrationException {
        final DockerClient dockerClient = getDockerClient();
        stopContainer(dockerClient, containerId);
        removeContainer(dockerClient, containerId);
    }

    public void logServiceLogAsDebug(final String containerId) {
        final StringBuilder stringBuilder = new StringBuilder();
        final StringBuilderLogReader callback = new StringBuilderLogReader(stringBuilder);
        DockerClient dockerClient;
        try {
            dockerClient = getDockerClient();
        } catch (final Exception e1) {
            logger.debug(String.format("Error getting docker client: %s", e1.getMessage()));
            try {
                callback.close();
            } catch (final IOException e) {
            }
            return;
        }
        try {
            dockerClient.logContainerCmd(containerId)
                    .withStdErr(true)
                    .withStdOut(true)
                    .withTailAll()
                    .exec(callback)
                    .awaitCompletion();
        } catch (final InterruptedException e) {
            logger.debug(String.format("Error getting container log: %s", e.getMessage()));
        }

        final String log = callback.builder.toString();
        logger.debug(String.format("Image inspector service log:\n%s\n==================================\n", log));
        try {
            callback.close();
        } catch (final IOException e) {
        }
    }

    private String getLoggingLevelString() {
        if (logger.isTraceEnabled()) {
            return "TRACE";
        }
        if (logger.isDebugEnabled()) {
            return "DEBUG";
        }
        return "INFO";
    }

    private static class StringBuilderLogReader extends LogContainerResultCallback {
        public StringBuilder builder;

        public StringBuilderLogReader(final StringBuilder builder) {
            this.builder = builder;
        }

        @Override
        public void onNext(final Frame item) {
            builder.append(new String(item.getPayload()));
            super.onNext(item);
        }
    }

    private File saveImageToDir(final File imageTarDirectory, final String imageTarFilename, final String imageName, final String tagName) throws IOException, IntegrationException {
        final File imageTarFile = new File(imageTarDirectory, imageTarFilename);
        saveImageToFile(imageName, tagName, imageTarFile);
        return imageTarFile;
    }

    private Image getLocalImage(final DockerClient dockerClient, final String imageName, final String tagName) {
        Image localImage = null;
        final List<Image> images = dockerClient.listImagesCmd().withImageNameFilter(imageName).exec();
        for (final Image image : images) {
            if (image == null) {
                logger.warn("Encountered a null image in local docker registry");
                continue;
            }
            final String[] tags = image.getRepoTags();
            if (tags == null) {
                logger.warn("Encountered an image with a null tag list in local docker registry");
            } else {
                for (final String tag : tags) {
                    if (tag == null) {
                        continue;
                    }
                    if (tag.contains(tagName)) {
                        localImage = image;
                        break;
                    }
                }
            }
            if (localImage != null) {
                break;
            }
        }
        return localImage;
    }

    private void removeContainer(final DockerClient dockerClient, final String containerId) {
        final RemoveContainerCmd rmCmd = dockerClient.removeContainerCmd(containerId);
        logger.debug(String.format("Removing container %s", containerId));
        rmCmd.exec();
        logger.debug(String.format("Container %s removed", containerId));
    }

    private void stopContainer(final DockerClient dockerClient, final String containerId) {
        final Long timeoutMilliseconds = config.getCommandTimeout();
        final int timeoutSeconds = (int) (timeoutMilliseconds / 60000L);
        logger.debug(String.format("Stopping container %s", containerId));
        final StopContainerCmd stopCmd = dockerClient.stopContainerCmd(containerId).withTimeout(timeoutSeconds);
        stopCmd.exec();
        logger.debug(String.format("Container %s stopped", containerId));
    }

    public String getDockerEngineVersion() {
        logger.debug("Requesting version string from Docker engine");
        try {
            final DockerClient dockerClient = getDockerClient();
            final Info dockerInfo = dockerClient.infoCmd().exec();
            final String engineVersion = dockerInfo.getServerVersion();
            logger.debug(String.format("Docker Engine (Server) Version: %s", engineVersion));
            if (engineVersion == null) {
                return "Unknown";
            } else {
                return engineVersion;
            }
        } catch (final Exception e) {
            return "Unknown";
        }
    }

    private void stopRemoveContainerIfExists(final DockerClient dockerClient, final String extractorContainerName) {
        String oldContainerId;

        final Container extractorContainer = getRunningContainerByContainerName(dockerClient, extractorContainerName);
        if (extractorContainer != null) {
            logger.debug(String.format("Extractor container status: %s", extractorContainer.getStatus()));
            oldContainerId = extractorContainer.getId();
            if (extractorContainer.getStatus().startsWith("Up")) {
                logger.debug("The extractor container is running; stopping it");
                dockerClient.stopContainerCmd(oldContainerId).exec();
            }
            logger.debug("The extractor container exists; removing it");
            dockerClient.removeContainerCmd(oldContainerId).exec();
        }
    }

    private Bind createBindMount(final String pathOnHost, final String pathOnContainer) {
        logger.debug(String.format("Mounting host:%s to container:%s", pathOnHost, pathOnContainer));
        final File dirOnHost = new File(pathOnHost);
        final Volume volume = new Volume(pathOnContainer);
        final Bind bind = new Bind(dirOnHost.getAbsolutePath(), volume, AccessMode.rw);
        return bind;
    }

    public Container getRunningContainerByAppName(final String targetAppName, final ImageInspectorOsEnum targetInspectorOs) throws IntegrationException {
        final DockerClient dockerClient = getDockerClient();
        final List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
        for (final Container container : containers) {
            logger.debug(String.format("Checking container %s to see if it has labels app = %s, os = %s", container.getNames()[0], targetAppName, targetInspectorOs.name()));
            final String containerAppName = container.getLabels().get(CONTAINER_APPNAME_LABEL_KEY);
            final String containerOsName = container.getLabels().get(CONTAINER_OS_LABEL_KEY);
            logger.debug(String.format("Comparing app name %s to %s, os name %s to %s", targetAppName, containerAppName, targetInspectorOs.name(), containerOsName));
            if (targetAppName.equals(containerAppName) && targetInspectorOs.name().equalsIgnoreCase(containerOsName)) {
                logger.debug("\tIt's a match");
                return container;
            }
        }
        throw new BlackDuckIntegrationException(String.format("No running container found with app = %s, os = %s", targetAppName, targetInspectorOs.name()));
    }

    private Container getRunningContainerByContainerName(final DockerClient dockerClient, final String extractorContainerName) {
        Container extractorContainer = null;
        final List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
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

    private void saveImageToFile(final String imageName, final String tagName, final File imageTarFile) throws IOException {
            logger.info(String.format("Saving the docker image to : %s", imageTarFile.getCanonicalPath()));
            final DockerClient dockerClient = getDockerClient();
            final String imageToSave = String.format("%s:%s", imageName, tagName);
            final SaveImageCmd saveCommand = dockerClient.saveImageCmd(imageToSave);
        try (InputStream tarInputStream = saveCommand.exec()) {
            FileUtils.copyInputStreamToFile(tarInputStream, imageTarFile);
        }
    }
}
