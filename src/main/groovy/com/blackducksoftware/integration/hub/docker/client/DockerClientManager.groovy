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

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.docker.Application
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.CopyArchiveToContainerCmd
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.command.ExecCreateCmdResponse
import com.github.dockerjava.api.command.PullImageCmd
import com.github.dockerjava.api.command.SaveImageCmd
import com.github.dockerjava.api.model.Container
import com.github.dockerjava.core.command.ExecStartResultCallback
import com.github.dockerjava.core.command.PullImageResultCallback


@Component
class DockerClientManager {
    private final Logger logger = LoggerFactory.getLogger(DockerClientManager.class)

    @Autowired
    HubDockerClient hubDockerClient

    @Autowired
    ProgramPaths programPaths

    @Value('${working.directory}')
    String workingDirectoryPath

    File getTarFileFromDockerImage(String imageName, String tagName) {
        // use docker to pull image if necessary
        // use docker to save image to tar
        // performExtractFromDockerTar()
        File imageTarDirectory = new File(new File(workingDirectoryPath), 'tarDirectory')
        pullImage(imageName, tagName)
        File imageTarFile = new File(imageTarDirectory, "${imageName}_${tagName}.tar")
        saveImage(imageName, tagName, imageTarFile)
        imageTarFile
    }

    void pullImage(String imageName, String tagName) {
        logger.info("Pulling image ${imageName}:${tagName}")
        DockerClient dockerClient = hubDockerClient.getDockerClient()
        PullImageCmd pull = dockerClient.pullImageCmd("${imageName}").withTag(tagName)
        pull.exec(new PullImageResultCallback()).awaitSuccess()
    }

    void run(String imageName, String tagName, File dockerTarFile) {
        String imageId = "${imageName}:${tagName}"
        logger.info("Running container based on image ${imageId}")

        DockerClient dockerClient = hubDockerClient.getDockerClient()

        String tarFileDirInSubContainer = programPaths.getHubDockerTargetDirPath()
        String tarFilePathInSubContainer = programPaths.getHubDockerTargetDirPath() + dockerTarFile.getName()

        String containerId = ''
        boolean isContainerRunning = false
        List<Container> containers = dockerClient.listContainersCmd().exec()
        Container extractorContainer = containers.find{ container ->
            //FIXME There should be a better way to do this
            boolean foundName = false
            for(String name : container.getNames()){
                // name prefixed with '/' for some reason
                if(name.contains(Application.HUB_DOCKER_EXTRACTOR_CONTAINER)){
                    foundName = true
                }
            }
            foundName
        }
        if(extractorContainer != null){
            containerId = extractorContainer.getId()
            if(extractorContainer.getStatus().startsWith('Up')){
                isContainerRunning = true
            }
        } else{
            CreateContainerResponse containerResponse = dockerClient.createContainerCmd(imageId)
                    .withTty(true)
                    .withName(Application.HUB_DOCKER_EXTRACTOR_CONTAINER)
                    .withCmd('/bin/bash')
                    .exec()

            containerId = containerResponse.getId()

            String srcPath = programPaths.getHubDockerConfigFilePath()
            String destPath = programPaths.getHubDockerConfigDirPath()
            copyFileToContainer(dockerClient, containerId, srcPath, destPath)

            logger.info(sprintf("Docker image tar file: %s", dockerTarFile.getAbsolutePath()))
            logger.info(sprintf("Docker image tar file path in sub-container: %s", tarFilePathInSubContainer))
            copyFileToContainer(dockerClient, containerId, dockerTarFile.getAbsolutePath(), tarFileDirInSubContainer);

        }
        if(!isContainerRunning){
            dockerClient.startContainerCmd(containerId).exec()
            logger.info(sprintf("Started container %s from image %s", containerId, imageId))
        }
        String cmd = programPaths.getHubDockerPgmDirPath() + "scan-docker-image-tar.sh"
        execCommandInContainer(dockerClient, imageId, containerId, cmd, tarFilePathInSubContainer)
    }

    private execCommandInContainer(DockerClient dockerClient, String imageId, String containerId, String cmd, String arg) {
        logger.info(sprintf("Running %s on %s in container %s from image %s", cmd, arg, containerId, imageId))
        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd(cmd, arg).exec()
        dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(
                new ExecStartResultCallback(System.out, System.err)).awaitCompletion()
    }

    private copyFileToContainer(DockerClient dockerClient, String containerId, String srcPath, String destPath) {
        logger.info("Copying ${srcPath} to container ${containerId}: ${destPath}")
        CopyArchiveToContainerCmd  copyProperties = dockerClient.copyArchiveToContainerCmd(containerId).withHostResource(srcPath).withRemotePath(destPath)
        copyProperties.exec()
        logger.info("Copied ${srcPath} to container ${containerId}: ${destPath}")
    }

    private saveImage(String imageName, String tagName, File imageTarFile) {
        InputStream tarInputStream = null
        try{
            logger.info("Saving the docker image to : ${imageTarFile.getAbsolutePath()}")
            DockerClient dockerClient = hubDockerClient.getDockerClient()
            SaveImageCmd saveCommand = dockerClient.saveImageCmd(imageName)
            saveCommand.withTag(tagName)
            tarInputStream = saveCommand.exec()
            FileUtils.copyInputStreamToFile(tarInputStream, imageTarFile)
        } finally{
            IOUtils.closeQuietly(tarInputStream)
        }
    }
}
