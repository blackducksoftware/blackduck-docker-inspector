/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2019 Synopsys, Inc.
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

import com.synopsys.integration.rest.client.IntHttpClient;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.exception.BlackDuckIntegrationException;
import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import com.synopsys.integration.exception.IntegrationException;

@Component
public class ImageInspectorServices {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final long CONTAINER_START_WAIT_MILLISECONDS = 5000L;
    private final int MAX_CONTAINER_START_TRY_COUNT = 60;

    @Autowired
    private Config config;

    @Autowired
    private HttpRequestor httpRequestor;

    public int getImageInspectorHostPort(final ImageInspectorOsEnum imageInspectorOs) throws BlackDuckIntegrationException {
        if (ImageInspectorOsEnum.ALPINE.equals(imageInspectorOs)) {
            return config.getImageInspectorHostPortAlpine();
        }
        if (ImageInspectorOsEnum.CENTOS.equals(imageInspectorOs)) {
            return config.getImageInspectorHostPortCentos();
        }
        if (ImageInspectorOsEnum.UBUNTU.equals(imageInspectorOs)) {
            return config.getImageInspectorHostPortUbuntu();
        }
        throw new BlackDuckIntegrationException(String.format("Unrecognized ImageInspector OS: %s", imageInspectorOs.toString()));
    }

    public int getImageInspectorContainerPort(final ImageInspectorOsEnum imageInspectorOs) throws BlackDuckIntegrationException {
        if (ImageInspectorOsEnum.ALPINE.equals(imageInspectorOs)) {
            return config.getImageInspectorContainerPortAlpine();
        }
        if (ImageInspectorOsEnum.CENTOS.equals(imageInspectorOs)) {
            return config.getImageInspectorContainerPortCentos();
        }
        if (ImageInspectorOsEnum.UBUNTU.equals(imageInspectorOs)) {
            return config.getImageInspectorContainerPortUbuntu();
        }
        throw new BlackDuckIntegrationException(String.format("Unrecognized ImageInspector OS: %s", imageInspectorOs));
    }

    public int getDefaultImageInspectorHostPortBasedOnDistro() throws IntegrationException {
        final String inspectorOsName = config.getImageInspectorDefaultDistro();
        if ("alpine".equalsIgnoreCase(inspectorOsName)) {
            return config.getImageInspectorHostPortAlpine();
        }
        if ("centos".equalsIgnoreCase(inspectorOsName)) {
            return config.getImageInspectorHostPortCentos();
        }
        if ("ubuntu".equalsIgnoreCase(inspectorOsName)) {
            return config.getImageInspectorHostPortUbuntu();
        }
        throw new IntegrationException(String.format("Invalid value for property image.inspector.default: %s", inspectorOsName));
    }


    public boolean startService(final IntHttpClient httpClient, final URI imageInspectorUri, final String imageInspectorRepo, final String imageInspectorTag) {
        boolean serviceIsUp = false;
        for (int tryIndex = 0; tryIndex < MAX_CONTAINER_START_TRY_COUNT && !serviceIsUp; tryIndex++) {
            try {
                final long timeoutMilliseconds = CONTAINER_START_WAIT_MILLISECONDS;
                logger.debug(String.format("Pausing %d seconds to give service time to start up", (int) (timeoutMilliseconds / 1000L)));
                Thread.sleep(timeoutMilliseconds);
            } catch (final InterruptedException e) {
                logger.error(String.format("Interrupted exception thrown while pausing so image imspector container based on image %s:%s could start", imageInspectorRepo, imageInspectorTag), e);
            }
            logger.debug(String.format("Checking service %s to see if it is up; attempt %d of %d", imageInspectorUri.toString(), tryIndex + 1, MAX_CONTAINER_START_TRY_COUNT));
            serviceIsUp = checkServiceHealth(httpClient, imageInspectorUri);
        }
        return serviceIsUp;
    }

    public boolean checkServiceHealth(final IntHttpClient httpClient, final URI imageInspectorUri) {
        logger.debug(String.format("Sending request for health check to: %s", imageInspectorUri));
        String healthCheckResponse;
        try {
            healthCheckResponse = httpRequestor
                .executeSimpleGetRequest(httpClient, imageInspectorUri, "health");
        } catch (final IntegrationException e) {
            logger.debug(String.format("Health check failed: %s", e.getMessage()));
            return false;
        }
        logger.debug(String.format("ImageInspector health check response: %s", healthCheckResponse));
        final boolean serviceIsUp = healthCheckResponse.contains("\"status\":\"UP\"");
        return serviceIsUp;
    }

    public String getServiceVersion(final IntHttpClient httpClient, final URI imageInspectorUri) {
        logger.debug(String.format("Sending request for service version to: %s", imageInspectorUri));
        String serviceVersionResponse;
        try {
            serviceVersionResponse = httpRequestor
                .executeSimpleGetRequest(httpClient, imageInspectorUri, "getversion");
        } catch (final IntegrationException e) {
            logger.debug(String.format("Get ImageInspector service version request failed: %s", e.getMessage()));
            return "unknown";
        }
        logger.debug(String.format("ImageInspector service version response: %s", serviceVersionResponse));
        return serviceVersionResponse;
    }
}
