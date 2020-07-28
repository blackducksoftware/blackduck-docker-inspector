/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2020 Synopsys, Inc.
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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.httpclient.response.SimpleResponse;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.client.IntHttpClient;

@Component
public class ImageInspectorClientUseExistingServices extends ImageInspectorClient {

    @Autowired
    private Config config;

    @Autowired
    private HttpRequestor restRequester;

    @Autowired
    private HttpConnectionCreator httpConnectionCreator;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public boolean isApplicable() {
        boolean answer = !config.isImageInspectorServiceStart() && StringUtils.isNotBlank(config.getImageInspectorUrl());
        logger.debug(String.format("isApplicable() returning %b", answer));
        return answer;
    }

    @Override
    public String getBdio(String hostPathToTarfile, String containerPathToInputDockerTarfile, String givenImageRepo, String givenImageTag,
        String containerPathToOutputFileSystemFile, String containerFileSystemExcludedPaths,
        boolean organizeComponentsByLayer, boolean includeRemovedComponents,
        boolean cleanup, String platformTopLayerId,
        String targetLinuxDistro)
        throws IntegrationException, MalformedURLException {
        URI imageInspectorUri;
        try {
            imageInspectorUri = new URI(config.getImageInspectorUrl());
        } catch (URISyntaxException e) {
            throw new IntegrationException(String.format("Error constructing URI from %s: %s", config.getImageInspectorUrl(), e.getMessage()), e);
        }
        int serviceRequestTimeoutSeconds = deriveTimeoutSeconds();
        IntHttpClient restConnection = httpConnectionCreator
                                           .createRedirectingConnection(imageInspectorUri, serviceRequestTimeoutSeconds);
        SimpleResponse response = restRequester.executeGetBdioRequest(restConnection, imageInspectorUri, containerPathToInputDockerTarfile,
            givenImageRepo, givenImageTag,
            containerPathToOutputFileSystemFile, containerFileSystemExcludedPaths,
            organizeComponentsByLayer, includeRemovedComponents, cleanup,
            platformTopLayerId,
            targetLinuxDistro);
        return response.getBody();
    }

    private int deriveTimeoutSeconds() {
        return (int) (config.getServiceTimeout() / 1000L);
    }
}
