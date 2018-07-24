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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.docker.dockerinspector.InspectorImages;
import com.blackducksoftware.integration.hub.docker.dockerinspector.config.Config;
import com.blackducksoftware.integration.hub.docker.dockerinspector.config.ProgramPaths;
import com.blackducksoftware.integration.hub.docker.dockerinspector.dockerclient.DockerClientManager;
import com.blackducksoftware.integration.hub.docker.dockerinspector.dockerclient.HubDockerClient;
import com.blackducksoftware.integration.hub.docker.dockerinspector.restclient.response.SimpleResponse;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.imageinspector.api.ImageInspectorOsEnum;
import com.blackducksoftware.integration.rest.RestConstants;
import com.blackducksoftware.integration.rest.connection.RestConnection;
import com.blackducksoftware.integration.rest.exception.IntegrationRestException;
import com.github.dockerjava.api.model.Container;

public abstract class ImageInspectorClient {
    private static final String HUB_IMAGEINSPECTOR_WS_APPNAME = "hub-imageinspector-ws";
    private final String II_SERVICE_URI_SCHEME = "http";
    private final String II_SERVICE_HOST = "localhost";
    private final int MAX_CONTAINER_START_TRY_COUNT = 30;
    private static final long CONTAINER_START_WAIT_MILLISECONDS = 2000L;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Config config;

    @Autowired
    private ImageInspectorServices imageInspectorServices;

    @Autowired
    private DockerClientManager dockerClientManager;

    @Autowired
    private HubDockerClient hubDockerClient;

    @Autowired
    private RestRequestor restRequestor;

    @Autowired
    private RestConnectionCreator restConnectionCreator;

    @Autowired
    private InspectorImages inspectorImages;

    @Autowired
    private ProgramPaths programPaths;

    @Autowired
    private ContainerPaths containerPaths;

    public abstract boolean isApplicable();

    public String getBdio(final String hostPathToTarfile, final String containerPathToInputDockerTarfile, final String givenImageRepo, final String givenImageTag, final String containerPathToOutputFileSystemFile, final boolean cleanup)
            throws IntegrationException {
        // First, try the default inspector service (which will return either the BDIO, or a redirect)
        final ImageInspectorOsEnum inspectorOs = ImageInspectorOsEnum.determineOperatingSystem(config.getImageInspectorDefaultDistro());
        final URI imageInspectorBaseUri = deriveInspectorBaseUri();
        final Predicate<Integer> initialRequestFailureCriteria = statusCode -> statusCode != RestConstants.OK_200 && statusCode != RestConstants.MOVED_TEMP_302 && statusCode != RestConstants.MOVED_PERM_301;
        final SimpleResponse response = getResponseFromService(imageInspectorBaseUri, inspectorOs, containerPathToInputDockerTarfile, givenImageRepo, givenImageTag, containerPathToOutputFileSystemFile, cleanup,
                initialRequestFailureCriteria);
        if (response.getStatusCode() == RestConstants.OK_200) {
            return response.getBody();
        }
        // Handle redirect
        final String redirectUrlString = response.getHeaders().get("Location");
        final String correctImageInspectorOsName = response.getBody().trim();
        logger.info(String.format("This image needs to be inspected on %s using service url %s", correctImageInspectorOsName, redirectUrlString));
        logger.info("(Image inspection may complete faster if you align the default image inspector service with the images you inspect most frequently)");
        final URI correctedImageInspectorBaseUri = deriveInspectorBaseUri(redirectUrlString);
        final ImageInspectorOsEnum correctedInspectorOs = ImageInspectorOsEnum.determineOperatingSystem(correctImageInspectorOsName);
        final Predicate<Integer> correctedRequestFailureCriteria = statusCode -> statusCode != RestConstants.OK_200;
        final SimpleResponse responseFromCorrectedContainer = getResponseFromService(correctedImageInspectorBaseUri, correctedInspectorOs, containerPathToInputDockerTarfile, givenImageRepo, givenImageTag,
                containerPathToOutputFileSystemFile,
                cleanup,
                correctedRequestFailureCriteria);
        return responseFromCorrectedContainer.getBody();
    }

