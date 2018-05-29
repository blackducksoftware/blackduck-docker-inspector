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
import com.blackducksoftware.integration.hub.docker.dockerinspector.config.ProgramPaths;

@Component
public class ContainerPaths {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Config config;

    @Autowired
    private ProgramPaths programPaths;

    // TODO Rethink distribution of responsibility across Config, ContainerPath and ProgramPaths
    // TODO Should this be in ProgramPaths? Or at least delegated to from there?

    /*
     * Translate a local path (to a file within the dir shared with the container) to the equivalent path for the container. Find path to the given localPath RELATIVE to the local shared dir. Convert that to the container's path by
     * appending that relative path to the container's path to the shared dir
     */
    public String getContainerPathToTargetFile(final String localPathToTargetFile) throws IOException {

        logger.debug(String.format("localPathToTargetFile: %s", localPathToTargetFile));
        final String sharedDirPathLocal = new File(config.getSharedDirPathLocal()).getCanonicalPath();
        final String sharedDirPathImageInspector = config.getSharedDirPathImageInspector();
        if (StringUtils.isBlank(sharedDirPathImageInspector)) {
            // TODO is this even a real scenario??
            logger.debug(String.format("config.getSharedDirPathImageInspector() is BLANK"));
            final String containerTargetDirPathDefault = getContainerPathToTargetDir();
            if (StringUtils.isNotBlank(containerTargetDirPathDefault)) {
                return getContainerTargetFilePath(containerTargetDirPathDefault, localPathToTargetFile);
            } else {
                // TODO this should throw an exception... localPath will never work!
                return localPathToTargetFile;
            }
        }
        logger.debug(String.format("*** config.getSharedDirPathLocal(): %s", sharedDirPathLocal));
        final String localRelPath = localPathToTargetFile.substring(sharedDirPathLocal.length());
        logger.debug(String.format("localRelPath: %s", localRelPath));
        final File containerFile = getFileInDir(sharedDirPathImageInspector, localRelPath);
        logger.debug(String.format("containerPath: %s", containerFile.getAbsolutePath()));
        return containerFile.getAbsolutePath();
    }

    public String getContainerPathToOutputFile(final String outputFilename) {
        final File containerFileSystemFileInContainer = getFileInDir(getContainerPathToOutputDir(), outputFilename);
        return containerFileSystemFileInContainer.getAbsolutePath();
    }

    public String getContainerPathToSharedDir() {
        logger.debug(String.format("getContainerPathToSharedDir() returning %s", config.getSharedDirPathImageInspector()));
        return config.getSharedDirPathImageInspector();
    }

    public String getContainerPathToOutputDir() {
        // TODO refactor some of this out; should be sharable
        final File containerSharedDir = new File(getContainerPathToSharedDir());
        final File containerRunDir = new File(containerSharedDir, programPaths.getHubDockerRunDirName());
        final File containerOutputDir = new File(containerRunDir, ProgramPaths.OUTPUT_DIR);
        logger.debug(String.format("*** getContainerPathToOutputDir() returning %s", containerOutputDir.getAbsolutePath()));
        return containerOutputDir.getAbsolutePath();
    }

    public String getContainerPathToTargetDir() {
        final File targetDir = getFileInDir(getContainerPathToSharedDir(), ProgramPaths.TARGET_DIR);
        logger.debug(String.format("getContainerPathToTargetDir() returning %s", targetDir.getAbsolutePath()));
        return targetDir.getAbsolutePath();
    }

    private File getFileInDir(final String dirPath, final String filename) {
        final File containerOutputDir = new File(dirPath);
        final File containerFileSystemFileInContainer = new File(containerOutputDir, filename);
        return containerFileSystemFileInContainer;
    }

    private String getContainerTargetFilePath(final String containerTargetDirPathDefault, final String localPath) {
        final File localFile = new File(localPath);
        final File containerTargetFile = getFileInDir(containerTargetDirPathDefault, localFile.getName());
        final String containerTargetFilePath = containerTargetFile.getAbsolutePath();
        return containerTargetFilePath;
    }
}
