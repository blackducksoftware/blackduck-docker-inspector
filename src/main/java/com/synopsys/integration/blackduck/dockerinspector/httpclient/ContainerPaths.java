/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2019 Synopsys, Inc.
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

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.ProgramPaths;

@Component
public class ContainerPaths {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Config config;

    @Autowired
    private ProgramPaths programPaths;

    /*
     * Translate a local path (to a file within the dir shared with the container) to the equivalent path for the container. Find path to the given localPath RELATIVE to the local shared dir. Convert that to the container's path by
     * appending that relative path to the container's path to the shared dir
     */
    public String getContainerPathToTargetFile(final String localPathToTargetFile) throws IOException {

        logger.debug(String.format("localPathToTargetFile: %s", localPathToTargetFile));
        final String sharedDirPathLocal = new File(config.getSharedDirPathLocal()).getCanonicalPath();
        final String sharedDirPathImageInspector = config.getSharedDirPathImageInspector();
        logger.debug(String.format("config.getSharedDirPathLocal(): %s", sharedDirPathLocal));
        final String localRelPath = localPathToTargetFile.substring(sharedDirPathLocal.length());
        logger.debug(String.format("localRelPath: %s", localRelPath));
        final File containerFile = getFileInDir(sharedDirPathImageInspector, localRelPath);
        logger.debug(String.format("containerPath: %s", containerFile.getAbsolutePath()));
        return containerFile.getAbsolutePath();
    }

    public String getContainerPathToOutputFile(final String outputFilename) {
        final File containerFileSystemFileInContainer = new File(getContainerOutputDir(), outputFilename);
        logger.debug(String.format("Derived container docker tar file path: %s", containerFileSystemFileInContainer.getAbsolutePath()));
        return containerFileSystemFileInContainer.getAbsolutePath();
    }

    private File getContainerOutputDir() {
        final File containerRunDir = getFileInDir(config.getSharedDirPathImageInspector(), programPaths.getDockerInspectorRunDirName());
        final File containerOutputDir = new File(containerRunDir, ProgramPaths.OUTPUT_DIR);
        return containerOutputDir;
    }

    private File getFileInDir(final String dirPath, final String filename) {
        final File containerOutputDir = new File(dirPath);
        final File containerFileSystemFileInContainer = new File(containerOutputDir, filename);
        return containerFileSystemFileInContainer;
    }
}
