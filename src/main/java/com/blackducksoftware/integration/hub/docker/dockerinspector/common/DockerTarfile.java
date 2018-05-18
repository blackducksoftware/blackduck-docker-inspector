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

import com.blackducksoftware.integration.exception.IntegrationException;
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

    // TODO refactor this method
    public File deriveDockerTarFile() throws IOException, HubIntegrationException {
        logger.debug(String.format("*** programPaths.getHubDockerTargetDirPathHost(): %s", programPaths.getHubDockerTargetDirPathHost()));
        File finalDockerTarfile = null;
        if (StringUtils.isNotBlank(config.getDockerTar())) {
            final File givenDockerTarfile = new File(config.getDockerTar());
            logger.debug(String.format("Given docker tarfile: %s", givenDockerTarfile.getCanonicalPath()));

            // TODO make it clearer what's going on (ideally eliminate the conditional with better design)
            if (!config.isOnHost() || StringUtils.isNotBlank(config.getImageInspectorUrl())) {
                return givenDockerTarfile;
            }
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
        // Given image:tag
        final File imageTarDirectory = new File(programPaths.getHubDockerTargetDirPath());
        if (StringUtils.isNotBlank(config.getDockerImageId())) {
            finalDockerTarfile = dockerClientManager.getTarFileFromDockerImageById(config.getDockerImageId(), imageTarDirectory);
        } else if (StringUtils.isNotBlank(config.getDockerImageRepo())) {
            finalDockerTarfile = dockerClientManager.getTarFileFromDockerImage(config.getDockerImageRepo(), config.getDockerImageTag(), imageTarDirectory);
        }
        return finalDockerTarfile;
    }

    public String deriveContainerFileSystemTarGzFilename(final File dockerTarFile) throws IOException, IntegrationException {
        final String dockerTarFilename = dockerTarFile.getName();
        if (!dockerTarFilename.endsWith(".tar")) {
            throw new IntegrationException(String.format("Expected %s to end with '.tar'", dockerTarFilename));
        }
        final String dockerTarFilenameBase = dockerTarFilename.substring(0, dockerTarFilename.length() - ".tar".length());
        logger.info(String.format("dockerTarFilenameBase: %s", dockerTarFilenameBase));
        final String containerFileSystemTarGzFilename = String.format("%s_containerfilesystem.tar.gz", dockerTarFilenameBase);
        logger.info(String.format("containerFileSystemTarGzFilename: %s", containerFileSystemTarGzFilename));
        return containerFileSystemTarGzFilename;
    }

}
