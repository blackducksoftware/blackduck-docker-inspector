/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.dockerclient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
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
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.ProgramPaths;
import com.synopsys.integration.blackduck.dockerinspector.exception.DisabledException;
import com.synopsys.integration.blackduck.dockerinspector.output.ImageTarFilename;
import com.synopsys.integration.blackduck.dockerinspector.output.ImageTarWrapper;
import com.synopsys.integration.blackduck.exception.BlackDuckIntegrationException;
import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import com.synopsys.integration.blackduck.imageinspector.api.name.ImageNameResolver;
import com.synopsys.integration.blackduck.imageinspector.image.common.RepoTag;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.util.OperatingSystemType;

@Component
public class DockerClientManager {
    private static final String CONTAINER_APPNAME_LABEL_KEY = "app";
    private static final String CONTAINER_OS_LABEL_KEY = "os";
    private final Logger logger = LoggerFactory.getLogger(DockerClientManager.class);
    private final FileOperations fileOperations;
    private final ImageNameResolver imageNameResolver;
    private final Config config;
    private final ImageTarFilename imageTarFilename;
    private final ProgramPaths programPaths;
    private final DockerClient dockerClient;

    @Autowired
    public DockerClientManager(
        FileOperations fileOperations, ImageNameResolver imageNameResolver, Config config, ImageTarFilename imageTarFilename,
        ProgramPaths programPaths
    ) {
        this.fileOperations = fileOperations;
        this.imageNameResolver = imageNameResolver;
        this.config = config;
        this.imageTarFilename = imageTarFilename;
        this.programPaths = programPaths;

        Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder();
        // The java-docker library's default docker host value is the Linux/Mac default value, so no action required
        // But for Windows, unless told not to: use the Windows default docker host value
        if (config.isUsePlatformDefaultDockerHost() &&
            (OperatingSystemType.determineFromSystem() == OperatingSystemType.WINDOWS)) {
            builder
                .withDockerHost("npipe:////./pipe/docker_engine");
        }

        DockerClientConfig dockerClientConfig = builder.build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(dockerClientConfig.getDockerHost())
            .sslConfig(dockerClientConfig.getSSLConfig())
            .build();

        dockerClient = DockerClientImpl.getInstance(dockerClientConfig, httpClient);
    }

    public String getDockerJavaLibraryVersion() {
        try {
            Version version = dockerClient.versionCmd().exec();
            return String.format("docker-java library version: %s; API version: %s", version.getApiVersion(), version.getVersion());
        } catch (Exception e) {
            return String.format("docker-java library version: <unknown>; Error getting version: %s", e.getMessage());
        }
    }

    public ImageTarWrapper getTarFileFromDockerImageById(String imageId, File imageTarDirectory) throws IntegrationException, IOException {

        InspectImageCmd inspectImageCmd = dockerClient.inspectImageCmd(imageId);
        InspectImageResponse imageDetails = inspectImageCmd.exec();
        List<String> repoTags = imageDetails.getRepoTags();
        if (repoTags == null || repoTags.isEmpty()) {
            throw new IntegrationException(String.format("Unable to get image name:tag for image ID %s", imageId));
        }
        RepoTag resolvedRepoTag = imageNameResolver.resolve(repoTags.get(0), null, null);
        String imageName = resolvedRepoTag.getRepo().orElse("");
        String tagName = resolvedRepoTag.getTag().orElse("");
        logger.debug(String.format("Converted image ID %s to image name:tag %s:%s", imageId, imageName, tagName));
        File imageTarFile = saveImageToDir(imageTarDirectory, imageTarFilename.deriveImageTarFilenameFromImageTag(imageName, tagName), imageName, tagName);
        return new ImageTarWrapper(imageTarFile, imageName, tagName);
    }

