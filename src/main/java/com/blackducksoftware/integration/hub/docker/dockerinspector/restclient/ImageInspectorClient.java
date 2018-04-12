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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.request.Request;
import com.blackducksoftware.integration.hub.request.Response;
import com.blackducksoftware.integration.hub.rest.HttpMethod;
import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.blackducksoftware.integration.hub.rest.UnauthenticatedRestConnectionBuilder;
import com.blackducksoftware.integration.log.IntLogger;
import com.blackducksoftware.integration.log.Slf4jIntLogger;

@Component
public class ImageInspectorClient {
    private static final int INSPECT_TIMEOUT_SECONDS = 240;
    private static final String CLEANUP_QUERY_PARAM = "cleanup";
    private static final String CONTAINER_OUTPUT_PATH = "/opt/blackduck/shared/output";
    private static final String RESULTING_CONTAINER_FS_PATH_QUERY_PARAM = "resultingcontainerfspath";
    private static final String TARFILE_QUERY_PARAM = "tarfile";
    private static final String GETBDIO_ENDPOINT = "getbdio";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public String getBdio(final String imageInspectorUrl, final String containerPathToTarfile, final String containerFileSystemFilename, final boolean cleanup) throws IntegrationException, MalformedURLException {
        logger.info(String.format("ImageInspector URL: %s", imageInspectorUrl));
        // TODO with the redirect, a long timeout is required
        final int timeoutSeconds = INSPECT_TIMEOUT_SECONDS;
        final RestConnection restConnection = createConnection(imageInspectorUrl, timeoutSeconds);
        String containerFileSystemQueryString = "";
        if (containerFileSystemFilename != null) {
            containerFileSystemQueryString = String.format("&%s=%s/%s", RESULTING_CONTAINER_FS_PATH_QUERY_PARAM, CONTAINER_OUTPUT_PATH, containerFileSystemFilename);
        }
        final String url = String.format("%s/%s?%s=%s&%s=%b%s",
                imageInspectorUrl, GETBDIO_ENDPOINT, TARFILE_QUERY_PARAM, containerPathToTarfile, CLEANUP_QUERY_PARAM, cleanup, containerFileSystemQueryString);
        logger.debug(String.format("Doing a GET (%d second timeout) on %s", timeoutSeconds, url));
        final Request request = new Request.Builder(url).method(HttpMethod.GET).build();
        try (Response response = restConnection.executeRequest(request)) {
            logger.info(String.format("Response: HTTP status: %d", response.getStatusCode()));
            final String responseBody = response.getContentString();
            logger.info(String.format("Response: body: %s", responseBody));
            return responseBody;
        } catch (final IOException | IllegalArgumentException e) {
            throw new IntegrationException(e);
        }
    }

    private RestConnection createConnection(final String baseUrl, final int timeoutSeconds) throws MalformedURLException {
        final UnauthenticatedRestConnectionBuilder connectionBuilder = new UnauthenticatedRestConnectionBuilder();
        connectionBuilder.setBaseUrl(baseUrl);
        connectionBuilder.setTimeout(timeoutSeconds);
        final IntLogger intLogger = new Slf4jIntLogger(logger);
        connectionBuilder.setLogger(intLogger);
        connectionBuilder.setAlwaysTrustServerCertificate(false);

        final RestConnection connection = connectionBuilder.build();
        return connection;
    }
}
