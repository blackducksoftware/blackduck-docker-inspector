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
package com.blackducksoftware.integration.hub.docker.dockerinspector;

import java.io.IOException;
import java.net.MalformedURLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.request.Request;
import com.blackducksoftware.integration.hub.request.Response;
import com.blackducksoftware.integration.hub.rest.HttpMethod;
import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.blackducksoftware.integration.hub.rest.UnauthenticatedRestConnectionBuilder;
import com.blackducksoftware.integration.log.IntLogger;
import com.blackducksoftware.integration.log.Slf4jIntLogger;

public class WebServiceClient {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public void getBdio(final String containerPathToTarfile) throws IntegrationException, MalformedURLException {
        final String baseUrl = "http://192.168.99.100:8080"; // TODO
        final RestConnection restConnection = createConnection(baseUrl);

        final String url = String.format("%s/getbdio?tarfile=%s", baseUrl, containerPathToTarfile);
        final Request request = new Request.Builder(url).method(HttpMethod.GET).build();
        try (Response response = restConnection.executeRequest(request)) {
            final String responseBody = response.getContentString();
            logger.info(String.format("Response: %s", responseBody));
        } catch (final IOException | IllegalArgumentException e) {
            throw new IntegrationException(e);
        }
    }

    private RestConnection createConnection(final String baseUrl) throws MalformedURLException {
        final UnauthenticatedRestConnectionBuilder connectionBuilder = new UnauthenticatedRestConnectionBuilder();
        connectionBuilder.setBaseUrl(baseUrl);
        connectionBuilder.setTimeout(30);
        final IntLogger intLogger = new Slf4jIntLogger(logger);
        connectionBuilder.setLogger(intLogger);
        connectionBuilder.setAlwaysTrustServerCertificate(false);

        final RestConnection connection = connectionBuilder.build();
        return connection;
    }
}