    public ImageTarWrapper deriveDockerTarFileFromConfig() throws IOException, IntegrationException {
        logger.debug(String.format("programPaths.getDockerInspectorTargetDirPath(): %s", programPaths.getDockerInspectorTargetDirPath()));
        ImageTarWrapper tarWrapper;
        if (StringUtils.isNotBlank(config.getDockerTar())) {
            File dockerTarFile = new File(config.getDockerTar());
            checkGivenTarFileExtension(dockerTarFile);
            fileOperations.logFileOwnerGroupPerms(dockerTarFile);
            tarWrapper = new ImageTarWrapper(dockerTarFile);
        } else {
            tarWrapper = deriveDockerTarFileGivenImageSpec();
        }
        fileOperations.logFileOwnerGroupPerms(tarWrapper.getFile());
        return tarWrapper;
    }

    public String pullImage(String imageName, String tagName) throws IntegrationException, InterruptedException {
        validateMode();
        logger.info(String.format("Pulling image %s:%s", imageName, tagName));
        PullImageCmd pull = dockerClient.pullImageCmd(imageName).withTag(tagName);
        return pullImage(imageName, tagName, pull);
    }

    public String pullImageByPlatform(String imageName, String tagName, String platform) throws IntegrationException, InterruptedException {
        validateMode();
        logger.info(String.format("Pulling image %s:%s, platform: %s", imageName, tagName, platform));
        PullImageCmd pull = dockerClient.pullImageCmd(imageName).withTag(tagName).withPlatform(platform);
        return pullImage(imageName, tagName, pull);
    }

    private void checkGivenTarFileExtension(File givenTarFile) {
        if (!givenTarFile.getName().endsWith(".tar")) {
            logger.warn("The given docker tar file {} must be UNIX tar format but does not have a .tar extension; proceeding anyway", givenTarFile.getAbsolutePath());
        }
    }

    private String pullImage(String imageName, String tagName, PullImageCmd pull) throws IntegrationException, InterruptedException {
        try {
            pull.exec(new PullImageResultCallback()).awaitCompletion();
        } catch (NotFoundException e) {
            throw new BlackDuckIntegrationException(String.format("Pull failed: Image %s:%s not found. Please check the image name/tag. Error: %s", imageName, tagName, e.getMessage()), e);
        }
        Optional<Image> justPulledImage = getLocalImage(dockerClient, imageName, tagName);
        if (!justPulledImage.isPresent()) {
            String msg = String.format("Pulled image %s:%s not found in image list.", imageName, tagName);
            logger.error(msg);
            throw new BlackDuckIntegrationException(msg);
        }
        return justPulledImage.get().getId();
    }

    private void validateMode() throws DisabledException {
        if (config.isOfflineMode()) {
            throw new DisabledException("Image pulling is disabled in offline mode");
        }
    }

    public void removeImage(String imageId) {
        if (imageId == null) {
            logger.debug("removeImage(): given imageId is null; not doing anything");
            return;
        }
        try {
            RemoveImageCmd rmCmd = dockerClient.removeImageCmd(imageId);
            logger.info(String.format("Removing image %s", imageId));
            rmCmd.exec();
            logger.debug(String.format("Image %s removed", imageId));
        } catch (Exception e) {
            logger.warn(String.format("Unable to remove image with ID %s: %s", imageId, e.getMessage()));
        }
    }

