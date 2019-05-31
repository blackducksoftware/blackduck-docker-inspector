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
package com.synopsys.integration.blackduck.dockerinspector.output;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.ProgramPaths;
import com.synopsys.integration.blackduck.dockerinspector.dockerclient.DockerClientManager;
import com.synopsys.integration.blackduck.exception.BlackDuckIntegrationException;
import com.synopsys.integration.exception.IntegrationException;

@Component
public class ImageTarFilename {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private Config config;
    private DockerClientManager dockerClientManager;
    private ProgramPaths programPaths;

    @Autowired
    public void setConfig(final Config config) {
        this.config = config;
    }

    @Autowired
    public void setDockerClientManager(final DockerClientManager dockerClientManager) {
        this.dockerClientManager = dockerClientManager;
    }

    @Autowired
    public void setProgramPaths(final ProgramPaths programPaths) {
        this.programPaths = programPaths;
    }

    public String deriveImageTarFilenameFromImageTag(final String imageName, final String tagName) {
        return String.format("%s_%s.tar", cleanImageName(imageName), tagName);
    }

    public File deriveDockerTarFileFromConfig() throws IOException, IntegrationException {
        logger.debug(String.format("programPaths.getDockerInspectorTargetDirPath(): %s", programPaths.getDockerInspectorTargetDirPath()));
        if (StringUtils.isNotBlank(config.getDockerTar())) {
            return new File(config.getDockerTar());
        } else {
            return deriveDockerTarFileGivenImageSpec();
        }
    }

    private String cleanImageName(final String imageName) {
        return colonsToUnderscores(slashesToUnderscore(imageName));
    }

    private String colonsToUnderscores(final String imageName) {
        return imageName.replaceAll(":", "_");
    }

    private String slashesToUnderscore(final String givenString) {
        return givenString.replaceAll("/", "_");
    }

    private File deriveDockerTarFileGivenImageSpec() throws IntegrationException, IOException {
        File finalDockerTarfile;
        final File imageTarDirectory = new File(programPaths.getDockerInspectorTargetDirPath());
        if (StringUtils.isNotBlank(config.getDockerImageId())) {
            finalDockerTarfile = dockerClientManager.getTarFileFromDockerImageById(config.getDockerImageId(), imageTarDirectory);
        } else if (StringUtils.isNotBlank(config.getDockerImageRepo())) {
            finalDockerTarfile = dockerClientManager.getTarFileFromDockerImage(config.getDockerImageRepo(), config.getDockerImageTag(), imageTarDirectory);
        } else {
            throw new BlackDuckIntegrationException("You must specify a docker image");
        }
        return finalDockerTarfile;
    }
}
