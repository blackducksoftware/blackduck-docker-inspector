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
package com.blackducksoftware.integration.hub.docker.dockerinspector.config;

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
    private static final String SHARED_DIR = "shared";

    @Autowired
    private Config config;

    private static final String CONTAINER_JAR_PATH = "/opt/blackduck/hub-docker-inspector/hub-docker-inspector.jar";

    private static final String JAR_FILE_SUFFIX = ".jar";

    private static final String FILE_URI_PREFIX = "file:";
    private static final String HOST_RESULT_JSON_FILENAME = "result.json";
    private static final String CONTAINER_RESULT_JSON_FILENAME = "containerResult.json";

    private static final String RUNDIR_BASENAME = "run";

    public static final String OUTPUT_DIR = "output";

    private static final String WORKING_DIR = "working";

    public static final String TARGET_DIR = "target";

    private static final String TEMP_DIR = "temp";

    private static final String CONFIG_DIR = "config";

    private static final String CONTAINER_PROGRAM_DIR = "/opt/blackduck/hub-docker-inspector/";

    private String hubDockerPgmDirPathHost;
    private String hubDockerPgmDirPathContainer;

    private String hubDockerRunDirName;
    private String hubDockerRunDirPathHost;

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
    private String hubDockerContainerResultPathOnHost;
    private String hubDockerContainerResultPathInContainer;
    private String hubDockerHostResultPath;
    private String cleanedProcessId;

    private String getProgramDirPathHost() {
        if (config.isImageInspectorServiceStart()) {
            final File workingDir = new File(config.getWorkingDirPath());
            final File sharedDir = new File(workingDir, SHARED_DIR);
            logger.debug(String.format("*** getProgramDirPathHost(): returning %s", sharedDir.getAbsolutePath()));
            return sharedDir.getAbsolutePath();
        }
        // TODO should be able to eliminate the need for this adjustment:
        if (!config.getWorkingDirPath().endsWith("/")) {
            config.setWorkingDirPath(String.format("%s/", config.getWorkingDirPath()));
        }
        logger.debug(String.format("*** getProgramDirPathHost(): returning %s", config.getWorkingDirPath()));
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
        hubDockerRunDirName = adjustWithProcessId(RUNDIR_BASENAME);
        final File runDirHost = new File(hubDockerPgmDirPathHost, hubDockerRunDirName);
        hubDockerRunDirPathHost = runDirHost.getAbsolutePath() + "/";
        logger.debug(String.format("hubDockerRunDirPathHost: %s", hubDockerRunDirPathHost));

        hubDockerJarPathHost = hubDockerJarPathActual;
        hubDockerPgmDirPathContainer = getProgramDirPathContainer();
        hubDockerConfigDirPathHost = new File(runDirHost, CONFIG_DIR).getAbsolutePath() + "/";
        hubDockerConfigDirPathContainer = hubDockerPgmDirPathContainer + CONFIG_DIR + "/";
        hubDockerTempDirPathContainer = hubDockerPgmDirPathContainer + TEMP_DIR + "/";
        hubDockerConfigFilePathHost = hubDockerConfigDirPathHost + APPLICATION_PROPERTIES_FILENAME;
        hubDockerConfigFilePathContainer = hubDockerConfigDirPathContainer + APPLICATION_PROPERTIES_FILENAME;
        hubDockerTargetDirPathHost = new File(runDirHost, TARGET_DIR).getAbsolutePath() + "/";
        hubDockerTargetDirPathContainer = hubDockerPgmDirPathContainer + TARGET_DIR + "/";
        hubDockerWorkingDirPathHost = new File(runDirHost, WORKING_DIR).getAbsolutePath() + "/";
        hubDockerWorkingDirPathContainer = hubDockerPgmDirPathContainer + WORKING_DIR + "/";
        hubDockerOutputPathHost = new File(runDirHost, OUTPUT_DIR).getAbsolutePath() + "/";
        hubDockerOutputPathContainer = getProgramDirPathContainer() + OUTPUT_DIR + "/";
        hubDockerContainerResultPathOnHost = hubDockerOutputPathHost + CONTAINER_RESULT_JSON_FILENAME;
        hubDockerContainerResultPathInContainer = hubDockerOutputPathContainer + CONTAINER_RESULT_JSON_FILENAME;
        hubDockerHostResultPath = hubDockerOutputPathHost + HOST_RESULT_JSON_FILENAME;

    }

    private String getProcessIdOrGenerateUniqueId() {
        String processId = null;
        try {
            processId = ManagementFactory.getRuntimeMXBean().getName();
            return processId;
        } catch (final Throwable t) {
            logger.debug("Unable to get process ID from system");
            final long currentMillisecond = new Date().getTime();
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
        if (!qualifiedJarPathString.contains(prefix)) {
            return null;
        }
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

    public String getHubDockerTargetDirPath() {
        if (config.isOnHost()) {
            return getHubDockerTargetDirPathHost();
        } else {
            return getHubDockerTargetDirPathContainer();
        }
    }

    public String getHubDockerPgmDirPathHost() {
        return hubDockerPgmDirPathHost;
    }

    public String getHubDockerRunDirName() {
        return hubDockerRunDirName;
    }

    public String getHubDockerRunDirPathHost() {
        return hubDockerRunDirPathHost;
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
        if (StringUtils.isNotBlank(config.getImageInspectorUrl())) {
            final File outputDir = new File(config.getSharedDirPathLocal(), OUTPUT_DIR);
            return outputDir.getAbsolutePath();
        }
        return hubDockerOutputPathHost;
    }

    public String getHubDockerContainerResultPath() {
        if (config.isOnHost()) {
            return getHubDockerContainerResultPathOnHost();
        } else {
            return getHubDockerContainerResultPathInContainer();
        }
    }

    public String getHubDockerContainerResultPathOnHost() {
        return hubDockerContainerResultPathOnHost;
    }

    public String getHubDockerContainerResultPathInContainer() {
        return hubDockerContainerResultPathInContainer;
    }

    public String getHubDockerHostResultPath() {
        return hubDockerHostResultPath;
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
