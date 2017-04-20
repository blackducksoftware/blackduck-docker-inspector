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
        // TODO look for a way to reuse dockerClient
        DockerClient dockerClient = hubDockerClient.getDockerClient()
        PullImageCmd pull = dockerClient.pullImageCmd("${imageName}").withTag(tagName)
        pull.exec(new PullImageResultCallback()).awaitSuccess()
    }

    void run(String imageName, String tagName, File dockerTarFile) {
        String imageId = "${imageName}:${tagName}"
        logger.info("Running container based on image ${imageId}")

        // TODO look for a way to reuse dockerClient
        DockerClient dockerClient = hubDockerClient.getDockerClient()
        CreateContainerResponse container = dockerClient.createContainerCmd(imageId)
                .withTty(true)
                .withAttachStdin(true) // TODO remove these
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withCmd("/bin/bash")
                .exec();

        String srcPath = "/opt/blackduck/hub-docker/config/application.properties"
        String destPath = "/opt/blackduck/hub-docker/config/"
        copyFileToContainer(dockerClient, container, srcPath, destPath);

        copyFileToContainer(dockerClient, container, dockerTarFile.getAbsolutePath(), dockerTarFile.getParentFile().getAbsolutePath());

        dockerClient.startContainerCmd(container.getId()).exec();
        logger.info(sprintf("Started container %s from image %s", container.getId(), imageId))

        // Run hub-docker in sub-container
        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(container.getId())
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd("/opt/blackduck/hub-docker/scan-docker-image-tar.sh", dockerTarFile.getAbsolutePath()).exec();
        dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(
                new ExecStartResultCallback(System.out, System.err)).awaitCompletion();
    }

    private copyFileToContainer(DockerClient dockerClient, CreateContainerResponse container, String srcPath, String destPath) {
        CopyArchiveToContainerCmd  copyProperties = dockerClient.copyArchiveToContainerCmd(container.getId()).withHostResource(srcPath).withRemotePath(destPath)
        copyProperties.exec()
        logger.info("Copied ${srcPath} to container ${container.toString()}: ${destPath}")
    }

    private saveImage(String imageName, String tagName, File imageTarFile) {
        InputStream tarInputStream = null
        try{
            logger.info("Saving the docker image to : ${imageTarFile.getAbsolutePath()}")
            // TODO look for a way to reuse dockerClient
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
