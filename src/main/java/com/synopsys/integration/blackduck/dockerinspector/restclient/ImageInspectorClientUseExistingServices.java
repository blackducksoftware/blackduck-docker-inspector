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
package com.synopsys.integration.blackduck.dockerinspector.restclient;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.restclient.response.SimpleResponse;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.client.IntHttpClient;

@Component
public class ImageInspectorClientUseExistingServices implements ImageInspectorClient {

    @Autowired
    private Config config;

    @Autowired
    private RestRequestor restRequester;

    @Autowired
    private RestConnectionCreator restConnectionCreator;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public boolean isApplicable() {
        final boolean answer = !config.isImageInspectorServiceStart() && StringUtils.isNotBlank(config.getImageInspectorUrl());
        logger.debug(String.format("isApplicable() returning %b", answer));
        return answer;
    }

    @Override
    public File copyTarfileToSharedDir(final File givenDockerTarfile) throws IOException {
        return givenDockerTarfile;
    }

    @Override
    public String getBdio(final String hostPathToTarfile, final String containerPathToInputDockerTarfile, final String givenImageRepo, final String givenImageTag, final String containerPathToOutputFileSystemFile,
        final boolean organizeComponentsByLayer, final boolean includeRemovedComponents,
        final boolean cleanup)
            throws IntegrationException, MalformedURLException {
        URI imageInspectorUri;
        try {
            imageInspectorUri = new URI(config.getImageInspectorUrl());
        } catch (final URISyntaxException e) {
            throw new IntegrationException(String.format("Error constructing URI from %s: %s", config.getImageInspectorUrl(), e.getMessage()), e);
        }
        final int serviceRequestTimeoutSeconds = (int) (config.getCommandTimeout() / 1000L);
        final IntHttpClient restConnection = restConnectionCreator.createRedirectingConnection(imageInspectorUri, serviceRequestTimeoutSeconds);
        final SimpleResponse response = restRequester.executeGetBdioRequest(restConnection, imageInspectorUri, containerPathToInputDockerTarfile,
                givenImageRepo, givenImageTag, containerPathToOutputFileSystemFile, organizeComponentsByLayer, includeRemovedComponents, cleanup);
        return response.getBody();
    }
}