    public String startContainerAsService(
        String runOnImageName, String runOnTagName, String containerName, ImageInspectorOsEnum inspectorOs, int containerPort, int hostPort,
        String appNameLabelValue,
        String jarPath,
        String inspectorUrlAlpine, String inspectorUrlCentos, String inspectorUrlUbuntu
    ) throws IOException {
        String imageNameTag = String.format("%s:%s", runOnImageName, runOnTagName);
        logger.info(String.format("Starting container: %s", containerName));
        logger.debug(String.format("\timageNameTag: %s", imageNameTag));
        stopRemoveContainerIfExists(dockerClient, containerName);

        logger.debug(String.format("Creating container %s from image %s", containerName, imageNameTag));
        String imageInspectorOsName = inspectorOs.name();
        String cmd = String.format("java -jar %s --logging.level.com.synopsys=%s --server.port=%d --current.linux.distro=%s --inspector.url.alpine=%s --inspector.url.centos=%s --inspector.url.ubuntu=%s",
            jarPath,
            getLoggingLevelString(),
            containerPort,
            imageInspectorOsName, inspectorUrlAlpine, inspectorUrlCentos, inspectorUrlUbuntu
        );
        logger.debug(String.format("Starting service with cmd: %s", cmd));
        Map<String, String> labels = new HashMap<>(1);
        labels.put(CONTAINER_APPNAME_LABEL_KEY, appNameLabelValue);
        labels.put(CONTAINER_OS_LABEL_KEY, imageInspectorOsName);
        Bind bindMount = createBindMount(config.getSharedDirPathLocal(), config.getSharedDirPathImageInspector());
        ExposedPort exposedPort = new ExposedPort(containerPort);
        Ports portBindings = new Ports();
        portBindings.bind(exposedPort, Binding.bindPort(hostPort));
        HostConfig hostConfig = HostConfig.newHostConfig().withPortBindings(portBindings).withBinds(bindMount);
        try (CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(imageNameTag)
            .withName(containerName)
            .withLabels(labels)
            .withExposedPorts(exposedPort)
            .withHostConfig(hostConfig)
            .withCmd(cmd.split(" "))) {

            List<String> envAssignments = new ArrayList<>();
            if (StringUtils.isBlank(config.getBlackDuckProxyHost()) && !StringUtils.isBlank(config.getScanCliOptsEnvVar())) {
                envAssignments.add(String.format("SCAN_CLI_OPTS=%s", config.getScanCliOptsEnvVar()));
            }
            createContainerCmd.withEnv(envAssignments);
            CreateContainerResponse containerResponse = createContainerCmd.exec();
            String containerId = containerResponse.getId();

            dockerClient.startContainerCmd(containerId).exec();
            logger.debug(String.format("Started container %s from image %s", containerId, imageNameTag));

            return containerId;
        }
    }

    public void stopRemoveContainer(String containerId) {
        stopContainer(dockerClient, containerId);
        removeContainer(dockerClient, containerId);
    }

    public String buildImage(File dockerBuildDir, Set<String> tags) throws IOException {
        logger.debug(String.format("Building image: %s", tags));
        String imageId;
        try (BuildImageCmd buildImageCmd = dockerClient.buildImageCmd(dockerBuildDir).withTags(tags);
            BuildImageResultCallback callback = new BuildImageResultCallback()) {
            try (BuildImageResultCallback resultCallback = buildImageCmd.exec(callback)) {
                imageId = resultCallback.awaitImageId();
            }
        }
        logger.debug(String.format("Built image: %s", imageId));
        return imageId;
    }

    public Optional<String> lookupImageIdByRepoTag(String repo, String tag) {
        Optional<String> imageId = Optional.empty();
        Optional<Image> image = getLocalImage(dockerClient, repo, tag);
        if (image.isPresent()) {
            imageId = Optional.of(image.get().getId());
        }
        return imageId;
    }

    public void logServiceLogAsDebug(String containerId) {
        StringBuilder stringBuilder = new StringBuilder();
        try (StringBuilderLogReader callback = new StringBuilderLogReader(stringBuilder)) {
            dockerClient.logContainerCmd(containerId)
                .withStdErr(true)
                .withStdOut(true)
                .withTail(config.getImageInspectorServiceLogLength())
                .exec(callback)
                .awaitCompletion();
            String log = callback.builder.toString();
            logger.debug("Image inspector service log:");
            logger.debug(log);
            logger.debug("==================================");
        } catch (Exception e) {
            logger.warn(String.format("Error getting log for service container %s", containerId), e);
        }
    }

    private ImageTarWrapper deriveDockerTarFileGivenImageSpec() throws IntegrationException, IOException {
        ImageTarWrapper finalDockerTarfile;
        File imageTarDirectory = new File(programPaths.getDockerInspectorTargetDirPath());
        if (StringUtils.isNotBlank(config.getDockerImageId())) {
            finalDockerTarfile = getTarFileFromDockerImageById(config.getDockerImageId(), imageTarDirectory);
        } else if (StringUtils.isNotBlank(config.getDockerImageRepo())) {
            finalDockerTarfile = getTarFileFromDockerImage(config.getDockerImageRepo(), config.getDockerImageTag(), imageTarDirectory);
        } else {
            throw new BlackDuckIntegrationException("You must specify a docker image");
        }
        return finalDockerTarfile;
    }

