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
package com.synopsys.integration.blackduck.dockerinspector.config;

import com.synopsys.integration.blackduck.dockerinspector.ProcessId;
import java.io.File;

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

    @Autowired
    private ProcessId processId;

    private static final String HOST_RESULT_JSON_FILENAME = "output.json";
    private static final String RUNDIR_BASENAME = "run";
    public static final String OUTPUT_DIR = "output";
    private static final String TARGET_DIR = "target";
    private static final String CONFIG_DIR = "config";
    private static final String SQUASHED_IMAGE_DIR = "squashedImage";
    private String dockerInspectorPgmDirPath;
    private String dockerInspectorRunDirName;
    private String dockerInspectorRunDirPath;

    public static final String APPLICATION_PROPERTIES_FILENAME = "application.properties";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String dockerInspectorConfigDirPath;
    private String dockerInspectorConfigFilePath;
    private String dockerInspectorTargetDirPath;
    private String dockerInspectorDefaultOutputPath;
    private String dockerInspectorResultPath;

    private String getProgramDirPath() {
        final File workingDir = new File(config.getWorkingDirPath());
        logger.debug(String.format("getProgramDirPath(): returning %s", config.getWorkingDirPath()));
        return workingDir.getAbsolutePath();
    }

    @PostConstruct
    public void init() {
        dockerInspectorPgmDirPath = getProgramDirPath();
        logger.debug(String.format("dockerInspectorPgmDirPath: %s", dockerInspectorPgmDirPath));
        dockerInspectorRunDirName = processId.addProcessIdToName(RUNDIR_BASENAME);
        final File runDir = new File(dockerInspectorPgmDirPath, dockerInspectorRunDirName);
        dockerInspectorRunDirPath = runDir.getAbsolutePath() + "/";
        logger.debug(String.format("dockerInspectorRunDirPath: %s", dockerInspectorRunDirPath));
        dockerInspectorConfigDirPath = new File(runDir, CONFIG_DIR).getAbsolutePath() + "/";
        dockerInspectorConfigFilePath = dockerInspectorConfigDirPath + APPLICATION_PROPERTIES_FILENAME;
        dockerInspectorTargetDirPath = new File(runDir, TARGET_DIR).getAbsolutePath() + "/";
        dockerInspectorDefaultOutputPath = new File(runDir, OUTPUT_DIR).getAbsolutePath() + "/";
        dockerInspectorResultPath = dockerInspectorDefaultOutputPath + HOST_RESULT_JSON_FILENAME;
    }

    public String getUserOutputDirPath() {
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

    public String getDockerInspectorDefaultOutputPath() {
        return dockerInspectorDefaultOutputPath;
    }

    public String getDockerInspectorResultPath() {
        return dockerInspectorResultPath;
    }

    void setConfig(final Config config) {
        this.config = config;
    }
}
