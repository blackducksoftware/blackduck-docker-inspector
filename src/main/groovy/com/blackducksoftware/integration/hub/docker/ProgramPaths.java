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
package com.blackducksoftware.integration.hub.docker;

import java.io.File;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.docker.client.DockerClientManager;

@Component
public class ProgramPaths {
    private static final String CONTAINER_JAR_PATH = "/opt/blackduck/hub-docker-inspector/hub-docker-inspector.jar";

    private static final String JAR_FILENAME = "hub-docker-inspector.jar";

    private static final String JAR_FILE_SUFFIX = ".jar";

    private static final String FILE_URI_PREFIX = "file:";

    private static final String RESULT_JSON_FILENAME = "result.json";

    private static final String OUTPUT_DIR = "output/";

    private static final String WORKING_DIR = "working/";

    private static final String TARGET_DIR = "target/";

    private static final String TEMP_DIR = "temp/";

    private static final String CONFIG_DIR = "config/";

    private static final String CONTAINER_PROGRAM_DIR = "/opt/blackduck/hub-docker-inspector/";

    @Value("${on.host}")
    private boolean onHost;

    @Value("${working.dir.path:/tmp/hub-docker-inspector}")
    private String hostWorkingDirPath;

    @Value("${DOCKER_INSPECTOR_WORKING_DIR:}")
    private String hostWorkingDirPathEnvVarValue;

    @Value("${hub.codelocation.prefix}")
    private String codeLocationPrefix;

    @Value("${jar.path}")
    private String givenJarPath;

    @Value("${output.path:}")
    private String userOutputDir;

    private String hubDockerPgmDirPath;
    private String hubDockerPgmDirPathContainer;
    public static final String APPLICATION_PROPERTIES_FILENAME = "application.properties";

    private final Logger logger = LoggerFactory.getLogger(ProgramPaths.class);

    private String hubDockerConfigDirPath;
    private String hubDockerTempDirPath;
    private String hubDockerConfigDirPathContainer;
    private String hubDockerConfigFilePath;
    private String hubDockerTargetDirPath;
    private String hubDockerTargetDirPathContainer;
    private String hubDockerJarPathActual;
    private String hubDockerJarPathHost;
    private String hubDockerWorkingDirPath;
    private String hubDockerOutputPath;
    private String hubDockerOutputPathContainer;
    private String hubDockerResultPath;

    private String getProgramDirPath() {
        if (onHost) {
            return getProgramDirPathHost();
        } else {
            return getProgramDirPathContainer();
        }
    }

    private String getProgramDirPathHost() {
        if (!StringUtils.isBlank(hostWorkingDirPathEnvVarValue)) {
            logger.debug("Working dir: Using env variable");
            hostWorkingDirPath = hostWorkingDirPathEnvVarValue;
        }
        if (!hostWorkingDirPath.endsWith("/")) {
            hostWorkingDirPath = String.format("%s/", hostWorkingDirPath);
        }
        return hostWorkingDirPath;
    }

    private String getProgramDirPathContainer() {
        return CONTAINER_PROGRAM_DIR;
    }

    @PostConstruct
    public void init() {
        logger.debug(String.format("givenJarPath: %s", givenJarPath));
        if (StringUtils.isBlank(hubDockerPgmDirPath)) {
            hubDockerPgmDirPath = getProgramDirPath();
        }
        logger.debug(String.format("hubDockerPgmDirPath: %s", hubDockerPgmDirPath));
        if (StringUtils.isBlank(hubDockerJarPathHost)) {
            hubDockerJarPathHost = givenJarPath.replaceAll("%20", " ");
        }
        hubDockerPgmDirPathContainer = getProgramDirPathContainer();
        hubDockerConfigDirPath = hubDockerPgmDirPath + CONFIG_DIR;
        hubDockerTempDirPath = hubDockerPgmDirPath + TEMP_DIR;
        hubDockerConfigDirPathContainer = hubDockerPgmDirPathContainer + CONFIG_DIR;
        hubDockerConfigFilePath = hubDockerConfigDirPath + APPLICATION_PROPERTIES_FILENAME;
        hubDockerTargetDirPath = hubDockerPgmDirPath + TARGET_DIR;
        hubDockerTargetDirPathContainer = hubDockerPgmDirPathContainer + TARGET_DIR;
        hubDockerWorkingDirPath = hubDockerPgmDirPath + WORKING_DIR;
        hubDockerOutputPath = hubDockerPgmDirPath + OUTPUT_DIR;
        hubDockerOutputPathContainer = getProgramDirPathContainer() + OUTPUT_DIR;
        hubDockerResultPath = hubDockerOutputPath + RESULT_JSON_FILENAME;
        hubDockerJarPathActual = deriveJarPath();
    }

