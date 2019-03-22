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
package com.synopsys.integration.blackduck.dockerinspector.httpclient;

import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;
import com.synopsys.integration.rest.client.IntHttpClient;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.dockerjava.api.model.Container;
import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.ProgramPaths;
import com.synopsys.integration.blackduck.dockerinspector.dockerclient.DockerClientManager;
import com.synopsys.integration.blackduck.dockerinspector.httpclient.response.SimpleResponse;
import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.RestConstants;
import com.synopsys.integration.rest.exception.IntegrationRestException;

@Component
public class ImageInspectorClientStartServices implements ImageInspectorClient {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String II_SERVICE_URI_SCHEME = "http";
    private final String II_SERVICE_HOST = "localhost";

    @Autowired
    private Config config;

    @Autowired
    private ProgramVersion programVersion;

    @Autowired
    private ImageInspectorServices imageInspectorServices;

    @Autowired
    private HttpConnectionCreator httpConnectionCreator;

    @Autowired
    private HttpRequestor httpRequestor;

    @Autowired
    private InspectorImages inspectorImages;

    @Autowired
    private DockerClientManager dockerClientManager;

    @Autowired
    private ProgramPaths programPaths;

    @Autowired
    private ContainerName containerNameManager;

    @Override
    public boolean isApplicable() {
        final boolean answer = config.isImageInspectorServiceStart();
        logger.debug(String.format("isApplicable() returning %b", answer));
        return answer;
    }

    @Override
    public File copyTarfileToSharedDir(final File givenDockerTarfile) throws IOException {
        // Copy the tarfile to the shared/target dir
        final File finalDockerTarfile = new File(programPaths.getDockerInspectorTargetDirPath(), givenDockerTarfile.getName());
        logger.debug(String.format("Required docker tarfile location: %s", finalDockerTarfile.getCanonicalPath()));
        if (!finalDockerTarfile.getCanonicalPath().equals(givenDockerTarfile.getCanonicalPath())) {
            logger.debug(String.format("Copying %s to %s", givenDockerTarfile.getCanonicalPath(), finalDockerTarfile.getCanonicalPath()));
            FileUtils.copyFile(givenDockerTarfile, finalDockerTarfile);
        }
        logger.debug(String.format("Final docker tar file path: %s", finalDockerTarfile.getCanonicalPath()));
        return finalDockerTarfile;
    }

    @Override
    public String getBdio(final String hostPathToTarfile, final String containerPathToInputDockerTarfile, final String givenImageRepo, final String givenImageTag, final String containerPathToOutputFileSystemFile,
        final boolean organizeComponentsByLayer, final boolean includeRemovedComponents, final boolean cleanup,
        final String platformTopLayerId)
        throws IntegrationException {
        logger.info(dockerClientManager.getDockerJavaLibraryVersion());

        // First, try the default inspector service (which will return either the BDIO, or a redirect)
        final ImageInspectorOsEnum inspectorOs = ImageInspectorOsEnum.determineOperatingSystem(config.getImageInspectorDefaultDistro());
        final URI imageInspectorBaseUri = deriveInspectorBaseUri(imageInspectorServices.getDefaultImageInspectorHostPortBasedOnDistro());
        final Predicate<Integer> initialRequestFailureCriteria = statusCode -> statusCode != RestConstants.OK_200 && statusCode != RestConstants.MOVED_TEMP_302 && statusCode != RestConstants.MOVED_PERM_301;
        final SimpleResponse response = getResponseFromService(imageInspectorBaseUri, inspectorOs, containerPathToInputDockerTarfile, givenImageRepo, givenImageTag, containerPathToOutputFileSystemFile, organizeComponentsByLayer,
            includeRemovedComponents, cleanup, platformTopLayerId,
            initialRequestFailureCriteria);
        if (response.getStatusCode() == RestConstants.OK_200) {
            return response.getBody();
        }
        if (response.getStatusCode() >= RestConstants.BAD_REQUEST_400) {
            throw new IntegrationException(String.format("getBdio request returned status: %d: %s", response.getStatusCode(), response.getBody()));
        }
        final String correctImageInspectorOsName = response.getBody().trim();
        logger.info(String.format("This image needs to be inspected on %s", correctImageInspectorOsName));
        logger.info("(Image inspection may complete faster if you align the value of property imageinspector.service.distro.default with the images you inspect most frequently)");

        // Handle redirect
        final ImageInspectorOsEnum correctedInspectorOs = ImageInspectorOsEnum.determineOperatingSystem(correctImageInspectorOsName);
        final URI correctedImageInspectorBaseUri = deriveInspectorBaseUri(imageInspectorServices.getImageInspectorHostPort(correctedInspectorOs));
        final Predicate<Integer> correctedRequestFailureCriteria = statusCode -> statusCode != RestConstants.OK_200;
        final SimpleResponse responseFromCorrectedContainer = getResponseFromService(correctedImageInspectorBaseUri, correctedInspectorOs, containerPathToInputDockerTarfile, givenImageRepo, givenImageTag,
            containerPathToOutputFileSystemFile,
            organizeComponentsByLayer, includeRemovedComponents,
            cleanup, platformTopLayerId,
            correctedRequestFailureCriteria);
        return responseFromCorrectedContainer.getBody();
    }

