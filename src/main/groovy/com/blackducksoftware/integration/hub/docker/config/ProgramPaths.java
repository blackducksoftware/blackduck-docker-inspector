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
package com.blackducksoftware.integration.hub.docker.config;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Date;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProgramPaths {
    @Autowired
    private Config config;

    private static final String CONTAINER_JAR_PATH = "/opt/blackduck/hub-docker-inspector/hub-docker-inspector.jar";

    private static final String JAR_FILE_SUFFIX = ".jar";

    private static final String FILE_URI_PREFIX = "file:";

    private static final String RESULT_JSON_FILENAME = "result.json";

    private static final String OUTPUT_DIR = "output";

    private static final String WORKING_DIR = "working";

    private static final String TARGET_DIR = "target";

    private static final String TEMP_DIR = "temp";

    private static final String CONFIG_DIR = "config";

    private static final String CONTAINER_PROGRAM_DIR = "/opt/blackduck/hub-docker-inspector/";

    private String hubDockerPgmDirPathHost;
    private String hubDockerPgmDirPathContainer;
    public static final String APPLICATION_PROPERTIES_FILENAME = "application.properties";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String hubDockerConfigDirPathHost;
    private String hubDockerConfigDirPathContainer;
    private String hubDockerTempDirPathContainer;
    private String hubDockerConfigFilePathHost;
    private String hubDockerConfigFilePathContainer;
    private String hubDockerTargetDirPathHost;
    private String hubDockerTargetDirPathContainer;
    private String hubDockerJarPathActual;
    private String hubDockerJarPathHost;
    private String hubDockerWorkingDirPathHost;
    private String hubDockerWorkingDirPathContainer;
    private String hubDockerOutputPathHost;
    private String hubDockerOutputPathContainer;
    private String hubDockerResultPathHost;
    private String hubDockerResultPathContainer;
    private String cleanedProcessId;

    private String getProgramDirPathHost() {
        if (!config.getWorkingDirPath().endsWith("/")) {
            config.setWorkingDirPath(String.format("%s/", config.getWorkingDirPath()));
        }
        return config.getWorkingDirPath();
    }

    private String getProgramDirPathContainer() {
        return CONTAINER_PROGRAM_DIR;
    }

    @PostConstruct
    public void init() {
        cleanedProcessId = atSignToUnderscore(getProcessIdOrGenerateUniqueId());
        logger.info(String.format("Process name: %s", cleanedProcessId));
        hubDockerJarPathActual = deriveJarPath();
        if (StringUtils.isBlank(hubDockerPgmDirPathHost)) {
            hubDockerPgmDirPathHost = getProgramDirPathHost();
        }
        logger.debug(String.format("hubDockerPgmDirPathHost: %s", hubDockerPgmDirPathHost));
        hubDockerJarPathHost = hubDockerJarPathActual;
        hubDockerPgmDirPathContainer = getProgramDirPathContainer();
        hubDockerConfigDirPathHost = adjustWithProcessId(hubDockerPgmDirPathHost + CONFIG_DIR) + "/";
        hubDockerConfigDirPathContainer = hubDockerPgmDirPathContainer + CONFIG_DIR + "/";
        hubDockerTempDirPathContainer = hubDockerPgmDirPathContainer + TEMP_DIR + "/";
        hubDockerConfigFilePathHost = hubDockerConfigDirPathHost + APPLICATION_PROPERTIES_FILENAME;
        hubDockerConfigFilePathContainer = hubDockerConfigDirPathContainer + APPLICATION_PROPERTIES_FILENAME;
        hubDockerTargetDirPathHost = adjustWithProcessId(hubDockerPgmDirPathHost + TARGET_DIR) + "/";
        hubDockerTargetDirPathContainer = hubDockerPgmDirPathContainer + TARGET_DIR + "/";
        hubDockerWorkingDirPathHost = adjustWithProcessId(hubDockerPgmDirPathHost + WORKING_DIR) + "/";
        hubDockerWorkingDirPathContainer = hubDockerPgmDirPathContainer + WORKING_DIR + "/";
        hubDockerOutputPathHost = adjustWithProcessId(hubDockerPgmDirPathHost + OUTPUT_DIR) + "/";
        hubDockerOutputPathContainer = getProgramDirPathContainer() + OUTPUT_DIR + "/";
        hubDockerResultPathHost = hubDockerOutputPathHost + RESULT_JSON_FILENAME;
        hubDockerResultPathContainer = hubDockerOutputPathContainer + RESULT_JSON_FILENAME;

    }

    private String getProcessIdOrGenerateUniqueId() {
        String processId = null;
        try {
            processId = ManagementFactory.getRuntimeMXBean().getName();
            return processId;
        } catch (final Throwable t) {
            logger.debug("Unable to get process ID from system");
            final long currentMillisecond = (new Date()).getTime();
            processId = Long.toString(currentMillisecond);
        }
        return processId;
    }

    public String unEscape(final String origString) {
        final String unEscapedString = origString.replaceAll("%20", " ");
        return unEscapedString;
    }

    public String getUserOutputDir() {
        if (StringUtils.isBlank(config.getOutputPath())) {
            return null;
        }
        return config.getOutputPath();
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

    public String getQualifiedJarPath() {
        return this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
    }

    public String getHubDockerConfigDirPathHost() {
        return hubDockerConfigDirPathHost;
    }

    public String getHubDockerTempDirPathContainer() {
        return hubDockerTempDirPathContainer;
    }

    public String getHubDockerConfigDirPathContainer() {
        return hubDockerConfigDirPathContainer;
    }

    public String getHubDockerConfigFilePathHost() {
        return hubDockerConfigFilePathHost;
    }

    public String getHubDockerConfigFilePathContainer() {
        return hubDockerConfigFilePathContainer;
    }

    public String getHubDockerTargetDirPathHost() {
        return hubDockerTargetDirPathHost;
    }

    public String getHubDockerTargetDirPathContainer() {
        return hubDockerTargetDirPathContainer;
    }

    public String getHubDockerPgmDirPathHost() {
        return hubDockerPgmDirPathHost;
    }

    public String getHubDockerPgmDirPathContainer() {
        return hubDockerPgmDirPathContainer;
    }

    public String getHubDockerJarPathHost() {
        return hubDockerJarPathHost;
    }

    public String getHubDockerJarFilenameHost() {
        final File jarFile = new File(hubDockerJarPathHost);
        return jarFile.getName();
    }

    public String getHubDockerJarPathContainer() {
        return CONTAINER_JAR_PATH;
    }

    public String getHubDockerJarPathActual() {
        return hubDockerJarPathActual;
    }

    public String getHubDockerWorkingDirPath() {
        if (config.isOnHost()) {
            return getHubDockerWorkingDirPathHost();
        } else {
            return getHubDockerWorkingDirPathContainer();
        }
    }

    public String getHubDockerWorkingDirPathHost() {
        return hubDockerWorkingDirPathHost;
    }

    public String getHubDockerWorkingDirPathContainer() {
        return hubDockerWorkingDirPathContainer;
    }

    public String getHubDockerOutputPath() {
        if (config.isOnHost()) {
            return getHubDockerOutputPathHost();
        } else {
            return getHubDockerOutputPathContainer();
        }
    }

    public String getHubDockerOutputPathHost() {
        return hubDockerOutputPathHost;
    }

    public String getHubDockerResultPath() {
        if (config.isOnHost()) {
            return getHubDockerResultPathHost();
        } else {
            return getHubDockerResultPathContainer();
        }
    }

    public String getHubDockerResultPathHost() {
        return hubDockerResultPathHost;
    }

    public String getHubDockerResultPathContainer() {
        return hubDockerResultPathContainer;
    }

    public String getHubDockerOutputPathContainer() {
        return hubDockerOutputPathContainer;
    }

    public void setHubDockerPgmDirPathHost(final String hubDockerPgmDirPath) {
        this.hubDockerPgmDirPathHost = hubDockerPgmDirPath;
    }

    public String deriveContainerName(final String imageName) {
        String extractorContainerName;
        final int slashIndex = imageName.lastIndexOf('/');
        if (slashIndex < 0) {
            extractorContainerName = String.format("%s-extractor", imageName);
        } else {
            extractorContainerName = imageName.substring(slashIndex + 1);
        }
        return adjustWithProcessId(extractorContainerName);
    }

    private String adjustWithProcessId(final String origName) {
        final String adjustedName = String.format("%s_%s", origName, cleanedProcessId);
        logger.debug(String.format("Adjusted %s to %s", origName, adjustedName));
        return adjustedName;
    }

    void setCodeLocationPrefix(final String codeLocationPrefix) {
        config.setHubCodelocationPrefix(codeLocationPrefix);
    }

    private String atSignToUnderscore(final String imageName) {
        return imageName.replaceAll("@", "_");
    }

    void setConfig(final Config config) {
        this.config = config;
    }
}
