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
package com.synopsys.integration.blackduck.dockerinspector.output;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.synopsys.integration.blackduck.dockerinspector.dockerclient.DockerClientManager;
import com.synopsys.integration.exception.IntegrationException;

public class SquashedImage {
    private static final Logger logger = LoggerFactory.getLogger(SquashedImage.class);
    private DockerClientManager dockerClientManager;

    @Autowired
    public void setDockerClientManager(final DockerClientManager dockerClientManager) {
        this.dockerClientManager = dockerClientManager;
    }

    public void createSquashedImageTarGz(final File targetImageFileSystemTarGz, final File squashedImageTarGz,
        final File tempTarFile, final File tempWorkingDir) throws IOException, IntegrationException {
        logger.info(String.format("Transforming container filesystem %s to squashed image %s", targetImageFileSystemTarGz, squashedImageTarGz));
        final File dockerBuildDir = tempWorkingDir;
        final File containerFileSystemDir = new File(dockerBuildDir, "containerFileSystem");
        containerFileSystemDir.mkdirs();
        CompressedFile.gunZipUnTarFile(targetImageFileSystemTarGz, tempTarFile, containerFileSystemDir);
        final File dockerfile = new File(dockerBuildDir, "Dockerfile");
        final String dockerfileContents = String.format("FROM scratch\nCOPY %s .\n", containerFileSystemDir.getName());
        FileUtils.writeStringToFile(dockerfile, dockerfileContents, StandardCharsets.UTF_8);

        final Set<String> tags = new HashSet<>();
        tags.add("tttt:tttt"); // TODO
        final String squashedImageId = dockerClientManager.buildImage(dockerBuildDir, tags);

        final File generatedSquashedImageTarfile = dockerClientManager.getTarFileFromDockerImageById(squashedImageId, tempWorkingDir);
        logger.info(String.format("Generated squashed tarfile: %s", generatedSquashedImageTarfile.getAbsolutePath()));
        CompressedFile.gZipFile(generatedSquashedImageTarfile, squashedImageTarGz);
    }
}
