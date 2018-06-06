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
import java.util.Map;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.docker.dockerinspector.InspectorImages;
import com.blackducksoftware.integration.hub.docker.dockerinspector.config.Config;
import com.blackducksoftware.integration.hub.docker.dockerinspector.config.ProgramPaths;
import com.blackducksoftware.integration.hub.docker.dockerinspector.dockerclient.DockerClientManager;
import com.blackducksoftware.integration.hub.docker.dockerinspector.dockerclient.HubDockerClient;
import com.blackducksoftware.integration.hub.docker.dockerinspector.restclient.response.SimpleResponse;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.imageinspector.lib.OperatingSystemEnum;
import com.blackducksoftware.integration.rest.RestConstants;
import com.blackducksoftware.integration.rest.connection.RestConnection;
import com.blackducksoftware.integration.rest.exception.IntegrationRestException;
import com.github.dockerjava.api.model.Container;

@Component
public class ImageInspectorClientContainersStartedAsNeeded implements ImageInspectorClient {
    private static final String HUB_IMAGEINSPECTOR_WS_APPNAME = "hub-imageinspector-ws";
    private static final long CONTAINER_START_WAIT_MILLISECONDS = 2000L;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final int MAX_CONTAINER_START_TRY_COUNT = 30;

    @Autowired
    private Config config;

    @Autowired
    private ImageInspectorServices imageInspectorServices;

    @Autowired
    private RestConnectionCreator restConnectionCreator;

    @Autowired
    private RestRequestor restRequestor;

    @Autowired
    private InspectorImages inspectorImages;

    @Autowired
    private DockerClientManager dockerClientManager;

    @Autowired
    private ProgramPaths programPaths;

    @Autowired
    private ContainerPaths containerPaths;

    @Autowired
    private HubDockerClient hubDockerClient;

    @Override
    public boolean isApplicable() {
        final boolean answer = config.isImageInspectorServiceStart();
        logger.debug(String.format("isApplicable() returning %b", answer));
        return answer;
    }

    @Override
    public String getBdio(final String hostPathToTarfile, final String containerPathToInputDockerTarfile, final String containerPathToOutputFileSystemFile, final boolean cleanup) throws IntegrationException {
        logger.debug(String.format("getBdio(): containerPathToTarfile: %s", containerPathToInputDockerTarfile));

        // First, try the default inspector service (which will return either the BDIO, or a redirect)
        final OperatingSystemEnum inspectorOs = OperatingSystemEnum.determineOperatingSystem(config.getImageInspectorDefault());
        final String imageInspectorUrl = deriveInspectorUrl(imageInspectorServices.getDefaultImageInspectorHostPort());
        final SimpleResponse response = getResponse(imageInspectorUrl, inspectorOs, containerPathToInputDockerTarfile, containerPathToOutputFileSystemFile, cleanup,
                statusCode -> statusCode != RestConstants.OK_200 && statusCode != RestConstants.MOVED_TEMP_302 && statusCode != RestConstants.MOVED_PERM_301);
        if (response.getStatusCode() == RestConstants.OK_200) {
            return response.getBody();
        }
        final String correctImageInspectorOsName = response.getBody().trim();
        logger.debug(String.format("correctImageInspectorOs: %s", correctImageInspectorOsName));

        // Handle redirect
        final OperatingSystemEnum correctedInspectorOs = OperatingSystemEnum.determineOperatingSystem(correctImageInspectorOsName);
        final String correctedImageInspectorUrl = deriveInspectorUrl(imageInspectorServices.getImageInspectorHostPort(correctedInspectorOs));
        final SimpleResponse responseFromCorrectedContainer = getResponse(correctedImageInspectorUrl, correctedInspectorOs, containerPathToInputDockerTarfile, containerPathToOutputFileSystemFile, cleanup,
                statusCode -> statusCode != RestConstants.OK_200);
        return responseFromCorrectedContainer.getBody();
    }

