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
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.dockerclient.DockerClientManager;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.LinuxFileSystem;
import com.synopsys.integration.exception.IntegrationException;

@Component
public class SquashedImage {
    private static final Logger logger = LoggerFactory.getLogger(SquashedImage.class);
    private static final String IMAGE_REPO_PREFIX = "dockerinspectorsquashed";
    private static final String IMAGE_TAG = "1";
    private static final int MAX_NAME_GENERATION_ATTEMPTS = 20;
    private static final int MAX_IMAGE_REPO_INDEX = 10000;

    private DockerClientManager dockerClientManager;
    private FileOperations fileOperations;

    @Autowired
    public void setDockerClientManager(final DockerClientManager dockerClientManager) {
        this.dockerClientManager = dockerClientManager;
    }

    @Autowired
    public void setFileOperations(final FileOperations fileOperations) {
        this.fileOperations = fileOperations;
    }

    public void createSquashedImageTarGzORIGINAL(final File targetImageFileSystemTarGz, final File squashedImageTarGz,
        final File tempTarFile, final File tempWorkingDir) throws IOException, IntegrationException {
        logger.info(String.format("Transforming container filesystem %s to squashed image %s", targetImageFileSystemTarGz, squashedImageTarGz));
        final File dockerBuildDir = tempWorkingDir;
        final File containerFileSystemDir = new File(dockerBuildDir, "containerFileSystem");
        containerFileSystemDir.mkdirs();
        CompressedFile.gunZipUnTarFile(targetImageFileSystemTarGz, tempTarFile, containerFileSystemDir);
        fileOperations.pruneDanglingSymLinksRecursively(containerFileSystemDir);
        final File dockerfile = new File(dockerBuildDir, "Dockerfile");
        final String dockerfileContents = String.format("FROM scratch\nCOPY %s/* .\n", containerFileSystemDir.getName());
        FileUtils.writeStringToFile(dockerfile, dockerfileContents, StandardCharsets.UTF_8);

        final String imageRepoTag = generateUniqueImageRepoTag();
        final Set<String> tags = new HashSet<>();
        tags.add(imageRepoTag);
        final String squashedImageId = dockerClientManager.buildImage(dockerBuildDir, tags);

        try {
            final File generatedSquashedImageTarfile = dockerClientManager.getTarFileFromDockerImageById(squashedImageId, tempWorkingDir);
            logger.info(String.format("Generated squashed tarfile: %s", generatedSquashedImageTarfile.getAbsolutePath()));
            CompressedFile.gZipFile(generatedSquashedImageTarfile, squashedImageTarGz);
        } finally {
            logger.debug(String.format("Removing temporary squashed image: %s", imageRepoTag));
            final String[] imageRepoTagParts = imageRepoTag.split(":");
            final Optional<String> imageId = dockerClientManager.lookupImageIdByRepoTag(imageRepoTagParts[0], imageRepoTagParts[1]);
            if (imageId.isPresent()) {
                dockerClientManager.removeImage(imageId.get());
            } else {
                logger.warn(String.format("Unable to remove temporary squashed image because image %s was not found", imageRepoTag));
            }
        }
    }