    private SimpleResponse getResponseFromService(final URI imageInspectorUri, final ImageInspectorOsEnum inspectorOs, final String containerPathToInputDockerTarfile,
        final String givenImageRepo, final String givenImageTag,
        final String containerPathToOutputFileSystemFile, final boolean organizeComponentsByLayer, final boolean includeRemovedComponents, final boolean cleanup, final String platformTopLayerId, final Predicate<Integer> failureTest)
        throws IntegrationException {
        SimpleResponse response = null;
        ContainerDetails serviceContainerDetails = null;
        IntHttpClient restConnection = null;
        try {
            restConnection = createRestConnection(imageInspectorUri, deriveTimeoutSeconds());
            serviceContainerDetails = ensureServiceReady(restConnection, imageInspectorUri, inspectorOs);
            try {
                logger.info(String.format("Sending getBdio request to: %s (%s)", imageInspectorUri.toString(), inspectorOs.name()));
                response = httpRequestor.executeGetBdioRequest(restConnection, imageInspectorUri, containerPathToInputDockerTarfile,
                    givenImageRepo, givenImageTag, containerPathToOutputFileSystemFile, organizeComponentsByLayer, includeRemovedComponents, cleanup,
                    platformTopLayerId);
                logServiceLogIfDebug(serviceContainerDetails.getContainerId());
            } catch (final IntegrationException e) {
                logServiceError(serviceContainerDetails.getContainerId());
                throw e;
            }
            final int statusCode = response.getStatusCode();
            logger.debug(String.format("Response StatusCode: %d", statusCode));
            final Map<String, String> headers = response.getHeaders();
            for (final String key : headers.keySet()) {
                logger.debug(String.format("Header: %s=%s", key, headers.get(key)));
            }
            logger.debug(String.format("If you want the log from the image inspector service, execute this command: docker logs %s. If the container is no longer running, set cleanup.inspector.container=false and run again",
                serviceContainerDetails.getContainerId()));
            if (failureTest.test(statusCode)) {
                logServiceError(serviceContainerDetails.getContainerId());
                final String warningHeaderValue = response.getWarningHeaderValue();
                final String responseBody = response.getBody();
                throw new IntegrationRestException(statusCode, warningHeaderValue, responseBody,
                    String.format("There was a problem trying to getBdio. Error: %d; Warning header: '%s'; Body: '%s'", statusCode, warningHeaderValue,
                        responseBody));
            }
        } finally {
            if (serviceContainerDetails == null) {
                logger.trace("Service connection/image/container cleanup: serviceContainerDetails is null");
            } else {
                logger.trace(String.format("Service connection/image/container cleanup: image id: %s, container id: %s", serviceContainerDetails.getImageId(), serviceContainerDetails.getContainerId()));
            }
            if (config.isCleanupInspectorContainer()) {
                if (serviceContainerDetails != null) {
                    dockerClientManager.stopRemoveContainer(serviceContainerDetails.getContainerId());
                }
            }
            if (config.isCleanupInspectorImage()) {
                if (serviceContainerDetails != null) {
                    if (serviceContainerDetails.getImageId() != null) {
                        dockerClientManager.removeImage(serviceContainerDetails.getImageId());
                    }
                }
            }
        }
        return response;
    }

    private void logServiceError(final String correctedContainerId) {
        final boolean serviceLogLogged = logServiceLogIfDebug(correctedContainerId);
        if (!serviceLogLogged) {
            logger.error(String.format("Request to image inspector service failed. To see image inspector service logs, set the Docker Inspector logging level to DEBUG, or execute the following command: 'docker logs %s'",
                correctedContainerId));
        }
    }

    private boolean logServiceLogIfDebug(final String correctedContainerId) {
        if (logger.isDebugEnabled()) {
            dockerClientManager.logServiceLogAsDebug(correctedContainerId);
            return true;
        }
        return false;
    }

    private int deriveTimeoutSeconds() {
        return (int) (config.getServiceTimeout() / 1000L);
    }

    private URI deriveInspectorBaseUri(final int inspectorPort) throws IntegrationException {
        URI imageInspectorUri;
        try {
            if (StringUtils.isNotBlank(config.getImageInspectorUrl())) {
                final URI serviceUri = new URI(config.getImageInspectorUrl());
                imageInspectorUri = new URI(serviceUri.getScheme(), serviceUri.getUserInfo(), serviceUri.getHost(), inspectorPort, serviceUri.getPath(), serviceUri.getQuery(), serviceUri.getFragment());
                logger.debug(String.format("Adjusted image inspector url from %s to %s", config.getImageInspectorUrl(), imageInspectorUri.toString()));
            } else {
                logger.debug(String.format("Will construct image inspector url for: %s", II_SERVICE_HOST));
                imageInspectorUri = new URI(II_SERVICE_URI_SCHEME, null, II_SERVICE_HOST, inspectorPort,
                    null, null, null);
            }
        } catch (final URISyntaxException e) {
            throw new IntegrationException(String.format("Error deriving inspector URL: %s", e.getMessage()), e);
        }
        logger.debug(String.format("ImageInspector URL: %s", imageInspectorUri.toString()));
        return imageInspectorUri;
    }

