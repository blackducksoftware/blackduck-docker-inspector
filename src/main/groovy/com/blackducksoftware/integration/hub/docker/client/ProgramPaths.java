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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProgramPaths {
    @Value("${on.host}")
    private boolean onHost;

    @Value("${host.working.dir.path:notused}")
    private String hostWorkingDirPath;

    @Value("${hub.codelocation.prefix}")
    private String codeLocationPrefix;

    private String hubDockerPgmDirPath;
    private String hubDockerPgmDirPathContainer;
    public static final String APPLICATION_PROPERTIES_FILENAME = "application.properties";

    private final Logger logger = LoggerFactory.getLogger(ProgramPaths.class);

    private String hubDockerConfigDirPath;
    private String hubDockerConfigDirPathContainer;
    private String hubDockerConfigFilePath;
    private String hubDockerTargetDirPath;
    private String hubDockerTargetDirPathContainer;
    private String hubDockerJarPath;
    private String hubDockerWorkingDirPath;
    private String hubDockerOutputPath;
    private String hubDockerOutputPathContainer;

    private boolean initDone = false;

    private String getProgramDirPath() {
        if (onHost) {
            return getProgramDirPathHost();
        } else {
            return getProgramDirPathContainer();
        }
    }

    private String getProgramDirPathHost() {
        return hostWorkingDirPath;
    }

    private String getProgramDirPathContainer() {
        return "/opt/blackduck/hub-docker-inspector/";
    }

    public void init() {
        if (initDone) {
            return;
        }
        if (StringUtils.isBlank(hubDockerPgmDirPath)) {
            hubDockerPgmDirPath = getProgramDirPath();
        }
        hubDockerPgmDirPathContainer = getProgramDirPathContainer();
        hubDockerConfigDirPath = hubDockerPgmDirPath + "config/";
        hubDockerConfigDirPathContainer = hubDockerPgmDirPathContainer + "config/";
        hubDockerConfigFilePath = hubDockerConfigDirPath + APPLICATION_PROPERTIES_FILENAME;
        hubDockerTargetDirPath = hubDockerPgmDirPath + "target/";
        hubDockerTargetDirPathContainer = hubDockerPgmDirPathContainer + "target/";
        hubDockerWorkingDirPath = hubDockerPgmDirPath + "working/";
        hubDockerOutputPath = hubDockerPgmDirPath + "output/";
        hubDockerOutputPathContainer = getProgramDirPathContainer() + "output/";

        final String qualifiedJarPathString = getQualifiedJarPath();
        logger.debug(String.format("qualifiedJarPathString: %s", qualifiedJarPathString));
        final String prefix = "file:";
        final int startIndex = qualifiedJarPathString.indexOf(prefix) + prefix.length();
        final int endIndex = qualifiedJarPathString.indexOf(".jar") + ".jar".length();
        hubDockerJarPath = qualifiedJarPathString.substring(startIndex, endIndex);
        logger.debug(String.format("hubDockerJarPath: %s", hubDockerJarPath));
        initDone = true;
    }

    public String getQualifiedJarPath() {
        return DockerClientManager.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    }

    public String getHubDockerConfigDirPath() {
        init();
        return hubDockerConfigDirPath;
    }

    public String getHubDockerConfigDirPathContainer() {
        init();
        return hubDockerConfigDirPathContainer;
    }

    public String getHubDockerConfigFilePath() {
        init();
        return hubDockerConfigFilePath;
    }

    public String getHubDockerTargetDirPath() {
        init();
        return hubDockerTargetDirPath;
    }

    public String getHubDockerTargetDirPathContainer() {
        init();
        return hubDockerTargetDirPathContainer;
    }

    public String getHubDockerPgmDirPath() {
        init();
        return hubDockerPgmDirPath;
    }

    public String getHubDockerPgmDirPathContainer() {
        init();
        return hubDockerPgmDirPathContainer;
    }

    public String getHubDockerJarPath() {
        init();
        return hubDockerJarPath;
    }

    public String getHubDockerWorkingDirPath() {
        init();
        return hubDockerWorkingDirPath;
    }

    public String getHubDockerOutputPath() {
        init();
        return hubDockerOutputPath;
    }

    public String getHubDockerOutputPathContainer() {
        init();
        return hubDockerOutputPathContainer;
    }

    public void setHubDockerPgmDirPath(final String hubDockerPgmDirPath) {
        this.hubDockerPgmDirPath = hubDockerPgmDirPath;
    }

    public String getImageTarFilename(final String imageName, final String tagName) {
        return String.format("%s_%s.tar", imageName, tagName);
    }

    public String getContainerFileSystemTarFilename(final String imageName, final String tagName) {
        return String.format("%s_%s_containerfilesystem.tar.gz", slashesToUnderscore(imageName), tagName);
    }

    public String getTargetImageFileSystemRootDirName(final String imageName, final String imageTag) {
        return String.format("image_%s_v_%s", imageName.replaceAll("/", "_"), imageTag);
    }

    public String getCodeLocationName(final String imageName, final String imageTag, final String pkgMgrFilePath, final String pkgMgrName) {
        if (!StringUtils.isBlank(codeLocationPrefix)) {
            return String.format("%s_%s_%s_%s_%s", codeLocationPrefix, slashesToUnderscore(imageName), imageTag, slashesToUnderscore(pkgMgrFilePath), pkgMgrName);
        }
        return String.format("%s_%s_%s_%s", slashesToUnderscore(imageName), imageTag, slashesToUnderscore(pkgMgrFilePath), pkgMgrName);
    }

    public String getBdioFilename(final String imageName, final String pkgMgrFilePath, final String hubProjectName, final String hubVersionName) {
        return String.format("%s_%s_%s_%s_bdio.jsonld", cleanImageName(imageName), cleanPath(pkgMgrFilePath), cleanHubProjectName(hubProjectName), hubVersionName);
    }

    public String getDependencyNodesFilename(final String imageName, final String pkgMgrFilePath, final String hubProjectName, final String hubVersionName) {
        return String.format("%s_%s_%s_%s_dependencies.json", cleanImageName(imageName), cleanPath(pkgMgrFilePath), cleanHubProjectName(hubProjectName), hubVersionName);
    }

    void setCodeLocationPrefix(final String codeLocationPrefix) {
        this.codeLocationPrefix = codeLocationPrefix;
    }

    public String cleanHubProjectName(final String hubProjectName) {
        return slashesToUnderscore(hubProjectName);
    }

    public String cleanImageName(final String imageName) {
        return colonsToUnderscores(slashesToUnderscore(imageName));
    }

    public String cleanPath(final String path) {
        return slashesToUnderscore(path);
    }

    private String slashesToUnderscore(final String imageName) {
        return imageName.replaceAll("/", "_");
    }

    private String colonsToUnderscores(final String imageName) {
        return imageName.replaceAll(":", "_");
    }
}