    public void createSquashedImageTarGz(final File targetImageFileSystemTarGz, final File squashedImageTarGz,
        final File tempTarFile, final File tempWorkingDir) throws IOException, IntegrationException {
        logger.info(String.format("Transforming container filesystem %s to squashed image %s", targetImageFileSystemTarGz, squashedImageTarGz));
        final File preTarImageDir = new File(tempWorkingDir, "preTarImageDir");

        final File imageJsonFile = new File(preTarImageDir, "62dba2907d08c02d77092f407ef77aa748fa9f89f7672d430d326f9f24a9ae33.json");
        FileUtils.writeStringToFile(imageJsonFile, "{\"architecture\":\"amd64\",\"config\":{\"Hostname\":\"\",\"Domainname\":\"\",\"User\":\"\",\"AttachStdin\":false,\"AttachStdout\":false,\"AttachStderr\":false,\"Tty\":false,\"OpenStdin\":false,\"StdinOnce\":false,\"Env\":[\"PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\"],\"Cmd\":null,\"Image\":\"\",\"Volumes\":null,\"WorkingDir\":\"\",\"Entrypoint\":null,\"OnBuild\":null,\"Labels\":null},\"container_config\":{\"Hostname\":\"\",\"Domainname\":\"\",\"User\":\"\",\"AttachStdin\":false,\"AttachStdout\":false,\"AttachStderr\":false,\"Tty\":false,\"OpenStdin\":false,\"StdinOnce\":false,\"Env\":[\"PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\"],\"Cmd\":[\"/bin/sh\",\"-c\",\"#(nop) COPY dir:2667d8ef8d59ebd8b1266eb50abf1338967aa1340d0baa44138af01f3e42245d in . \"],\"Image\":\"\",\"Volumes\":null,\"WorkingDir\":\"\",\"Entrypoint\":null,\"OnBuild\":null,\"Labels\":null},\"created\":\"2019-10-09T15:42:04.067613755Z\",\"docker_version\":\"19.03.2\",\"history\":[{\"created\":\"2019-10-09T15:42:04.067613755Z\",\"created_by\":\"/bin/sh -c #(nop) COPY dir:2667d8ef8d59ebd8b1266eb50abf1338967aa1340d0baa44138af01f3e42245d in . \"}],\"os\":\"linux\",\"rootfs\":{\"type\":\"layers\",\"diff_ids\":[\"sha256:82a2c5b99771f499619ffc14c56c247655c0ec46cb2444bd6fa3984cb08a3f69\"]}}", StandardCharsets.UTF_8);

        final File manifestFile = new File(preTarImageDir, "manifest.json");
        FileUtils.writeStringToFile(manifestFile, "[{\"Config\":\"62dba2907d08c02d77092f407ef77aa748fa9f89f7672d430d326f9f24a9ae33.json\",\"RepoTags\":[\"dockerinspectorsquashed-9079:1\"],\"Layers\":[\"00d3ac8e0d1124c22545747a7c54c48368a870550d5cef139278314ad704e663/layer.tar\"]}]\n", StandardCharsets.UTF_8);

        final File repositoriesFile = new File(preTarImageDir, "repositories");
        FileUtils.writeStringToFile(repositoriesFile, "{\"dockerinspectorsquashed-9079\":{\"1\":\"00d3ac8e0d1124c22545747a7c54c48368a870550d5cef139278314ad704e663\"}}\n", StandardCharsets.UTF_8);

        final File layerDir = new File(preTarImageDir, "00d3ac8e0d1124c22545747a7c54c48368a870550d5cef139278314ad704e663");
        layerDir.mkdir();

        final File layerTarFile = new File(layerDir, "layer.tar");
        final File layerTarStagingDir = new File(tempWorkingDir, "layerTarStagingDir");
        final File containerFileSystemDir = new File(layerTarStagingDir, "containerFileSystem");
        containerFileSystemDir.mkdir();
        CompressedFile.gunZipUnTarFile(targetImageFileSystemTarGz, tempTarFile, containerFileSystemDir);
        final File[] containerFileSystemDirFiles = containerFileSystemDir.listFiles();
        final File containerFilesSystemRoot = containerFileSystemDirFiles[0];
        fileOperations.pruneDanglingSymLinksRecursively(containerFilesSystemRoot);
        //final File containerFileSystemRootDir = new File(containerFileSystemDir, "containerfsdir");
        final LinuxFileSystem containerFileSystem = new LinuxFileSystem(containerFilesSystemRoot, new FileOperations());
        containerFileSystem.writeToTar(layerTarFile);

        final LinuxFileSystem preTarImage = new LinuxFileSystem(preTarImageDir, new FileOperations());
        final String squashedImageTarfileName = String.format("%s_notYetGZipped", targetImageFileSystemTarGz.getName());
        final File squashedImageTar = new File(tempWorkingDir, squashedImageTarfileName);
        preTarImage.writeToTar(squashedImageTar);

        CompressedFile.gZipFile(squashedImageTar, squashedImageTarGz);

        final File versionFile = new File(layerDir, "VERSION");
        FileUtils.writeStringToFile(versionFile, "1.0", StandardCharsets.UTF_8);

        final File jsonFile = new File(layerDir, "json");
        FileUtils.writeStringToFile(jsonFile, "{\"id\":\"00d3ac8e0d1124c22545747a7c54c48368a870550d5cef139278314ad704e663\",\"created\":\"2019-10-09T15:42:04.067613755Z\",\"container_config\":{\"Hostname\":\"\",\"Domainname\":\"\",\"User\":\"\",\"AttachStdin\":false,\"AttachStdout\":false,\"AttachStderr\":false,\"Tty\":false,\"OpenStdin\":false,\"StdinOnce\":false,\"Env\":[\"PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\"],\"Cmd\":[\"/bin/sh\",\"-c\",\"#(nop) COPY dir:2667d8ef8d59ebd8b1266eb50abf1338967aa1340d0baa44138af01f3e42245d in . \"],\"Image\":\"\",\"Volumes\":null,\"WorkingDir\":\"\",\"Entrypoint\":null,\"OnBuild\":null,\"Labels\":null},\"docker_version\":\"19.03.2\",\"config\":{\"Hostname\":\"\",\"Domainname\":\"\",\"User\":\"\",\"AttachStdin\":false,\"AttachStdout\":false,\"AttachStderr\":false,\"Tty\":false,\"OpenStdin\":false,\"StdinOnce\":false,\"Env\":[\"PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\"],\"Cmd\":null,\"Image\":\"\",\"Volumes\":null,\"WorkingDir\":\"\",\"Entrypoint\":null,\"OnBuild\":null,\"Labels\":null},\"architecture\":\"amd64\",\"os\":\"linux\"}", StandardCharsets.UTF_8);
    }

    String generateUniqueImageRepoTag() throws IntegrationException {

        for (int i=0; i < MAX_NAME_GENERATION_ATTEMPTS; i++) {
            int randomImageRepoIndex = (int)(Math.random() * MAX_IMAGE_REPO_INDEX);
            final String imageRepoCandidate = String.format("%s-%d", IMAGE_REPO_PREFIX, randomImageRepoIndex);
            final String imageRepoTagCandidate = String.format("%s:%s", imageRepoCandidate, IMAGE_TAG);
            logger.debug(String.format("Squashed image repo:name candidate: %s", imageRepoTagCandidate));
            final Optional<String> foundImageId = dockerClientManager.lookupImageIdByRepoTag(imageRepoCandidate, IMAGE_TAG);
            if (!foundImageId.isPresent()) {
                return imageRepoTagCandidate;
            } else {
                logger.debug("\tImage repo:name %s is not available", imageRepoTagCandidate);
            }
        }
        throw new IntegrationException(String.format("Failed to find an available image repo:tag to use when building the squashed image"));
    }
}
