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
            return new File(config.getDockerTar());
        } else {
            return deriveDockerTarFileGivenImageSpec();
        }
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
