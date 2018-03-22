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
package com.blackducksoftware.integration.hub.docker.dockerinspector.wsclient;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.docker.dockerinspector.common.DockerTarfile;
import com.blackducksoftware.integration.hub.docker.dockerinspector.config.Config;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.imageinspector.lib.DissectedImage;

@Component
public class RestClientInspector {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Config config;

    @Autowired
    private DockerTarfile dockerTarfile;

    public int getBdio(final DissectedImage dissectedImage) throws IOException, HubIntegrationException, IntegrationException, MalformedURLException {
        int returnCode;
        // TODO get BDIO via container (later: starting them if necessary)
        final File dockerTarFile = dockerTarfile.deriveDockerTarFile(config);
        final String dockerTarFilePathInContainer = getContainerPathToWorkingDirFile(dockerTarFile.getCanonicalPath(), new File(config.getWorkingDirPath()).getCanonicalPath(), config.getWorkingDirPathImageInspector());
        if (StringUtils.isBlank(config.getImageInspectorUrl())) {
            throw new IntegrationException("The imageinspector URL property must be set");
        }
        new RestClient().getBdio(config.getImageInspectorUrl(), dockerTarFilePathInContainer, config.isCleanupWorkingDir());
        // TODO take what that returns and do something with it
        returnCode = 0;
        return returnCode;
    }

    // TODO move to ProgramPaths or something
    /*
     * Translate a local path to a container path ASSUMING the local working dir is mounted for the container as it's working dir. Find path to the given localPath RELATIVE to the local working dir. Convert that to the container's path by
     * appending that relative path to the container's working dir
     */
    private String getContainerPathToWorkingDirFile(final String localPath, final String workingDirPath, final String workingDirPathImageInspector) {
        logger.debug(String.format("localPath: %s", localPath));
        if (StringUtils.isBlank(workingDirPathImageInspector)) {
            logger.debug(String.format("config.getWorkingDirPathImageInspector() is BLANK"));
            return localPath;
        }
        final String trimmedWorkingDirPath = trimTrailingFileSeparator(workingDirPath);
        final String trimmedWorkingDirPathImageInspector = trimTrailingFileSeparator(workingDirPathImageInspector);
        logger.debug(String.format("config.getWorkingDirPath(): %s", trimmedWorkingDirPath));
        final String localRelPath = localPath.substring(trimmedWorkingDirPath.length());
        logger.debug(String.format("localRelPath: %s", localRelPath));
        final String containerPath = String.format("%s%s", trimmedWorkingDirPathImageInspector, localRelPath);

        return containerPath;
    }

    String trimTrailingFileSeparator(final String path) {
        if (StringUtils.isBlank(path) || !path.endsWith("/")) {
            return path;
        }
        return path.substring(0, path.length() - 1);
    }
}
