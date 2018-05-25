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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.docker.dockerinspector.config.Config;

//////////////////////////////
// TODO obsolete???
//////////////////////////////
@Component
public class ContainerPathsTargetDirCopiedFromHost implements ContainerPaths {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Config config;

    @Override
    public String getContainerPathToLocalFile(final String localPath) throws IOException {
        logger.debug(String.format("local file path: %s", localPath));
        final File localFile = new File(localPath);
        final File containerTargetDir = new File(getContainerPathToTargetDir());
        final File containerTargetFile = new File(containerTargetDir, localFile.getName());
        logger.debug(String.format("container file path: %s", containerTargetFile.getAbsolutePath()));
        return containerTargetFile.getAbsolutePath();
    }

    // TODO this could be shared across both implementations; methods are identical (I think)
    @Override
    public String getContainerPathToOutputFile(final String outputFilename) {
        final File containerOutputDir = new File(getContainerPathToOutputDir());
        logger.debug(String.format("containerOutputDir: %s", containerOutputDir.getAbsolutePath()));
        final File containerFileSystemFileInContainer = new File(containerOutputDir, outputFilename);
        logger.debug(String.format("containerFileSystemFileInContainer: %s", containerFileSystemFileInContainer.getAbsolutePath()));
        return containerFileSystemFileInContainer.getAbsolutePath();
    }

    @Override
    public String getContainerPathToSharedDir() {
        logger.debug(String.format("getContainerPathToSharedDir() returning %s", config.getSharedDirPathImageInspector()));
        return config.getSharedDirPathImageInspector();
    }

    @Override
    public String getContainerPathToOutputDir() {
        // TODO is, or should, this (be) done in ProgramPaths?
        final File sharedDir = new File(getContainerPathToSharedDir());
        final File outputDir = new File(sharedDir, "output");
        logger.debug(String.format("getContainerPathToOutputDir() returning %s", outputDir.getAbsolutePath()));
        return outputDir.getAbsolutePath();
    }

    @Override
    public String getContainerPathToTargetDir() {
        // TODO is, or should, this (be) done in ProgramPaths?
        final File sharedDir = new File(getContainerPathToSharedDir());
        final File targetDir = new File(sharedDir, "target");
        logger.debug(String.format("getContainerPathToTargetDir() returning %s", targetDir.getAbsolutePath()));
        return targetDir.getAbsolutePath();
    }
}
