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
package com.blackducksoftware.integration.hub.docker

import javax.annotation.PostConstruct

import org.apache.commons.lang.exception.ExceptionUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder

import com.blackducksoftware.integration.hub.docker.client.DockerClientManager
import com.blackducksoftware.integration.hub.docker.client.ProgramVersion
import com.blackducksoftware.integration.hub.docker.image.DockerImages
import com.blackducksoftware.integration.hub.docker.tar.LayerMapping

@SpringBootApplication
class Application {
	private final Logger logger = LoggerFactory.getLogger(Application.class)

	@Value('${docker.tar}')
	String dockerTar

	@Value('${docker.image}')
	String dockerImage

	@Value('${linux.distro}')
	String linuxDistro

	@Value('${dev.mode}')
	boolean devMode

	@Value('${hub.project.name}')
	String hubProjectName

	@Value('${hub.project.version}')
	String hubVersionName

	@Autowired
	HubClient hubClient

	@Autowired
	DockerImages dockerImages

	@Autowired
	HubDockerManager hubDockerManager

	@Autowired
	DockerClientManager dockerClientManager

	@Autowired
	ProgramVersion programVersion

	String dockerImageName
	String dockerTagName

	static void main(final String[] args) {
		new SpringApplicationBuilder(Application.class).logStartupInfo(false).run(args)
	}

	@PostConstruct
	void inspectImage() {
		try {
			init()
			File dockerTarFile = deriveDockerTarFile()

			List<File> layerTars = hubDockerManager.extractLayerTars(dockerTarFile)
			List<LayerMapping> layerMappings = hubDockerManager.getLayerMappings(dockerTarFile.getName(), dockerImageName, dockerTagName)
			File imageFilesDir = hubDockerManager.extractDockerLayers(layerTars, layerMappings)

			OperatingSystemEnum targetOsEnum = hubDockerManager.detectOperatingSystem(linuxDistro, imageFilesDir)
			OperatingSystemEnum requiredOsEnum = dockerImages.getDockerImageOs(targetOsEnum)
			OperatingSystemEnum currentOsEnum = hubDockerManager.detectCurrentOperatingSystem()
			if (currentOsEnum == requiredOsEnum) {
				generateBdio(dockerTarFile, imageFilesDir, layerMappings, currentOsEnum, targetOsEnum)
			} else {
				runInSubContainer(dockerTarFile, currentOsEnum, targetOsEnum)
			}
		} catch (Exception e) {
			logger.error("Error inspecting image: ${e.message}")
			String trace = ExceptionUtils.getStackTrace(e)
			logger.debug("Stack trace: ${trace}")
		}
	}

	private runInSubContainer(File dockerTarFile, OperatingSystemEnum currentOsEnum, OperatingSystemEnum targetOsEnum) {
		String runOnImageName = dockerImages.getDockerImageName(targetOsEnum)
		String runOnImageVersion = dockerImages.getDockerImageVersion(targetOsEnum)
		String msg = sprintf("Image inspection for %s should not be run in this %s docker container; will use docker image %s:%s",
				targetOsEnum.toString(), currentOsEnum.toString(),
				runOnImageName, runOnImageVersion)
		logger.info(msg)
		try {
			dockerClientManager.pullImage(runOnImageName, runOnImageVersion)
		} catch (Exception e) {
			logger.warn(sprintf(
					"Unable to pull docker image %s:%s; proceeding anyway since it may already exist locally",
					runOnImageName, runOnImageVersion))
		}
		dockerClientManager.run(runOnImageName, runOnImageVersion, dockerTarFile, devMode)
	}

	private generateBdio(File dockerTarFile, File imageFilesDir, List layerMappings, OperatingSystemEnum currentOsEnum, OperatingSystemEnum targetOsEnum) {
		String msg = sprintf("Image inspection for %s can be run in this %s docker container; tarfile: %s",
				targetOsEnum.toString(), currentOsEnum.toString(), dockerTarFile.getAbsolutePath())
		logger.info(msg)
		List<File> bdioFiles = hubDockerManager.generateBdioFromImageFilesDir(layerMappings, hubProjectName, hubVersionName, dockerTarFile, imageFilesDir, targetOsEnum)
		if (bdioFiles.size() == 0) {
			logger.warn("No BDIO Files generated")
		} else {
			hubDockerManager.uploadBdioFiles(bdioFiles)
		}
	}

	private init() {
		logger.info("hub-docker-inspector ${programVersion.getProgramVersion()}")
		if (devMode) {
			logger.info("Running in development mode")
		}
		if(StringUtils.isBlank(dockerTagName)){
			dockerTagName = 'latest'
		}
		initImageName()
		logger.info("Inspecting image/tag ${dockerImageName}/${dockerTagName}")
		verifyHubConnection()
		hubDockerManager.init()
		hubDockerManager.cleanWorkingDirectory()
	}

	private void verifyHubConnection() {
		hubClient.testHubConnection()
		logger.info 'Your Hub configuration is valid and a successful connection to the Hub was established.'
		return
	}

	private void initImageName() {
		if (StringUtils.isNotBlank(dockerImage)) {
			String[] imageNameAndTag = dockerImage.split(':')
			if ( (imageNameAndTag.length > 0) && (StringUtils.isNotBlank(imageNameAndTag[0])) ) {
				dockerImageName = imageNameAndTag[0]
			}
			if ( (imageNameAndTag.length > 1) && (StringUtils.isNotBlank(imageNameAndTag[1]))) {
				dockerTagName = imageNameAndTag[1]
			}
		}
	}

	private File deriveDockerTarFile() {
		File dockerTarFile
		if(StringUtils.isNotBlank(dockerTar)) {
			dockerTarFile = new File(dockerTar)
		} else if (StringUtils.isNotBlank(dockerImageName)) {
			dockerTarFile = hubDockerManager.getTarFileFromDockerImage(dockerImageName, dockerTagName)
		}
		dockerTarFile
	}



}
