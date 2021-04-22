/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
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
    public void setDockerClientManager(DockerClientManager dockerClientManager) {
        this.dockerClientManager = dockerClientManager;
    }

    @Autowired
    public void setFileOperations(FileOperations fileOperations) {
        this.fileOperations = fileOperations;
    }

    public void createSquashedImageTarGz(File targetImageFileSystemTarGz, File squashedImageTarGz,
        File tempTarFile, File tempWorkingDir) throws IOException, IntegrationException {
        logger.info(String.format("Transforming container filesystem %s to squashed image %s", targetImageFileSystemTarGz, squashedImageTarGz));
        File dockerBuildDir = tempWorkingDir;
        File containerFileSystemDir = new File(dockerBuildDir, "containerFileSystem");
        containerFileSystemDir.mkdirs();
        CompressedFile.gunZipUnTarFile(targetImageFileSystemTarGz, tempTarFile, containerFileSystemDir);
        fileOperations.pruneProblematicSymLinksRecursively(containerFileSystemDir);
        File dockerfile = new File(dockerBuildDir, "Dockerfile");
        String dockerfileContents = String.format("FROM scratch\nCOPY %s/* .\n", containerFileSystemDir.getName());
        FileUtils.writeStringToFile(dockerfile, dockerfileContents, StandardCharsets.UTF_8);

        String imageRepoTag = generateUniqueImageRepoTag();
        Set<String> tags = new HashSet<>();
        tags.add(imageRepoTag);
        String squashedImageId = dockerClientManager.buildImage(dockerBuildDir, tags);

        try {
            ImageTarWrapper generatedSquashedImageTarfile = dockerClientManager.getTarFileFromDockerImageById(squashedImageId, tempWorkingDir);
            logger.info(String.format("Generated squashed tarfile: %s", generatedSquashedImageTarfile.getFile().getAbsolutePath()));
            CompressedFile.gZipFile(generatedSquashedImageTarfile.getFile(), squashedImageTarGz);
        } finally {
            logger.debug(String.format("Removing temporary squashed image: %s", imageRepoTag));
            String[] imageRepoTagParts = imageRepoTag.split(":");
            Optional<String> imageId = dockerClientManager.lookupImageIdByRepoTag(imageRepoTagParts[0], imageRepoTagParts[1]);
            if (imageId.isPresent()) {
                dockerClientManager.removeImage(imageId.get());
            } else {
                logger.warn(String.format("Unable to remove temporary squashed image because image %s was not found", imageRepoTag));
            }
        }
    }

    String generateUniqueImageRepoTag() throws IntegrationException {

        for (int i = 0; i < MAX_NAME_GENERATION_ATTEMPTS; i++) {
            int randomImageRepoIndex = (int) (Math.random() * MAX_IMAGE_REPO_INDEX);
            String imageRepoCandidate = String.format("%s-%d", IMAGE_REPO_PREFIX, randomImageRepoIndex);
            String imageRepoTagCandidate = String.format("%s:%s", imageRepoCandidate, IMAGE_TAG);
            logger.debug(String.format("Squashed image repo:name candidate: %s", imageRepoTagCandidate));
            Optional<String> foundImageId = dockerClientManager.lookupImageIdByRepoTag(imageRepoCandidate, IMAGE_TAG);
            if (!foundImageId.isPresent()) {
                return imageRepoTagCandidate;
            } else {
                logger.debug(String.format("\tImage repo:name %s is not available", imageRepoTagCandidate));
            }
        }
        throw new IntegrationException(String.format("Failed to find an available image repo:tag to use when building the squashed image"));
    }
}