    private ImageTarWrapper getTarFileFromDockerImage(String imageName, String tagName, File imageTarDirectory) throws IntegrationException, IOException {
        Optional<String> targetImageId = Optional.empty();
        try {
            targetImageId = getImageId(imageName, tagName);
        } catch (DisabledException disabledException) {
            logger.info("Image pulling is disabled in offline mode");
        } catch (Exception e) {
            throwIfPlatformSpecified(imageName, tagName, e);
            logger.warn(String.format("Unable to pull %s:%s; Proceeding anyway since the image might be in local docker image cache. Error on pull: %s", imageName, tagName, e.getMessage()));
        }
        File imageTarFile = saveImageToDir(imageTarDirectory, imageTarFilename.deriveImageTarFilenameFromImageTag(imageName, tagName), imageName, tagName);
        ImageTarWrapper imageTarWrapper = new ImageTarWrapper(imageTarFile, imageName, tagName);
        if (config.isCleanupTargetImage() && targetImageId.isPresent()) {
            removeImage(targetImageId.get());
        }
        return imageTarWrapper;
    }

    private void throwIfPlatformSpecified(String imageName, String tagName, Exception e) throws IntegrationException {
        if (StringUtils.isNotBlank(config.getDockerImagePlatform())) {
            String msg = String.format("Unable to pull %s:%s platform %s. If you want to inspect a local image, run again without specifying the platform. Error on pull: %s", imageName, tagName,
                config.getDockerImagePlatform(), e.getMessage()
            );
            throw new IntegrationException(msg, e);
        }
    }

