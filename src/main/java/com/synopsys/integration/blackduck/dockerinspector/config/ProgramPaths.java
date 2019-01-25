/**
 * blackduck-docker-inspector
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
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

    private static final String HOST_RESULT_JSON_FILENAME = "result.json";
    private static final String RUNDIR_BASENAME = "run";
    public static final String OUTPUT_DIR = "output";
    public static final String TARGET_DIR = "target";
    private static final String CONFIG_DIR = "config";
    private String dockerInspectorPgmDirPath;
    private String dockerInspectorRunDirName;
    private String dockerInspectorRunDirPath;

    public static final String APPLICATION_PROPERTIES_FILENAME = "application.properties";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String dockerInspectorConfigDirPath;
    private String dockerInspectorConfigFilePath;
    private String dockerInspectorTargetDirPath;
    private String dockerInspectorOutputPath;
    private String dockerInspectorResultPath;
    private String cleanedProcessId;

    private String getProgramDirPath() {
        if (config.isImageInspectorServiceStart()) {
            final File sharedDir = new File(config.getWorkingDirPath());
            logger.debug(String.format("getProgramDirPath(): returning %s", sharedDir.getAbsolutePath()));
            return sharedDir.getAbsolutePath();
        }
        logger.debug(String.format("getProgramDirPath(): returning %s", config.getWorkingDirPath()));
        return config.getWorkingDirPath();
    }

    @PostConstruct
    public void init() {
        cleanedProcessId = atSignToUnderscore(getProcessIdOrGenerateUniqueId());
        logger.info(String.format("Process name: %s", cleanedProcessId));
        if (StringUtils.isBlank(dockerInspectorPgmDirPath)) {
            dockerInspectorPgmDirPath = getProgramDirPath();
        }
        logger.debug(String.format("dockerInspectorPgmDirPath: %s", dockerInspectorPgmDirPath));
        dockerInspectorRunDirName = adjustWithProcessId(RUNDIR_BASENAME);
        final File runDir = new File(dockerInspectorPgmDirPath, dockerInspectorRunDirName);
        dockerInspectorRunDirPath = runDir.getAbsolutePath() + "/";
        logger.debug(String.format("dockerInspectorRunDirPath: %s", dockerInspectorRunDirPath));
        dockerInspectorConfigDirPath = new File(runDir, CONFIG_DIR).getAbsolutePath() + "/";
        dockerInspectorConfigFilePath = dockerInspectorConfigDirPath + APPLICATION_PROPERTIES_FILENAME;
        dockerInspectorTargetDirPath = new File(runDir, TARGET_DIR).getAbsolutePath() + "/";
        dockerInspectorOutputPath = new File(runDir, OUTPUT_DIR).getAbsolutePath() + "/";
        dockerInspectorResultPath = dockerInspectorOutputPath + HOST_RESULT_JSON_FILENAME;
    }

    private String getProcessIdOrGenerateUniqueId() {
        String processId;
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

    public String getDockerInspectorConfigDirPath() {
        return dockerInspectorConfigDirPath;
    }

    public String getDockerInspectorConfigFilePath() {
        return dockerInspectorConfigFilePath;
    }

    public String getDockerInspectorTargetDirPath() {
        return dockerInspectorTargetDirPath;
    }

    public String getDockerInspectorPgmDirPath() {
        return dockerInspectorPgmDirPath;
    }

    public String getDockerInspectorRunDirName() {
        return dockerInspectorRunDirName;
    }

    public String getDockerInspectorRunDirPath() {
        return dockerInspectorRunDirPath;
    }

    public String getDockerInspectorOutputPath() {
        if (StringUtils.isNotBlank(config.getImageInspectorUrl())) {
            final File outputDir = new File(this.getDockerInspectorRunDirPath(), OUTPUT_DIR);
            return outputDir.getAbsolutePath();
        }
        return dockerInspectorOutputPath;
    }

    public String getDockerInspectorResultPath() {
        return dockerInspectorResultPath;
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

    private String atSignToUnderscore(final String imageName) {
        return imageName.replaceAll("@", "_");
    }

    void setConfig(final Config config) {
        this.config = config;
    }
}