    public String getUserOutputDir() {
        if (StringUtils.isBlank(userOutputDir)) {
            return null;
        }
        return userOutputDir;
    }

    private String deriveJarPath() {
        final String qualifiedJarPathString = getQualifiedJarPath();
        logger.debug(String.format("qualifiedJarPathString: %s", qualifiedJarPathString));
        final String prefix = FILE_URI_PREFIX;
        final int startIndex = qualifiedJarPathString.indexOf(prefix) + prefix.length();
        final int endIndex = qualifiedJarPathString.indexOf(JAR_FILE_SUFFIX) + JAR_FILE_SUFFIX.length();
        final String hubDockerJarPathActual = qualifiedJarPathString.substring(startIndex, endIndex);
        logger.debug(String.format("hubDockerJarPathActual: %s", hubDockerJarPathActual));
        return hubDockerJarPathActual;
    }

    // TODO not sure this class should be copying files
    public String copyJarToWorkingDir(final String hostJarPath) throws IOException {
        final File fromFile = new File(hostJarPath);
        final File toFile = new File(getHubDockerTempDirPath() + JAR_FILENAME);
        logger.debug(String.format("copyJarToWorkingDir(): Copying %s to %s", fromFile.getAbsolutePath(), toFile.getAbsolutePath()));
        FileUtils.copyFile(fromFile, toFile);
        return toFile.getAbsolutePath();
    }

    public String getQualifiedJarPath() {
        return DockerClientManager.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    }

    public String getHubDockerConfigDirPath() {
        return hubDockerConfigDirPath;
    }

    public String getHubDockerTempDirPath() {
        return hubDockerTempDirPath;
    }

    public String getHubDockerConfigDirPathContainer() {
        return hubDockerConfigDirPathContainer;
    }

    public String getHubDockerConfigFilePath() {
        return hubDockerConfigFilePath;
    }

    public String getHubDockerTargetDirPath() {
        return hubDockerTargetDirPath;
    }

    public String getHubDockerTargetDirPathContainer() {
        return hubDockerTargetDirPathContainer;
    }

    public String getHubDockerPgmDirPath() {
        return hubDockerPgmDirPath;
    }

    public String getHubDockerPgmDirPathContainer() {
        return hubDockerPgmDirPathContainer;
    }

    public String getHubDockerJarPathHost() {
        return hubDockerJarPathHost;
    }

    public String getHubDockerJarPathContainer() {
        return CONTAINER_JAR_PATH;
    }

    public String getHubDockerJarPathActual() {
        return hubDockerJarPathActual;
    }

    public String getHubDockerWorkingDirPath() {
        return hubDockerWorkingDirPath;
    }

    public String getHubDockerOutputPath() {
        return hubDockerOutputPath;
    }

    public String getHubDockerResultPath() {
        return hubDockerResultPath;
    }

    public String getHubDockerOutputPathContainer() {
        return hubDockerOutputPathContainer;
    }

    public void setHubDockerPgmDirPath(final String hubDockerPgmDirPath) {
        this.hubDockerPgmDirPath = hubDockerPgmDirPath;
    }

    void setGivenJarPath(final String givenJarPath) {
        this.givenJarPath = givenJarPath;
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
        return createBdioFilename(cleanImageName(imageName), cleanPath(pkgMgrFilePath), cleanHubProjectName(hubProjectName), hubVersionName);
    }

    private String createBdioFilename(final String cleanImageName, final String cleanPkgMgrFilePath, final String cleanHubProjectName, final String hubVersionName) {
        final String[] parts = new String[4];
        parts[0] = cleanImageName;
        parts[1] = cleanPkgMgrFilePath;
        parts[2] = cleanHubProjectName;
        parts[3] = hubVersionName;

        String filename = generateFilename(cleanImageName, cleanPkgMgrFilePath, cleanHubProjectName, hubVersionName);
        for (int i = 0; (filename.length() >= 255) && (i < 4); i++) {
            parts[i] = DigestUtils.sha1Hex(parts[i]);
            if (parts[i].length() > 15) {
                parts[i] = parts[i].substring(0, 15);
            }

            filename = generateFilename(parts[0], parts[1], parts[2], parts[3]);
        }
        return filename;
    }

    private String generateFilename(final String cleanImageName, final String cleanPkgMgrFilePath, final String cleanHubProjectName, final String hubVersionName) {
        return String.format("%s_%s_%s_%s_bdio.jsonld", cleanImageName, cleanPkgMgrFilePath, cleanHubProjectName, hubVersionName);
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
