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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;

@Component
public class ContainerFilesystemFilename {
    private static final String CONTAINER_FILESYSTEM_IDENTIFIER = "containerfilesystem";
    private static final String APP_ONLY_HINT = "app";

    private Config config;

    @Autowired
    public void setConfig(final Config config) {
        this.config = config;
    }

    public String deriveContainerFilesystemFilename(final String repo, final String tag) {
        final String containerFileSystemFilename;
        if (StringUtils.isBlank(config.getDockerPlatformTopLayerId())) {
            containerFileSystemFilename = getContainerFileSystemTarFilename(repo, tag, config.getDockerTar());
        } else {
            containerFileSystemFilename = getContainerFileSystemAppLayersTarFilename(repo, tag, config.getDockerTar());
        }
        return containerFileSystemFilename;
    }

    private String getContainerFileSystemTarFilename(final String repo, final String tag, final String tarPath) {
        return getContainerOutputTarFileNameUsingBase(CONTAINER_FILESYSTEM_IDENTIFIER, repo, tag, tarPath);
    }

    private String getContainerFileSystemAppLayersTarFilename(final String repo, final String tag, final String tarPath) {
        final String contentHint = String.format("%s_%s", APP_ONLY_HINT, CONTAINER_FILESYSTEM_IDENTIFIER);
        return getContainerOutputTarFileNameUsingBase(contentHint, repo, tag, tarPath);
    }

    private static String getContainerOutputTarFileNameUsingBase(final String contentHint, final String repo, final String tag, final String tarPath) {
        final String containerFilesystemFilenameSuffix = String.format("%s.tar.gz", contentHint);
        if (StringUtils.isNotBlank(repo)) {
            return String.format("%s_%s_%s", slashesToUnderscore(repo), slashesToUnderscore(tag), containerFilesystemFilenameSuffix);
        } else {
            final File tarFile = new File(tarPath);
            final String tarFilename = tarFile.getName();
            if (tarFilename.contains(".")) {
                final int finalPeriodIndex = tarFilename.lastIndexOf('.');
                return String.format("%s_%s", tarFilename.substring(0, finalPeriodIndex), containerFilesystemFilenameSuffix);
            }
            return String.format("%s_%s", cleanImageName(tarFilename), containerFilesystemFilenameSuffix);
        }
    }

    private static String cleanImageName(final String imageName) {
        return colonsToUnderscores(slashesToUnderscore(imageName));
    }

    private static String colonsToUnderscores(final String imageName) {
        return imageName.replaceAll(":", "_");
    }

    private static String slashesToUnderscore(final String givenString) {
        return givenString.replaceAll("/", "_");
    }
}
