/*
 * Copyright (C) 2017 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Black Duck Software.
 */
package com.blackducksoftware.integration.hub.docker.client

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class ProgramPaths {

    @Value('${install.dir}')
    String hubDockerPgmDirPath

    private static final String DEFAULT_PGM_DIR = "/opt/blackduck/hub-docker"

    private final Logger logger = LoggerFactory.getLogger(ProgramPaths.class)

    String hubDockerConfigDirPath
    String hubDockerConfigFilePath
    String hubDockerTargetDirPath

    ProgramPaths() {
        if (StringUtils.isBlank(hubDockerPgmDirPath)) {
            logger.info("hubDockerPgmDirPath had no value; using: ${DEFAULT_PGM_DIR}")
            hubDockerPgmDirPath = DEFAULT_PGM_DIR;
        } else {
            logger.info("Read program dir: ${hubDockerPgmDirPath}")
        }
        if (!hubDockerPgmDirPath.endsWith("/")) {
            hubDockerPgmDirPath += "/"
        }
        logger.info("Final program dir: ${hubDockerPgmDirPath}")
        hubDockerConfigDirPath = hubDockerPgmDirPath + "config/"
        hubDockerConfigFilePath = hubDockerConfigDirPath + "application.properties"
        hubDockerTargetDirPath = hubDockerPgmDirPath + "target/"
    }
}
