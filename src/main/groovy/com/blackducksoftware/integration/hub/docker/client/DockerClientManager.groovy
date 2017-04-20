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
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.command.PullImageCmd
import com.github.dockerjava.api.command.SaveImageCmd
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.DefaultDockerClientConfig.Builder
import com.github.dockerjava.core.command.PullImageResultCallback

@Component
class DockerClientManager {
    private final Logger logger = LoggerFactory.getLogger(DockerClientManager.class)

    @Value('${working.directory}')
    String workingDirectoryPath

    private DockerClient client

    public DockerClientManager() {
        client = getDockerClient()
    }

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
        PullImageCmd pull = client.pullImageCmd("${imageName}").withTag(tagName)
        pull.exec(new PullImageResultCallback()).awaitSuccess()
    }

    void run(String imageName, String tagName) {
        String imageId = "${imageName}:${tagName}"
        logger.info("Running container based on image ${imageId}")


        CreateContainerResponse container = dockerClient.createContainerCmd(imageId)
                .withTty(true)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withCmd("/bin/bash")
                .exec();

        dockerClient.startContainerCmd(container.getId()).exec();

        logger.info(sprintf("Started container %s from image %s", container.getId(), imageId))
    }

    private saveImage(String imageName, String tagName, File imageTarFile) {
        InputStream tarInputStream = null
        try{
            logger.info("Saving the docker image to : ${imageTarFile.getAbsolutePath()}")
            SaveImageCmd saveCommand = client.saveImageCmd(imageName)
            saveCommand.withTag(tagName)
            tarInputStream = saveCommand.exec()
            FileUtils.copyInputStreamToFile(tarInputStream, imageTarFile);
        } finally{
            IOUtils.closeQuietly(tarInputStream);
        }
    }

    private DockerClient getDockerClient(){
        Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder()
        DefaultDockerClientConfig config =  builder.build()

        DockerClientBuilder.getInstance(config).build();
    }
}
