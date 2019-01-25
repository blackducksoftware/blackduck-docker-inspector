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

import com.synopsys.integration.blackduck.dockerinspector.httpclient.connection.NonRedirectingIntHttpClient;
import com.synopsys.integration.rest.client.IntHttpClient;
import com.synopsys.integration.rest.proxy.ProxyInfo;
import java.net.MalformedURLException;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;


@Component
public class RestConnectionCreator {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // Be sure to close the rest connection
    public IntHttpClient createNonRedirectingConnection(final URI baseUri, final int timeoutSeconds) throws MalformedURLException {
        logger.debug(String.format("Creating a rest connection (%d second timeout) for URL: %s", timeoutSeconds, baseUri.toString()));
        final IntLogger intLogger = new Slf4jIntLogger(logger);
        IntHttpClient connection = new NonRedirectingIntHttpClient(intLogger, timeoutSeconds, false, ProxyInfo.NO_PROXY_INFO);
        return connection;

    }

    // Be sure to close the rest connection
    public IntHttpClient createRedirectingConnection(final URI baseUri, final int timeoutSeconds) throws MalformedURLException {
        logger.debug(String.format("Creating a rest connection (%d second timeout) for URL: %s", timeoutSeconds, baseUri.toString()));
        final IntLogger intLogger = new Slf4jIntLogger(logger);
        IntHttpClient connection = new IntHttpClient(intLogger, timeoutSeconds, false, ProxyInfo.NO_PROXY_INFO);
        return connection;
    }
}
