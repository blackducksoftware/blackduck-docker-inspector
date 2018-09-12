/**
 * blackduck-docker-inspector
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
package com.synopsys.integration.blackduck.dockerinspector.restclient;

import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.restclient.response.SimpleResponse;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.HttpMethod;
import com.synopsys.integration.rest.connection.RestConnection;
import com.synopsys.integration.rest.request.Request;
import com.synopsys.integration.rest.request.Response;

@Component
public class RestRequestor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String LOGGING_LEVEL_QUERY_PARAM = "logginglevel";
    private static final String CLEANUP_QUERY_PARAM = "cleanup";
    private static final String FORGE_DERIVED_FROM_DISTRO_QUERY_PARAM = "forgederivedfromdistro";
    private static final String RESULTING_CONTAINER_FS_PATH_QUERY_PARAM = "resultingcontainerfspath";
    private static final String IMAGE_REPO_QUERY_PARAM = "imagerepo";
    private static final String IMAGE_TAG_QUERY_PARAM = "imagetag";
    private static final String TARFILE_QUERY_PARAM = "tarfile";
    private static final String GETBDIO_ENDPOINT = "getbdio";

    private static final String BASE_LOGGER_NAME = "com.blackducksoftware";

    public SimpleResponse executeGetBdioRequest(final RestConnection restConnection, final URI imageInspectorUri, final String containerPathToTarfile,
            final String givenImageRepo, final String givenImageTag, final String containerPathToContainerFileSystemFile, final boolean cleanup,
            final boolean forgeDerivedFromDistro)
            throws IntegrationException {
        String containerFileSystemQueryString = "";
        if (StringUtils.isNotBlank(containerPathToContainerFileSystemFile)) {
            containerFileSystemQueryString = String.format("&%s=%s", RESULTING_CONTAINER_FS_PATH_QUERY_PARAM, containerPathToContainerFileSystemFile);
        }
        String imageRepoQueryString = "";
        if (StringUtils.isNotBlank(givenImageRepo)) {
            imageRepoQueryString = String.format("&%s=%s", IMAGE_REPO_QUERY_PARAM, givenImageRepo);
        }
        String imageTagQueryString = "";
        if (StringUtils.isNotBlank(givenImageTag)) {
            imageTagQueryString = String.format("&%s=%s", IMAGE_TAG_QUERY_PARAM, givenImageTag);
        }
        String forgeDerivedFromDistroQueryString = "";
        if (forgeDerivedFromDistro) {
            forgeDerivedFromDistroQueryString = String.format("&%s=true", FORGE_DERIVED_FROM_DISTRO_QUERY_PARAM);
        }
        final String url = String.format("%s/%s?%s=%s&%s=%s&%s=%b%s%s%s%s",
                imageInspectorUri.toString(), GETBDIO_ENDPOINT, LOGGING_LEVEL_QUERY_PARAM, getLoggingLevel(), TARFILE_QUERY_PARAM, containerPathToTarfile, CLEANUP_QUERY_PARAM, cleanup, containerFileSystemQueryString,
                imageRepoQueryString, imageTagQueryString, forgeDerivedFromDistroQueryString);
        logger.debug(String.format("Doing a getBdio request on %s", url));
        final Request request = new Request.Builder(url).method(HttpMethod.GET).build();
        try (Response response = restConnection.executeRequest(request)) {
            logger.debug(String.format("Response: HTTP status: %d", response.getStatusCode()));
            return new SimpleResponse(response.getStatusCode(), response.getHeaders(), getResponseBody(response));
        } catch (final Exception e) {
            logger.info(String.format("getBdio request on %s failed: %s", url, e.getMessage()));
            throw new IntegrationException(e);
        }
    }

    public String executeSimpleGetRequest(final RestConnection restConnection, final URI imageInspectorUri, String endpoint)
            throws IntegrationException {
        if (endpoint.startsWith("/")) {
            endpoint = endpoint.substring(1);
        }
        final String url = String.format("%s/%s", imageInspectorUri.toString(), endpoint);
        logger.debug(String.format("Doing a GET on %s", url));
        final Request request = new Request.Builder(url).method(HttpMethod.GET).build();
        try (Response response = restConnection.executeRequest(request)) {
            logger.debug(String.format("Response: HTTP status: %d", response.getStatusCode()));
            return getResponseBody(response);
        } catch (final Exception e) {
            logger.debug(String.format("GET on %s failed: %s", url, e.getMessage()));
            throw new IntegrationException(e);
        }
    }

    private String getLoggingLevel() {
        String loggingLevel = "INFO";
        try {
            final ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(BASE_LOGGER_NAME);
            loggingLevel = root.getLevel().toString();
            logger.debug(String.format("Logging level: %s", loggingLevel));
        } catch (final Exception e) {
            logger.debug(String.format("No logging level set. Defaulting to %s", loggingLevel));
        }
        return loggingLevel;
    }

    private String getResponseBody(final Response response) throws IntegrationException {
        final String responseBody = response.getContentString();
        logger.trace(String.format("Response: body: %s", responseBody));
        return responseBody;
    }
}
