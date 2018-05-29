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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.docker.dockerinspector.config.Config;
import com.blackducksoftware.integration.hub.docker.dockerinspector.restclient.response.SimpleResponse;
import com.blackducksoftware.integration.rest.connection.RestConnection;

@Component
public class ImageInspectorClientExistingServices implements ImageInspectorClient {

    @Autowired
    private Config config;

    @Autowired
    private RestRequestor restRequester;

    @Autowired
    private RestConnectionCreator restConnectionCreator;

    @Autowired
    private ContainerPaths containerPaths;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public boolean isApplicable() {
        boolean answer = false;
        if (StringUtils.isNotBlank(config.getImageInspectorUrl())) {
            answer = true;
        }
        logger.debug(String.format("isApplicable() returning %b", answer));
        return answer;
    }

    @Override
    public String getBdio(final String hostPathToTarfile, final String containerPathToInputDockerTarfile, final String containerPathToOutputFileSystemFile, final boolean cleanup) throws IntegrationException, MalformedURLException {
        final String imageInspectorUrl = config.getImageInspectorUrl();
        logger.info(String.format("ImageInspector URL: %s", imageInspectorUrl));
        final int serviceRequestTimeoutSeconds = (int) (config.getCommandTimeout() / 1000L);
        logger.debug(String.format("Creating a rest connection (%d second timeout) for URL: %s", serviceRequestTimeoutSeconds, imageInspectorUrl));
        final RestConnection restConnection = restConnectionCreator.createRedirectingConnection(imageInspectorUrl, serviceRequestTimeoutSeconds);
        logger.debug(String.format("containerPathToFileSystemFile: %s", containerPathToOutputFileSystemFile));
        final SimpleResponse response = restRequester.executeGetBdioRequest(restConnection, imageInspectorUrl, containerPathToInputDockerTarfile, containerPathToOutputFileSystemFile, cleanup);
        return response.getBody();
    }

    @Override
    public ContainerPaths getContainerPaths() {
        return containerPaths;
    }
}
