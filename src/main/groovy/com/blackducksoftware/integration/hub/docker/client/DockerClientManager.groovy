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
import com.github.dockerjava.core.command.ExecStartResultCallback
import com.github.dockerjava.core.command.PullImageResultCallback

@Component
class DockerClientManager {

    private final Logger logger = LoggerFactory.getLogger(DockerClientManager.class)

    @Autowired
    HubDockerClient hubDockerClient

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
        CreateContainerResponse container = dockerClient.createContainerCmd(imageId)
                .withTty(true)
                .withCmd("/bin/bash")
                .exec();
        String srcPath = Application.HUB_DOCKER_CONFIG_FILE_PATH
        String destPath = Application.HUB_DOCKER_CONFIG_DIR_PATH
        copyFileToContainer(dockerClient, container, srcPath, destPath);

        logger.info(sprintf("Docker image tar file: %s", dockerTarFile.getAbsolutePath()))
        String tarFileDirInSubContainer = Application.HUB_DOCKER_TARGET_DIR_PATH
        String tarFilePathInSubContainer = Application.HUB_DOCKER_TARGET_DIR_PATH + dockerTarFile.getName()
        logger.info(sprintf("Docker image tar file path in sub-container: %s", tarFilePathInSubContainer))
        copyFileToContainer(dockerClient, container, dockerTarFile.getAbsolutePath(), tarFileDirInSubContainer);

        dockerClient.startContainerCmd(container.getId()).exec();
        logger.info(sprintf("Started container %s from image %s", container.getId(), imageId))

        String cmd = Application.HUB_DOCKER_PGM_DIR_PATH + "scan-docker-image-tar.sh"
        String arg = tarFilePathInSubContainer
        execCommandInContainer(dockerClient, imageId, container, cmd, arg);
    }

    private execCommandInContainer(DockerClient dockerClient, String imageId, CreateContainerResponse container, String cmd, String arg) {
        logger.info(sprintf("Running %s on %s in container %s from image %s", cmd, arg, container.getId(), imageId))
        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(container.getId())
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd(cmd, arg).exec();
        dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(
                new ExecStartResultCallback(System.out, System.err)).awaitCompletion()
    }

    private copyFileToContainer(DockerClient dockerClient, CreateContainerResponse container, String srcPath, String destPath) {
        logger.info("Copying ${srcPath} to container ${container.toString()}: ${destPath}")
        CopyArchiveToContainerCmd  copyProperties = dockerClient.copyArchiveToContainerCmd(container.getId()).withHostResource(srcPath).withRemotePath(destPath)
        copyProperties.exec()
        logger.info("Copied ${srcPath} to container ${container.toString()}: ${destPath}")
    }

    private saveImage(String imageName, String tagName, File imageTarFile) {
        InputStream tarInputStream = null
        try{
            logger.info("Saving the docker image to : ${imageTarFile.getAbsolutePath()}")
            DockerClient dockerClient = hubDockerClient.getDockerClient()
            SaveImageCmd saveCommand = dockerClient.saveImageCmd(imageName)
            saveCommand.withTag(tagName)
            tarInputStream = saveCommand.exec()
            FileUtils.copyInputStreamToFile(tarInputStream, imageTarFile);
        } finally{
            IOUtils.closeQuietly(tarInputStream);
        }
    }
}
