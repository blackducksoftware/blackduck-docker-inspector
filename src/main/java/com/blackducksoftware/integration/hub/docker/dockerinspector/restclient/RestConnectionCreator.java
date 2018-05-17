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

import java.net.MalformedURLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.blackducksoftware.integration.hub.rest.UnauthenticatedRestConnectionBuilder;
import com.blackducksoftware.integration.log.IntLogger;
import com.blackducksoftware.integration.log.Slf4jIntLogger;

@Component
public class RestConnectionCreator {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public RestConnection createNonRedirectingConnection(final String baseUrl, final int timeoutSeconds) throws MalformedURLException {
        final RestConnection connection = createRedirectingConnection(baseUrl, timeoutSeconds);
        connection.getClientBuilder().disableRedirectHandling();
        return connection;

    }

    public RestConnection createRedirectingConnection(final String baseUrl, final int timeoutSeconds) throws MalformedURLException {
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