    private SimpleResponse getResponse(final String imageInspectorUrl, final OperatingSystemEnum inspectorOs, final String containerPathToInputDockerTarfile,
            final String containerPathToOutputFileSystemFile, final boolean cleanup, final Predicate<Integer> failureTest) throws IntegrationException, HubIntegrationException {
        SimpleResponse response = null;
        String serviceContainerId = null;
        RestConnection restConnection = null;
        try {
            restConnection = createRestConnection(imageInspectorUrl, deriveTimeoutSeconds());
            serviceContainerId = ensureServiceReady(restConnection, imageInspectorUrl, inspectorOs);
            logger.debug(String.format("Sending getBdio request to: %s", imageInspectorUrl));
            try {
                response = restRequestor.executeGetBdioRequest(restConnection, imageInspectorUrl, containerPathToInputDockerTarfile, containerPathToOutputFileSystemFile, cleanup);
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
            logger.debug(String.format("If you want the log from the image inspector service, execute this command: docker logs %s", serviceContainerId));
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

    private void logServiceError(final String correctedContainerId) {
        if (logger.isDebugEnabled()) {
            dockerClientManager.logServiceLogAsDebug(correctedContainerId);
        } else {
            logger.error(String.format("Request to image inspector service failed. To see image inspector service logs, set the Docker Inspector logging level to DEBUG, or execute the following command: 'docker logs %s'",
                    correctedContainerId));
        }
    }

    private int deriveTimeoutSeconds() {
        return (int) (config.getCommandTimeout() / 1000L);
    }

    private String deriveInspectorUrl(final int inspectorPort) {
        final String imageInspectorUrl = String.format("http://localhost:%d", inspectorPort);
        logger.info(String.format("ImageInspector URL: %s", imageInspectorUrl));
        return imageInspectorUrl;
    }

    private RestConnection createRestConnection(final String imageInspectorUrl, final int serviceRequestTimeoutSeconds) throws IntegrationException {
        logger.debug(String.format("Creating a rest connection (%d second timeout) for URL: %s", serviceRequestTimeoutSeconds, imageInspectorUrl));
        RestConnection restConnection;
        try {
            restConnection = restConnectionCreator.createNonRedirectingConnection(imageInspectorUrl, serviceRequestTimeoutSeconds);
        } catch (final MalformedURLException e) {
            throw new IntegrationException(String.format("Error creating connection for URL: %s, timeout: %d", imageInspectorUrl, serviceRequestTimeoutSeconds), e);
        }
        return restConnection;
    }

    private String ensureServiceReady(final RestConnection restConnection, final String imageInspectorUrl, final OperatingSystemEnum inspectorOs) throws IntegrationException {
        boolean serviceIsUp = checkServiceHealth(restConnection, imageInspectorUrl);
        if (serviceIsUp) {
            final Container container = dockerClientManager.getRunningContainerByAppName(hubDockerClient.getDockerClient(), HUB_IMAGEINSPECTOR_WS_APPNAME, imageInspectorServices.getDefaultImageInspectorOs());
            return container.getId();
        }
        // Need to spin up container
        final String imageInspectorRepo;
        final String imageInspectorTag;
        try {
            imageInspectorRepo = inspectorImages.getInspectorImageName(inspectorOs);
            imageInspectorTag = inspectorImages.getInspectorImageTag(inspectorOs);
        } catch (final IOException e) {
            throw new IntegrationException(String.format("Error getting image inspector container repo/tag for default inspector OS: %s", inspectorOs.name()), e);
        }
        logger.debug(String.format("Need to pull/run %s:%s", imageInspectorRepo, imageInspectorTag));
        final String imageId = dockerClientManager.pullImage(imageInspectorRepo, imageInspectorTag);
        final int containerPort = imageInspectorServices.getImageInspectorContainerPort(inspectorOs);
        final int hostPort = imageInspectorServices.getImageInspectorHostPort(inspectorOs);
        final String containerName = programPaths.deriveContainerName(imageInspectorRepo);
        final String containerId = dockerClientManager.startContainerAsService(imageId, containerName, inspectorOs, containerPort, hostPort,
                containerPaths.getContainerPathToOutputDir());
        serviceIsUp = startService(restConnection, imageInspectorUrl, imageInspectorRepo, imageInspectorTag);
        if (!serviceIsUp) {
            throw new IntegrationException(String.format("Tried to start image imspector container %s:%s, but service %s never came online", imageInspectorRepo, imageInspectorTag, imageInspectorUrl));
        }
        return containerId;
    }

    private boolean startService(final RestConnection restConnection, final String imageInspectorUrl, final String imageInspectorRepo, final String imageInspectorTag) throws IntegrationException {
        boolean serviceIsUp = false;
        for (int tryIndex = 0; tryIndex < MAX_CONTAINER_START_TRY_COUNT && !serviceIsUp; tryIndex++) {
            try {
                final long timeoutMilliseconds = CONTAINER_START_WAIT_MILLISECONDS;
                logger.debug(String.format("Pausing %d seconds to give service time to start up", (int) (timeoutMilliseconds / 1000L)));
                Thread.sleep(timeoutMilliseconds);
            } catch (final InterruptedException e) {
                logger.error(String.format("Interrupted exception thrown while pausing so image imspector container based on image %s:%s could start", imageInspectorRepo, imageInspectorTag), e);
            }
            logger.debug(String.format("Checking service %s to see if it is up; attempt %d of %d", imageInspectorUrl, tryIndex + 1, MAX_CONTAINER_START_TRY_COUNT));
            serviceIsUp = checkServiceHealth(restConnection, imageInspectorUrl);
        }
        return serviceIsUp;
    }

    private boolean checkServiceHealth(final RestConnection restConnection, final String imageInspectorUrl) throws IntegrationException {
        logger.debug(String.format("Sending request for health check to: %s", imageInspectorUrl));
        String healthCheckResponse;
        try {
            healthCheckResponse = restRequestor.executeSimpleGetRequest(restConnection, imageInspectorUrl, "health");
        } catch (final IntegrationException e) {
            logger.debug(String.format("Health check failed: %s", e.getMessage()));
            return false;
        }
        logger.debug(String.format("ImageInspector health check response: %s", healthCheckResponse));
        final boolean serviceIsUp = healthCheckResponse.contains("\"status\":\"UP\"");
        return serviceIsUp;
    }
}
