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

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.CopyArchiveToContainerCmd
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.command.ExecCreateCmd
import com.github.dockerjava.api.command.ExecCreateCmdResponse
import com.github.dockerjava.api.command.PullImageCmd
import com.github.dockerjava.api.command.SaveImageCmd
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.Container
import com.github.dockerjava.api.model.Image
import com.github.dockerjava.core.command.ExecStartResultCallback
import com.github.dockerjava.core.command.PullImageResultCallback


@Component
class DockerClientManager {

	private static final String INSPECTOR_COMMAND = "hub-docker-inspector-launcher.sh"
	private static final String IMAGE_TARFILE_PROPERTY = "docker.tar"
	private final Logger logger = LoggerFactory.getLogger(DockerClientManager.class)

	@Autowired
	HubDockerClient hubDockerClient

	@Autowired
	ProgramPaths programPaths

	@Autowired
	HubDockerProperties hubDockerProperties

	@Value('${working.directory}')
	String workingDirectoryPath

	File getTarFileFromDockerImage(String imageName, String tagName) {
		// use docker to pull image if necessary
		// use docker to save image to tar
		// performExtractFromDockerTar()
		File imageTarDirectory = new File(new File(workingDirectoryPath), 'tarDirectory')
		pullImage(imageName, tagName)
		File imageTarFile = new File(imageTarDirectory, "${imageName.replaceAll(':', '_')}_${tagName}.tar")
		saveImage(imageName, tagName, imageTarFile)
		imageTarFile
	}

	void pullImage(String imageName, String tagName) {
		logger.info("Pulling image ${imageName}:${tagName}")
		DockerClient dockerClient = hubDockerClient.getDockerClient()

		List<Image> images =  dockerClient.listImagesCmd().withImageNameFilter(imageName).exec()
		Image alreadyPulledImage = images.find { image ->
			boolean foundTag = false
			for(String tag : image.getRepoTags()){
				if(tag.contains(tagName)){
					foundTag = true
					break
				}
			}
			foundTag
		}
		if(alreadyPulledImage == null){
			// Only pull if we dont already have it
			PullImageCmd pull = dockerClient.pullImageCmd("${imageName}").withTag(tagName)

			try {
				pull.exec(new PullImageResultCallback()).awaitSuccess()
			} catch (NotFoundException e) {
				logger.error("*** Pull failed: Image ${imageName}:${tagName} not found. Please check the image name/tag")
				throw e
			}
		} else{
			logger.info('Image already pulled')
		}
	}

	void run(String imageName, String tagName, File dockerTarFile, boolean copyJar) {

		String imageId = "${imageName}:${tagName}"
		logger.info("Running container based on image ${imageId}")
		String extractorContainerName = deriveContainerName(imageName)
		logger.debug("Container name: ${extractorContainerName}")
		DockerClient dockerClient = hubDockerClient.getDockerClient()

		String tarFileDirInSubContainer = programPaths.getHubDockerTargetDirPath()
		String tarFilePathInSubContainer = programPaths.getHubDockerTargetDirPath() + dockerTarFile.getName()

		String containerId = ''
		boolean isContainerRunning = false
		List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec()
		Container extractorContainer = containers.find{ container ->
			boolean foundName = false
			for(String name : container.getNames()){
				// name prefixed with '/' for some reason
				logger.debug("Checking running container ${name} to see if it is ${extractorContainerName}")
				if(name.contains(extractorContainerName)){
					logger.debug("The extractor container already exists")
					foundName = true
					break
				}
			}
			foundName
		}
		if(extractorContainer != null){
			containerId = extractorContainer.getId()
			if(extractorContainer.getStatus().startsWith('Up')){
				logger.debug("The extractor container is already running")
				isContainerRunning = true
			}
		} else{
			logger.debug("Creating container ${extractorContainerName} from image ${}")
			CreateContainerResponse containerResponse = dockerClient.createContainerCmd(imageId)
					.withStdinOpen(true)
					.withTty(true)
					.withName(extractorContainerName)
					.withCmd('/bin/bash')
					.exec()

			containerId = containerResponse.getId()
		}
		if(!isContainerRunning){
			dockerClient.startContainerCmd(containerId).exec()
			logger.info(sprintf("Started container %s from image %s", containerId, imageId))
		}
		hubDockerProperties.load()
		hubDockerProperties.set(IMAGE_TARFILE_PROPERTY, tarFilePathInSubContainer)
		String pathToPropertiesFileForSubContainer = "${programPaths.getHubDockerTargetDirPath()}${ProgramPaths.APPLICATION_PROPERTIES_FILENAME}"
		hubDockerProperties.save(pathToPropertiesFileForSubContainer)

		copyFileToContainer(dockerClient, containerId, pathToPropertiesFileForSubContainer, programPaths.getHubDockerConfigDirPath())

		logger.info(sprintf("Docker image tar file: %s", dockerTarFile.getAbsolutePath()))
		logger.info(sprintf("Docker image tar file path in sub-container: %s", tarFilePathInSubContainer))
		copyFileToContainer(dockerClient, containerId, dockerTarFile.getAbsolutePath(), tarFileDirInSubContainer);

		if (copyJar) {
			copyFileToContainer(dockerClient, containerId, programPaths.getHubDockerJarPath(), programPaths.getHubDockerPgmDirPath());
		}

		String cmd = programPaths.getHubDockerPgmDirPath() + INSPECTOR_COMMAND
		execCommandInContainer(dockerClient, imageId, containerId, cmd, tarFilePathInSubContainer)
	}

	private String deriveContainerName(String imageName) {
		String extractorContainerName
		int slashIndex = imageName.lastIndexOf('/')
		if (slashIndex < 0) {
			extractorContainerName = "${imageName}-extractor"
		} else {
			extractorContainerName = imageName.substring(slashIndex+1)
		}
		extractorContainerName
	}

	private void execCommandInContainer(DockerClient dockerClient, String imageId, String containerId, String cmd, String arg) {
		logger.info(sprintf("Running %s on %s in container %s from image %s", cmd, arg, containerId, imageId))
		ExecCreateCmd execCreateCmd = dockerClient.execCreateCmd(containerId)
				.withAttachStdout(true)
				.withAttachStderr(true)
		List<String> commands = [cmd, arg]
		String[] cmdArr = commands.toArray()
		execCreateCmd.withCmd(cmdArr)
		ExecCreateCmdResponse execCreateCmdResponse = execCreateCmd.exec()
		dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(
				new ExecStartResultCallback(System.out, System.err)).awaitCompletion()
	}

	private void copyFileToContainer(DockerClient dockerClient, String containerId, String srcPath, String destPath) {
		logger.info("Copying ${srcPath} to container ${containerId}: ${destPath}")
		CopyArchiveToContainerCmd  copyProperties = dockerClient.copyArchiveToContainerCmd(containerId).withHostResource(srcPath).withRemotePath(destPath)
		copyProperties.exec()
	}

	private void saveImage(String imageName, String tagName, File imageTarFile) {
		InputStream tarInputStream = null
		try{
			logger.info("Saving the docker image to : ${imageTarFile.getCanonicalPath()}")
			DockerClient dockerClient = hubDockerClient.getDockerClient()
			String imageToSave = "${imageName}:${tagName}"
			SaveImageCmd saveCommand = dockerClient.saveImageCmd(imageToSave)
			tarInputStream = saveCommand.exec()
			FileUtils.copyInputStreamToFile(tarInputStream, imageTarFile)
		} finally{
			IOUtils.closeQuietly(tarInputStream)
		}
	}
}