    private SimpleResponse getResponseFromService(final URI imageInspectorBaseUri, final ImageInspectorOsEnum inspectorOs, final String containerPathToInputDockerTarfile,
            final String givenImageRepo, final String givenImageTag,
            final String containerPathToOutputFileSystemFile, final boolean cleanup, final Predicate<Integer> failureTest) throws IntegrationException, HubIntegrationException {
        SimpleResponse response = null;
        String serviceContainerId = null;
        RestConnection restConnection = null;
        try {
            restConnection = createRestConnection(imageInspectorBaseUri, deriveTimeoutSeconds());
            final boolean serviceIsUp = checkServiceHealth(restConnection, imageInspectorBaseUri);
            if (serviceIsUp) {
                Optional<Container> container = Optional.empty();
                // TODO: don't like this check (too obscure). Need a better way to distinguish docker from non-docker(=cloud)
                if (StringUtils.isBlank(config.getImageInspectorUrl())) {
                    container = dockerClientManager.getRunningContainerByAppName(hubDockerClient.getDockerClient(), HUB_IMAGEINSPECTOR_WS_APPNAME, inspectorOs);
                }
                if (container.isPresent()) {
                    serviceContainerId = container.get().getId();
                }
            } else {
                serviceContainerId = startService(restConnection, imageInspectorBaseUri, inspectorOs);
            }
            try {
                logger.info(String.format("Sending getBdio request to: %s (%s)", imageInspectorBaseUri.toString(), inspectorOs.name()));
                response = restRequestor.executeGetBdioRequest(restConnection, imageInspectorBaseUri, containerPathToInputDockerTarfile,
                        givenImageRepo, givenImageTag, containerPathToOutputFileSystemFile, cleanup);
            } catch (final IntegrationException e) {
                logServiceError(serviceContainerId);
                throw e;
            }
            final int statusCode = response.getStatusCode();
            logger.debug(String.format("Response StatusCode: %d", statusCode));
            final Map<String, String> headers = response.getHeaders();
            for (final String key : headers.keySet()) {
                logger.debug(String.format("Header: %s=%s", key, headers.get(key)));
            }
            logger.debug(String.format("If you want the log from the image inspector service, execute this command: docker logs %s. If the container is no longer running, set cleanup.inspector.container=true", serviceContainerId));
            if (failureTest.test(statusCode)) {
                logServiceError(serviceContainerId);
                final String warningHeaderValue = response.getWarningHeaderValue();
                final String responseBody = response.getBody();
                throw new IntegrationRestException(statusCode, warningHeaderValue,
                        String.format("There was a problem trying to getBdio. Error: %d; Warning header: '%s'; Body: '%s'", statusCode, warningHeaderValue,
                                responseBody));
            }
        } finally {
            if (restConnection != null) {
                try {
                    restConnection.close();
                } catch (final Exception initialRestConnectionCloseException) {
                    logger.warn(String.format("Error closing initial rest connection: %s", initialRestConnectionCloseException.getMessage()));
                }
            }
            if (config.isCleanupInspectorContainer()) {
                if (serviceContainerId != null) {
                    dockerClientManager.stopRemoveContainer(serviceContainerId);
                }
            }
        }
        return response;
    }

    private void logServiceError(final String containerId) {
        if (containerId == null) {
            logger.error("Request to image inspector service failed. To see image inspector service logs, look at the image inspector container's log");
        } else {
            dockerClientManager.logServiceLogAsDebug(containerId);
        }
    }

    private int deriveTimeoutSeconds() {
        return (int) (config.getCommandTimeout() / 1000L);
    }

    private URI deriveInspectorBaseUri(final String fullUrlString) throws IntegrationException {
        URI imageInspectorUri;
        try {
            final URI fullUri = new URI(fullUrlString);
            imageInspectorUri = new URI(fullUri.getScheme(), null, fullUri.getHost(), fullUri.getPort(), null, null, null);
        } catch (final URISyntaxException e) {
            throw new IntegrationException(String.format("Error parsing inspector URL %s: %s", fullUrlString, e.getMessage()), e);
        }
        logger.debug(String.format("ImageInspector URL: %s", imageInspectorUri.toString()));
        return imageInspectorUri;
    }

    private URI deriveInspectorBaseUri() throws IntegrationException {
        URI imageInspectorUri;
        try {
            if (StringUtils.isNotBlank(config.getImageInspectorUrl())) {
                imageInspectorUri = new URI(config.getImageInspectorUrl());
                logger.debug(String.format("Using given image inspector url %s", imageInspectorUri.toString()));
            } else {
                // TODO this code belongs in ImageInspectorClientStartServices
                logger.debug(String.format("Will construct image inspector url for: %s", II_SERVICE_HOST));
                imageInspectorUri = createLocalHostUri(imageInspectorServices.getDefaultImageInspectorHostPortBasedOnDistro());
            }
        } catch (final URISyntaxException e) {
            throw new IntegrationException(String.format("Error deriving inspector URL: %s", e.getMessage()), e);
        }
        logger.debug(String.format("ImageInspector URL: %s", imageInspectorUri.toString()));
        return imageInspectorUri;
    }

    private URI createLocalHostUri(final int inspectorPort) throws URISyntaxException {
        return new URI(II_SERVICE_URI_SCHEME, null, II_SERVICE_HOST, inspectorPort,
                null, null, null);
    }

