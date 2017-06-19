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

	private static final String DEFAULT_PGM_DIR = "/opt/blackduck/hub-docker-inspector"

	public static final String APPLICATION_PROPERTIES_FILENAME = "application.properties"

	private final Logger logger = LoggerFactory.getLogger(ProgramPaths.class)

	private String hubDockerConfigDirPath
	private String hubDockerConfigFilePath
	private String hubDockerTargetDirPath
	private String hubDockerJarPath
	private boolean initDone = false

	private void init() {
		if (initDone) {
			return
		}
		if (StringUtils.isBlank(hubDockerPgmDirPath)) {
			logger.info("hubDockerPgmDirPath had no value; using: ${DEFAULT_PGM_DIR}")
			hubDockerPgmDirPath = DEFAULT_PGM_DIR;
		}
		if (!hubDockerPgmDirPath.endsWith("/")) {
			hubDockerPgmDirPath += "/"
		}
		hubDockerConfigDirPath = hubDockerPgmDirPath + "config/"
		hubDockerConfigFilePath = hubDockerConfigDirPath + APPLICATION_PROPERTIES_FILENAME
		hubDockerTargetDirPath = hubDockerPgmDirPath + "target/"

		String qualifiedJarPathString = getQualifiedJarPath()
		logger.debug("qualifiedJarPathString: ${qualifiedJarPathString}")
		String prefix = "${hubDockerPgmDirPath}hub-docker-"
		logger.debug("prefix: ${prefix}")
		int startIndex = qualifiedJarPathString.indexOf(prefix)
		int endIndex = qualifiedJarPathString.indexOf(".jar") + ".jar".length()
		hubDockerJarPath = qualifiedJarPathString.substring(startIndex, endIndex)

		initDone = true
	}

	String getQualifiedJarPath() {
		return new java.io.File(DockerClientManager.class.getProtectionDomain()
				.getCodeSource()
				.getLocation()
				.getPath()).getAbsolutePath()
	}

	public String getHubDockerConfigDirPath() {
		init()
		hubDockerConfigDirPath
	}

	public String getHubDockerConfigFilePath() {
		init()
		hubDockerConfigFilePath
	}

	public String getHubDockerTargetDirPath() {
		init()
		hubDockerTargetDirPath
	}

	public String getHubDockerPgmDirPath() {
		init()
		hubDockerPgmDirPath
	}

	public String getHubDockerJarPath() {
		init()
		return hubDockerJarPath;
	}
}