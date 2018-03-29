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

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.docker.dockerinspector.config.Config;

@Component
public class ContainerPath {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Config config;

    /*
     * Translate a local path (to a file within the dir shared with the container) to the equivalent path for the container. Find path to the given localPath RELATIVE to the local shared dir. Convert that to the container's path by
     * appending that relative path to the container's path to the shared dir
     */
    public String getContainerPathToLocalFile(final String localPath) throws IOException {
        logger.debug(String.format("localPath: %s", localPath));
        final String workingDirPath = new File(config.getSharedDirPathLocal()).getCanonicalPath();
        final String workingDirPathImageInspector = config.getSharedDirPathImageInspector();
        if (StringUtils.isBlank(workingDirPathImageInspector)) {
            logger.debug(String.format("config.getWorkingDirPathImageInspector() is BLANK"));
            return localPath;
        }
        final String trimmedWorkingDirPath = trimTrailingFileSeparator(workingDirPath);
        final String trimmedWorkingDirPathImageInspector = trimTrailingFileSeparator(workingDirPathImageInspector);
        logger.debug(String.format("config.getWorkingDirPath(): %s", trimmedWorkingDirPath));
        String localRelPath = localPath.substring(trimmedWorkingDirPath.length());
        if (!localRelPath.startsWith("/")) {
            localRelPath = String.format("/%s", localRelPath);
        }
        logger.debug(String.format("localRelPath (must start with /): %s", localRelPath));
        final String containerPath = String.format("%s%s", trimmedWorkingDirPathImageInspector, localRelPath);
        logger.debug(String.format("containerPath: %s", containerPath));
        return containerPath;
    }

    private String trimTrailingFileSeparator(final String path) {
        if (StringUtils.isBlank(path) || !path.endsWith("/")) {
            return path;
        }
        return path.substring(0, path.length() - 1);
    }
}