    private RestConnection createRestConnection(final URI imageInspectorUri, final int serviceRequestTimeoutSeconds) throws IntegrationException {
        logger.debug(String.format("Creating a rest connection (%d second timeout) for URL: %s", serviceRequestTimeoutSeconds, imageInspectorUri.toString()));
        RestConnection restConnection;
        try {
            restConnection = restConnectionCreator.createNonRedirectingConnection(imageInspectorUri, serviceRequestTimeoutSeconds);
        } catch (final MalformedURLException e) {
            throw new IntegrationException(String.format("Error creating connection for URL: %s, timeout: %d", imageInspectorUri.toString(), serviceRequestTimeoutSeconds), e);
        }
        return restConnection;
    }

    private String startService(final RestConnection restConnection, final URI imageInspectorUri, final ImageInspectorOsEnum inspectorOs) throws IntegrationException, HubIntegrationException {
        boolean serviceIsUp;
        logger.info(String.format("Service %s (%s) is not running; starting it...", imageInspectorUri.toString(), inspectorOs.name()));
        if (config.isCleanupInspectorContainer()) {
            logger.info("(Image inspection may complete faster if you set cleanup.inspector.container=false)");
        }
        final String imageInspectorRepo;
        final String imageInspectorTag;
        try {
            imageInspectorRepo = inspectorImages.getInspectorImageName(inspectorOs.getRawOs());
            imageInspectorTag = inspectorImages.getInspectorImageTag(inspectorOs.getRawOs());
        } catch (final IOException e) {
            throw new IntegrationException(String.format("Error getting image inspector container repo/tag for %s inspector: %s", inspectorOs.name()), e);
        }
        logger.debug(String.format("Need to pull/run image %s:%s to start the %s service", imageInspectorRepo, imageInspectorTag, imageInspectorUri.toString()));
        final String containerId = pullImageStartContainer(inspectorOs, imageInspectorRepo, imageInspectorTag);
        serviceIsUp = waitForServiceReady(restConnection, imageInspectorUri);
        if (!serviceIsUp) {
            throw new IntegrationException(String.format("Tried to start image imspector container %s:%s, but service %s never came online", imageInspectorRepo, imageInspectorTag, imageInspectorUri.toString()));
        }
        return containerId;
    }

    // TODO the stuff that pulls and starts should be in ImageInspectorClientStartServices
    private String pullImageStartContainer(final ImageInspectorOsEnum inspectorOs, final String imageInspectorRepo, final String imageInspectorTag) throws HubIntegrationException, IntegrationException {
        final String imageId = dockerClientManager.pullImage(imageInspectorRepo, imageInspectorTag);
        final int containerPort = imageInspectorServices.getImageInspectorContainerPort(inspectorOs);
        final int hostPort = imageInspectorServices.getImageInspectorHostPort(inspectorOs);
        final String containerName = programPaths.deriveContainerName(imageInspectorRepo);
        String containerId;
        try {
            containerId = dockerClientManager.startContainerAsService(imageId, containerName, inspectorOs, containerPort, hostPort,
                    containerPaths.getContainerPathToOutputDir(), createLocalHostUri(config.getImageInspectorHostPortAlpine()).toString(),
                    createLocalHostUri(config.getImageInspectorHostPortCentos()).toString(), createLocalHostUri(config.getImageInspectorHostPortUbuntu()).toString());
        } catch (final URISyntaxException e) {
            throw new HubIntegrationException(String.format("Error deriving image inspector URL from port: %s", e.getMessage()), e);
        }
        return containerId;
    }

    private boolean waitForServiceReady(final RestConnection restConnection, final URI imageInspectorUri) throws IntegrationException {
        boolean serviceIsUp = false;
        for (int tryIndex = 0; tryIndex < MAX_CONTAINER_START_TRY_COUNT && !serviceIsUp; tryIndex++) {
            try {
                final long timeoutMilliseconds = CONTAINER_START_WAIT_MILLISECONDS;
                logger.debug(String.format("Pausing %d seconds to give service time to start up", (int) (timeoutMilliseconds / 1000L)));
                Thread.sleep(timeoutMilliseconds);
            } catch (final InterruptedException e) {
                logger.error("Interrupted exception thrown while pausing so image imspector container could start", e);
            }
            logger.debug(String.format("Checking service %s to see if it is up; attempt %d of %d", imageInspectorUri.toString(), tryIndex + 1, MAX_CONTAINER_START_TRY_COUNT));
            serviceIsUp = checkServiceHealth(restConnection, imageInspectorUri);
        }
        return serviceIsUp;
    }

    private boolean checkServiceHealth(final RestConnection restConnection, final URI imageInspectorUri) throws IntegrationException {
        logger.debug(String.format("Sending request for health check to: %s", imageInspectorUri));
        String healthCheckResponse;
        try {
            healthCheckResponse = restRequestor.executeSimpleGetRequest(restConnection, imageInspectorUri, "health");
        } catch (final IntegrationException e) {
            logger.debug(String.format("Health check failed: %s", e.getMessage()));
            return false;
        }
        logger.debug(String.format("ImageInspector health check response: %s", healthCheckResponse));
        final boolean serviceIsUp = healthCheckResponse.contains("\"status\":\"UP\"");
        return serviceIsUp;
    }
}
