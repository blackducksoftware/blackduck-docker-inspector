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
package com.blackducksoftware.integration.hub.docker.dockerinspector.common;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.docker.dockerinspector.config.Config;
import com.blackducksoftware.integration.hub.docker.dockerinspector.config.ProgramPaths;
import com.blackducksoftware.integration.hub.docker.dockerinspector.dockerclient.DockerClientManager;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;

@Component
public class DockerTarfile {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Config config;

    @Autowired
    private DockerClientManager dockerClientManager;

    @Autowired
    private ProgramPaths programPaths;

    public File deriveDockerTarFile() throws IOException, HubIntegrationException {
        logger.debug(String.format("programPaths.getHubDockerTargetDirPath(): %s", programPaths.getHubDockerTargetDirPath()));
        if (StringUtils.isNotBlank(config.getDockerTar())) {
            return deriveDockerTarFileGivenTarfile();
        } else {
            return deriveDockerTarFileGivenImageSpec();
        }

    }

    private File deriveDockerTarFileGivenTarfile() throws IOException {
        File finalDockerTarfile = null;
        final File givenDockerTarfile = new File(config.getDockerTar());
        logger.debug(String.format("Given docker tarfile: %s", givenDockerTarfile.getCanonicalPath()));
        // When in container: no copy needed
        if (!config.isOnHost()) {
            return givenDockerTarfile;
        }
        // TODO: actually, don't need this. The not-on-host check takes care of it
        // In orchestration environments: user gives us the final tarfile location in the shared dir; no copy needed
        // if (StringUtils.isNotBlank(config.getImageInspectorUrl())) {
        // return givenDockerTarfile;
        // }
        if (config.isImageInspectorServiceStart()) {
            // TODO: move this code into a start-as-needed-specific class?
            // Copy the tarfile to the target dir
            finalDockerTarfile = new File(programPaths.getHubDockerTargetDirPath(), givenDockerTarfile.getName());
            logger.debug(String.format("Required docker tarfile location: %s", finalDockerTarfile.getCanonicalPath()));
            if (!finalDockerTarfile.getCanonicalPath().equals(givenDockerTarfile.getCanonicalPath())) {
                logger.debug(String.format("Copying %s to %s", givenDockerTarfile.getCanonicalPath(), finalDockerTarfile.getCanonicalPath()));
                // This copy isn't strictly necessary in the "exec" scenario; only the start-containers-as-needed scenario
                // But it doesn't seem worth complicating the code to avoid it, especially hopefully exec mode can eventually be removed
                FileUtils.copyFile(givenDockerTarfile, finalDockerTarfile);
            }
            return finalDockerTarfile;
        }
        return givenDockerTarfile;
    }

    private File deriveDockerTarFileGivenImageSpec() throws HubIntegrationException, IOException {
        File finalDockerTarfile = null;
        final File imageTarDirectory = new File(programPaths.getHubDockerTargetDirPath());
        if (StringUtils.isNotBlank(config.getDockerImageId())) {
            finalDockerTarfile = dockerClientManager.getTarFileFromDockerImageById(config.getDockerImageId(), imageTarDirectory);
        } else if (StringUtils.isNotBlank(config.getDockerImageRepo())) {
            finalDockerTarfile = dockerClientManager.getTarFileFromDockerImage(config.getDockerImageRepo(), config.getDockerImageTag(), imageTarDirectory);
        }
        return finalDockerTarfile;
    }
}
