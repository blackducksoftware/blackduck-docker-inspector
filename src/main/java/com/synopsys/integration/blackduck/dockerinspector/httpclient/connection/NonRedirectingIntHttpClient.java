/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.httpclient.connection;

import com.google.gson.Gson;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.rest.client.IntHttpClient;
import com.synopsys.integration.rest.proxy.ProxyInfo;

public class NonRedirectingIntHttpClient extends
    IntHttpClient {

    public NonRedirectingIntHttpClient(IntLogger logger, Gson gson, int timeout, boolean trustCert, ProxyInfo proxyInfo) {
        super(logger, gson, timeout, trustCert, proxyInfo);
        logger.debug("Disabling redirect handling on this HTTP client");
        getClientBuilder().disableRedirectHandling();
    }
}
