/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
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
public class HttpConnectionCreator {
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
