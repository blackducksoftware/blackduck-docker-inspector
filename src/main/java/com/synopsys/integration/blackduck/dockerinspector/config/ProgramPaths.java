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
package com.synopsys.integration.blackduck.dockerinspector.config;

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

    private static final String CONTAINER_PROGRAM_DIR = String.format("%s/blackduck-docker-inspector/", Config.CONTAINER_BLACKDUCK_DIR);
    private static final String CONTAINER_JAR_PATH = String.format("%s/hub-docker-inspector.jar", CONTAINER_PROGRAM_DIR);

    private String dockerInspectorPgmDirPathHost;
    private String dockerInspectorPgmDirPathContainer;

    private String dockerInspectorRunDirName;
    private String dockerInspectorRunDirPathHost;

    public static final String APPLICATION_PROPERTIES_FILENAME = "application.properties";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String dockerInspectorConfigDirPathHost;
    private String dockerInspectorConfigDirPathContainer;
    private String dockerInspectorTempDirPathContainer;
    private String dockerInspectorConfigFilePathHost;
    private String dockerInspectorConfigFilePathContainer;
    private String dockerInspectorTargetDirPathHost;
    private String dockerInspectorTargetDirPathContainer;
    private String dockerInspectorJarPathActual;
    private String dockerInspectorJarPathHost;
    private String dockerInspectorWorkingDirPathHost;
    private String dockerInspectorWorkingDirPathContainer;
    private String dockerInspectorOutputPathHost;
    private String dockerInspectorOutputPathContainer;
    private String dockerInspectorContainerResultPathOnHost;
    private String dockerInspectorContainerResultPathInContainer;
    private String dockerInspectorHostResultPath;
    private String cleanedProcessId;

    private String getProgramDirPathHost() {
        if (config.isImageInspectorServiceStart()) {
            final File sharedDir = new File(config.getWorkingDirPath());
            logger.debug(String.format("getProgramDirPathHost(): returning %s", sharedDir.getAbsolutePath()));
            return sharedDir.getAbsolutePath();
        }
        logger.debug(String.format("getProgramDirPathHost(): returning %s", config.getWorkingDirPath()));
        return config.getWorkingDirPath();
    }

    @PostConstruct
    public void init() {
        cleanedProcessId = atSignToUnderscore(getProcessIdOrGenerateUniqueId());
        logger.info(String.format("Process name: %s", cleanedProcessId));
        dockerInspectorJarPathActual = deriveJarPath();
        if (StringUtils.isBlank(dockerInspectorPgmDirPathHost)) {
            dockerInspectorPgmDirPathHost = getProgramDirPathHost();
        }
        logger.debug(String.format("dockerInspectorPgmDirPathHost: %s", dockerInspectorPgmDirPathHost));
        dockerInspectorRunDirName = adjustWithProcessId(RUNDIR_BASENAME);
        final File runDirHost = new File(dockerInspectorPgmDirPathHost, dockerInspectorRunDirName);
        dockerInspectorRunDirPathHost = runDirHost.getAbsolutePath() + "/";
        logger.debug(String.format("dockerInspectorRunDirPathHost: %s", dockerInspectorRunDirPathHost));

        dockerInspectorJarPathHost = dockerInspectorJarPathActual;
        dockerInspectorPgmDirPathContainer = CONTAINER_PROGRAM_DIR;
        dockerInspectorConfigDirPathHost = new File(runDirHost, CONFIG_DIR).getAbsolutePath() + "/";
        dockerInspectorConfigDirPathContainer = dockerInspectorPgmDirPathContainer + CONFIG_DIR + "/";
        dockerInspectorTempDirPathContainer = dockerInspectorPgmDirPathContainer + TEMP_DIR + "/";
        dockerInspectorConfigFilePathHost = dockerInspectorConfigDirPathHost + APPLICATION_PROPERTIES_FILENAME;
        dockerInspectorConfigFilePathContainer = dockerInspectorConfigDirPathContainer + APPLICATION_PROPERTIES_FILENAME;
        dockerInspectorTargetDirPathHost = new File(runDirHost, TARGET_DIR).getAbsolutePath() + "/";
        dockerInspectorTargetDirPathContainer = dockerInspectorPgmDirPathContainer + TARGET_DIR + "/";
        dockerInspectorWorkingDirPathHost = new File(runDirHost, WORKING_DIR).getAbsolutePath() + "/";
        dockerInspectorWorkingDirPathContainer = dockerInspectorPgmDirPathContainer + WORKING_DIR + "/";
        dockerInspectorOutputPathHost = new File(runDirHost, OUTPUT_DIR).getAbsolutePath() + "/";
        dockerInspectorOutputPathContainer = CONTAINER_PROGRAM_DIR + OUTPUT_DIR + "/";
        dockerInspectorContainerResultPathOnHost = dockerInspectorOutputPathHost + CONTAINER_RESULT_JSON_FILENAME;
        dockerInspectorContainerResultPathInContainer = dockerInspectorOutputPathContainer + CONTAINER_RESULT_JSON_FILENAME;
        dockerInspectorHostResultPath = dockerInspectorOutputPathHost + HOST_RESULT_JSON_FILENAME;

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
        final String dockerInspectorJarPathActual = qualifiedJarPathString.substring(startIndex, endIndex);
        logger.debug(String.format("dockerInspectorJarPathActual: %s", dockerInspectorJarPathActual));
        return dockerInspectorJarPathActual;
    }

    public String getQualifiedJarPath() {
        return this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
    }

    public String getDockerInspectorConfigDirPathHost() {
        return dockerInspectorConfigDirPathHost;
    }

    public String getDockerInspectorTempDirPathContainer() {
        return dockerInspectorTempDirPathContainer;
    }

    public String getDockerInspectorConfigDirPathContainer() {
        return dockerInspectorConfigDirPathContainer;
    }

    public String getDockerInspectorConfigFilePathHost() {
        return dockerInspectorConfigFilePathHost;
    }

    public String getDockerInspectorConfigFilePathContainer() {
        return dockerInspectorConfigFilePathContainer;
    }

    public String getDockerInspectorTargetDirPathHost() {
        return dockerInspectorTargetDirPathHost;
    }

    public String getDockerInspectorTargetDirPathContainer() {
        return dockerInspectorTargetDirPathContainer;
    }

    public String getDockerInspectorTargetDirPath() {
        if (config.isOnHost()) {
            return getDockerInspectorTargetDirPathHost();
        } else {
            return getDockerInspectorTargetDirPathContainer();
        }
    }

    public String getDockerInspectorPgmDirPathHost() {
        return dockerInspectorPgmDirPathHost;
    }

    public String getDockerInspectorRunDirName() {
        return dockerInspectorRunDirName;
    }

    public String getDockerInspectorRunDirPathHost() {
        return dockerInspectorRunDirPathHost;
    }

    public String getDockerInspectorPgmDirPathContainer() {
        return dockerInspectorPgmDirPathContainer;
    }

    public String getDockerInspectorJarPathHost() {
        return dockerInspectorJarPathHost;
    }

    public String getDockerInspectorJarFilenameHost() {
        final File jarFile = new File(dockerInspectorJarPathHost);
        return jarFile.getName();
    }

    public String getDockerInspectorJarPathContainer() {
        return CONTAINER_JAR_PATH;
    }

    public String getDockerInspectorJarPathActual() {
        return dockerInspectorJarPathActual;
    }

    public String getDockerInspectorWorkingDirPath() {
        if (config.isOnHost()) {
            return getDockerInspectorWorkingDirPathHost();
        } else {
            return getDockerInspectorWorkingDirPathContainer();
        }
    }

    public String getDockerInspectorWorkingDirPathHost() {
        return dockerInspectorWorkingDirPathHost;
    }

    public String getDockerInspectorWorkingDirPathContainer() {
        return dockerInspectorWorkingDirPathContainer;
    }

    public String getDockerInspectorOutputPath() {
        if (config.isOnHost()) {
            return getDockerInspectorOutputPathHost();
        } else {
            return getDockerInspectorOutputPathContainer();
        }
    }

    public String getDockerInspectorOutputPathHost() {
        if (StringUtils.isNotBlank(config.getImageInspectorUrl())) {
            final File outputDir = new File(this.getDockerInspectorRunDirPathHost(), OUTPUT_DIR);
            return outputDir.getAbsolutePath();
        }
        return dockerInspectorOutputPathHost;
    }

    public String getDockerInspectorContainerResultPath() {
        if (config.isOnHost()) {
            return getDockerInspectorContainerResultPathOnHost();
        } else {
            return getDockerInspectorContainerResultPathInContainer();
        }
    }

    public String getDockerInspectorContainerResultPathOnHost() {
        return dockerInspectorContainerResultPathOnHost;
    }

    public String getDockerInspectorContainerResultPathInContainer() {
        return dockerInspectorContainerResultPathInContainer;
    }

    public String getDockerInspectorHostResultPath() {
        return dockerInspectorHostResultPath;
    }

    public String getDockerInspectorOutputPathContainer() {
        return dockerInspectorOutputPathContainer;
    }

    public void setDockerInspectorPgmDirPathHost(final String dockerInspectorPgmDirPath) {
        this.dockerInspectorPgmDirPathHost = dockerInspectorPgmDirPath;
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
        config.setBlackDuckCodelocationPrefix(codeLocationPrefix);
    }

    private String atSignToUnderscore(final String imageName) {
        return imageName.replaceAll("@", "_");
    }

    void setConfig(final Config config) {
        this.config = config;
    }
}