    private Optional<String> getImageId(String imageName, String tagName) throws InterruptedException, IntegrationException {
        if (StringUtils.isNotBlank(config.getDockerImagePlatform())) {
            return Optional.ofNullable(pullImageByPlatform(imageName, tagName, config.getDockerImagePlatform()));
        } else {
            return Optional.ofNullable(pullImage(imageName, tagName));
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
        private final StringBuilder builder;

        public StringBuilderLogReader(StringBuilder builder) {
            this.builder = builder;
        }

        @Override
        public void onNext(Frame item) {
            builder.append(new String(item.getPayload()));
            super.onNext(item);
        }
    }

    private File saveImageToDir(File imageTarDirectory, String imageTarFilename, String imageName, String tagName) throws IOException {
        imageTarDirectory.mkdirs();
        File imageTarFile = new File(imageTarDirectory, imageTarFilename);
        Files.deleteIfExists(imageTarFile.toPath());
        saveImageToFile(imageName, tagName, imageTarFile);
        return imageTarFile;
    }

    private Optional<Image> getLocalImage(DockerClient dockerClient, String imageName, String tagName) {
        String nonNullTagName = tagName == null ? "" : tagName;
        List<Image> images = dockerClient.listImagesCmd().withImageNameFilter(imageName).exec();
        for (Image image : images) {
            if (image == null) {
                logger.warn("Encountered a null image in local docker registry");
                continue;
            }
            logger.trace(String.format("getLocalImage(%s, %s) examining %s", imageName, nonNullTagName, image.getId()));
            String[] repoTagList = image.getRepoTags();
            if ((repoTagList != null) && (findMatchForTargetImageAmongTheseTags(imageName, nonNullTagName, image, repoTagList))) {
                return Optional.of(image);
            } else {
                logger.trace("Encountered an image with a null tag list in local docker registry");
            }
        }
        return Optional.empty();
    }

    private boolean findMatchForTargetImageAmongTheseTags(
        String targetImageName, String targetTagName,
        Image candidateImage, String[] candidateImageTagList
    ) {
        for (String repoTag : candidateImageTagList) {
            logger.trace(String.format("getLocalImage(%s, %s) examining %s", targetImageName, targetTagName, repoTag));
            if (repoTag == null) {
                continue;
            }
            String repoColonString = String.format("%s:", targetImageName);
            if (!repoTag.startsWith(repoColonString)) {
                logger.trace(String.format("Repo value of %s doesn't match target repo %s", repoTag, targetImageName));
                continue;
            }
            String colonTagString = String.format(":%s", targetTagName);
            logger.trace(String.format("getLocalImage(%s, %s) checking to see if %s ends with %s", targetImageName, targetTagName, repoTag, colonTagString));
            if (repoTag.endsWith(colonTagString)) {
                logger.trace(String.format("getLocalImage(%s, %s) found image id %s", targetImageName, targetTagName, candidateImage.getId()));
                return true;
            }
        }
        return false;
    }

    private void removeContainer(DockerClient dockerClient, String containerId) {
        RemoveContainerCmd rmCmd = dockerClient.removeContainerCmd(containerId);
        logger.debug(String.format("Removing container %s", containerId));
        rmCmd.exec();
        logger.debug(String.format("Container %s removed", containerId));
    }

    private void stopContainer(DockerClient dockerClient, String containerId) {
        Long timeoutMilliseconds = config.getCommandTimeout();
        int timeoutSeconds = (int) (timeoutMilliseconds / 60000L);
        logger.debug(String.format("Stopping container %s", containerId));
        StopContainerCmd stopCmd = dockerClient.stopContainerCmd(containerId).withTimeout(timeoutSeconds);
        stopCmd.exec();
        logger.debug(String.format("Container %s stopped", containerId));
    }

    public String getDockerEngineVersion() {
        logger.debug("Requesting version string from Docker engine");
        try {
            Info dockerInfo = dockerClient.infoCmd().exec();
            String engineVersion = dockerInfo.getServerVersion();
            logger.debug(String.format("Docker Engine (Server) Version: %s", engineVersion));
            if (engineVersion == null) {
                return "Unknown";
            } else {
                return engineVersion;
            }
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private void stopRemoveContainerIfExists(DockerClient dockerClient, String extractorContainerName) {
        String oldContainerId;

        Container extractorContainer = getRunningContainerByContainerName(dockerClient, extractorContainerName);
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

    private Bind createBindMount(String pathOnHost, String pathOnContainer) {
        logger.debug(String.format("Mounting host:%s to container:%s", pathOnHost, pathOnContainer));
        File dirOnHost = new File(pathOnHost);
        Volume volume = new Volume(pathOnContainer);
        return new Bind(dirOnHost.getAbsolutePath(), volume, AccessMode.rw);
    }

    public Container getRunningContainerByAppName(String targetAppName, ImageInspectorOsEnum targetInspectorOs) throws IntegrationException {
        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
        for (Container container : containers) {
            logger.debug(String.format("Checking container %s to see if it has labels app = %s, os = %s", container.getNames()[0], targetAppName, targetInspectorOs.name()));
            String containerAppName = container.getLabels().get(CONTAINER_APPNAME_LABEL_KEY);
            String containerOsName = container.getLabels().get(CONTAINER_OS_LABEL_KEY);
            logger.debug(String.format("Comparing app name %s to %s, os name %s to %s", targetAppName, containerAppName, targetInspectorOs.name(), containerOsName));
            if (targetAppName.equals(containerAppName) && targetInspectorOs.name().equalsIgnoreCase(containerOsName)) {
                logger.debug("\tIt's a match");
                return container;
            }
        }
        throw new BlackDuckIntegrationException(String.format("No running container found with app = %s, os = %s", targetAppName, targetInspectorOs.name()));
    }

    private Container getRunningContainerByContainerName(DockerClient dockerClient, String extractorContainerName) {
        Container extractorContainer = null;
        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
        for (Container container : containers) {
            for (String name : container.getNames()) {
                // name prefixed with '/' for some reason
                logger.trace(String.format("Checking running container %s to see if it is %s", name, extractorContainerName));
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

    private void saveImageToFile(String imageName, String tagName, File imageTarFile) throws IOException {
        logger.info(String.format("Saving the docker image to : %s", imageTarFile.getCanonicalPath()));
        String imageToSave = String.format("%s:%s", imageName, tagName);
        SaveImageCmd saveCommand = dockerClient.saveImageCmd(imageToSave);
        try (InputStream tarInputStream = saveCommand.exec()) {
            FileUtils.copyInputStreamToFile(tarInputStream, imageTarFile);
        }
    }
}