    private IntHttpClient createRestConnection(final URI imageInspectorUri, final int serviceRequestTimeoutSeconds) throws IntegrationException {
        logger.debug(String.format("Creating a rest connection (%d second timeout) for URL: %s", serviceRequestTimeoutSeconds, imageInspectorUri.toString()));
        IntHttpClient restConnection;
        try {
            restConnection = httpConnectionCreator
                .createNonRedirectingConnection(imageInspectorUri, serviceRequestTimeoutSeconds);
        } catch (final MalformedURLException e) {
            throw new IntegrationException(String.format("Error creating connection for URL: %s, timeout: %d", imageInspectorUri.toString(), serviceRequestTimeoutSeconds), e);
        }
        return restConnection;
    }

    private ContainerDetails ensureServiceReady(final IntHttpClient httpClient, final URI imageInspectorUri, final ImageInspectorOsEnum inspectorOs) throws IntegrationException {
        boolean serviceIsUp = imageInspectorServices.checkServiceHealth(httpClient, imageInspectorUri);
        if (serviceIsUp) {
            final Container container = dockerClientManager.getRunningContainerByAppName(Config.IMAGEINSPECTOR_WS_APPNAME, inspectorOs);
            return new ContainerDetails(null, container.getId());
        }
        logger.info(String.format("Service %s (%s) is not running; starting it...", imageInspectorUri.toString(), inspectorOs.name()));
        if (config.isCleanupInspectorContainer()) {
            logger.info("(Image inspection may complete faster if you set cleanup.inspector.container=false)");
        }
        final String imageInspectorRepo;
        final String imageInspectorTag;
        imageInspectorRepo = inspectorImages.getInspectorImageName(inspectorOs);
        imageInspectorTag = inspectorImages.getInspectorImageTag(inspectorOs);
        logger.debug(String.format("Need to pull/run image %s:%s to start the %s service", imageInspectorRepo, imageInspectorTag, imageInspectorUri.toString()));
        final Optional<String> imageId = pullImageTolerantly(imageInspectorRepo, imageInspectorTag);
        final int containerPort = imageInspectorServices.getImageInspectorContainerPort(inspectorOs);
        final int hostPort = imageInspectorServices.getImageInspectorHostPort(inspectorOs);
        final String containerName = containerNameManager.deriveContainerNameFromImageInspectorRepo(imageInspectorRepo);
        final String containerId = dockerClientManager.startContainerAsService(imageInspectorRepo, imageInspectorTag, containerName, inspectorOs, containerPort, hostPort,
            Config.IMAGEINSPECTOR_WS_APPNAME,
            String.format("%s/%s/%s.jar", Config.CONTAINER_BLACKDUCK_DIR, Config.IMAGEINSPECTOR_WS_APPNAME, Config.IMAGEINSPECTOR_WS_APPNAME),
            deriveInspectorBaseUri(config.getImageInspectorHostPortAlpine()).toString(), deriveInspectorBaseUri(config.getImageInspectorHostPortCentos()).toString(),
            deriveInspectorBaseUri(config.getImageInspectorHostPortUbuntu()).toString());
        final ContainerDetails containerDetails = new ContainerDetails(imageId.orElse(null), containerId);
        serviceIsUp = imageInspectorServices.startService(httpClient, imageInspectorUri, imageInspectorRepo, imageInspectorTag);
        if (!serviceIsUp) {
            dockerClientManager.logServiceLogAsDebug(containerId);
            throw new IntegrationException(String.format("Tried to start image imspector container %s:%s, but service %s never came online", imageInspectorRepo, imageInspectorTag, imageInspectorUri.toString()));
        }
        checkServiceVersion(httpClient, imageInspectorUri);
        return containerDetails;
    }

    private void checkServiceVersion(IntHttpClient httpClient, URI imageInspectorUri) {
        final String serviceVersion = imageInspectorServices.getServiceVersion(httpClient, imageInspectorUri);
        final String expectedServiceVersion = programVersion.getInspectorImageVersion();
        if (!serviceVersion.equals(expectedServiceVersion)) {
            logger.warn(String.format("Expected image inspector service version %s, but the running image inspector service is version %s; This version of Docker Inspector is designed to work with image inspector service version %s. Please stop and remove all running image inspector containers.", expectedServiceVersion, serviceVersion, expectedServiceVersion));
        }
    }

    private Optional<String> pullImageTolerantly(final String imageInspectorRepo, final String imageInspectorTag) {
        Optional<String> imageId = Optional.empty();
        try {
            imageId = Optional.ofNullable(dockerClientManager.pullImage(imageInspectorRepo, imageInspectorTag));
            logger.debug(String.format("Pulled image ID %s", imageId.orElse("<null>")));
        } catch (final Exception e) {
            logger.warn(String.format("Unable to pull docker image %s:%s; proceeding anyway since it may already exist locally", imageInspectorRepo, imageInspectorTag));
        }
        return imageId;
    }

}
