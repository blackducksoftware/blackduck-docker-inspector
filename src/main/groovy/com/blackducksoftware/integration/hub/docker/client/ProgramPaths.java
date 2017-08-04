/**
 * Hub Docker Inspector
 *
 * Copyright (C) 2017 Black Duck Software, Inc.
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
package com.blackducksoftware.integration.hub.docker.client;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProgramPaths {

    public String hubDockerPgmDirPath; // TODO make private

    private static final String DEFAULT_PGM_DIR = "/opt/blackduck/hub-docker-inspector";

    public static final String APPLICATION_PROPERTIES_FILENAME = "application.properties";

    private final Logger logger = LoggerFactory.getLogger(ProgramPaths.class);

    private String hubDockerConfigDirPath;
    private String hubDockerConfigFilePath;
    private String hubDockerTargetDirPath;
    private String hubDockerJarPath;
    private String hubDockerWorkingDirPath;
    private String hubDockerOutputJsonPath;

    private boolean initDone = false;

    void init() {
        if (initDone) {
            return;
        }
        if (StringUtils.isBlank(hubDockerPgmDirPath)) {
            hubDockerPgmDirPath = DEFAULT_PGM_DIR;
        }
        if (!hubDockerPgmDirPath.endsWith("/")) {
            hubDockerPgmDirPath += "/";
        }
        hubDockerConfigDirPath = hubDockerPgmDirPath + "config/";
        hubDockerConfigFilePath = hubDockerConfigDirPath + APPLICATION_PROPERTIES_FILENAME;
        hubDockerTargetDirPath = hubDockerPgmDirPath + "target/";
        hubDockerWorkingDirPath = hubDockerPgmDirPath + "working/";
        hubDockerOutputJsonPath = hubDockerPgmDirPath + "output/";

        final String qualifiedJarPathString = getQualifiedJarPath();
        logger.debug(String.format("qualifiedJarPathString: %s", qualifiedJarPathString));
        final String prefix = String.format("%shub-docker-", hubDockerPgmDirPath);
        logger.debug(String.format("prefix: %s", prefix));
        final int startIndex = qualifiedJarPathString.indexOf(prefix);
        final int endIndex = qualifiedJarPathString.indexOf(".jar") + ".jar".length();
        hubDockerJarPath = qualifiedJarPathString.substring(startIndex, endIndex);

        initDone = true;
    }

    public String getQualifiedJarPath() {
        return new java.io.File(DockerClientManager.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getAbsolutePath();
    }

    public String getHubDockerConfigDirPath() {
        init();
        return hubDockerConfigDirPath;
    }

    public String getHubDockerConfigFilePath() {
        init();
        return hubDockerConfigFilePath;
    }

    public String getHubDockerTargetDirPath() {
        init();
        return hubDockerTargetDirPath;
    }

    public String getHubDockerPgmDirPath() {
        init();
        return hubDockerPgmDirPath;
    }

    public String getHubDockerJarPath() {
        init();
        return hubDockerJarPath;
    }

    public String getHubDockerWorkingDirPath() {
        init();
        return hubDockerWorkingDirPath;
    }

    public String getHubDockerOutputJsonPath() {
        init();
        return hubDockerOutputJsonPath;
    }

    public String getImageTarFilename(final String imageName, final String tagName) {
        // TODO this used to replace : with _ in image name; why??
        return String.format("%s_%s.tar", imageName, tagName);
    }

    public String getTargetImageFileSystemRootDirName(final String imageName, final String imageTag) {
        return String.format("image_%s_v_%s", imageName.replaceAll("/", "_"), imageTag);
    }

    public String getCodeLocationName(final String imageName, final String imageTag, final String pkgMgrFilePath, final String pkgMgrName) {
        return String.format("%s_%s_%s", cleanSlashedString(imageName), imageTag, cleanSlashedString(pkgMgrFilePath), pkgMgrName);
    }

    public String getBdioFilename(final String imageName, final String pkgMgrFilePath, final String hubProjectName, final String hubVersionName) {
        return String.format("%s_%s_%s_%s_bdio.jsonld", cleanImageName(imageName), cleanPath(pkgMgrFilePath), cleanHubProjectName(hubProjectName), hubVersionName);
    }

    public String cleanHubProjectName(final String hubProjectName) {
        return cleanSlashedString(hubProjectName);
    }

    public String cleanImageName(final String imageName) {
        return cleanSlashedString(imageName);
    }

    public String cleanPath(final String path) {
        return cleanSlashedString(path);
    }

    private String cleanSlashedString(final String imageName) {
        return imageName.replaceAll("/", "_");
    }
}
