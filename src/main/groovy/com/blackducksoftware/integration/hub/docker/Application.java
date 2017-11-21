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
package com.blackducksoftware.integration.hub.docker;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.docker.client.DockerClientManager;
import com.blackducksoftware.integration.hub.docker.client.ProgramVersion;
import com.blackducksoftware.integration.hub.docker.hub.HubClient;
import com.blackducksoftware.integration.hub.docker.image.DockerImages;
import com.blackducksoftware.integration.hub.docker.linux.FileOperations;
import com.blackducksoftware.integration.hub.docker.linux.FileSys;
import com.blackducksoftware.integration.hub.docker.result.Result;
import com.blackducksoftware.integration.hub.docker.result.ResultFile;
import com.blackducksoftware.integration.hub.docker.tar.manifest.ManifestLayerMapping;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.google.gson.Gson;

@SpringBootApplication
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    // User should specify docker.tar OR docker.image
    @Value("${docker.tar}")
    private String dockerTar;

    // This may or may not have tag, like: ubuntu or ubuntu:16.04
    @Value("${docker.image}")
    private String dockerImage;

    @Value("${docker.image.id}")
    private String dockerImageId;

    // docker.image.repo and docker.image.tag are for selecting an image
    // from a multi-image tarfile
    @Value("${docker.image.repo}")
    private String dockerImageRepo;

    @Value("${docker.image.tag}")
    private String dockerImageTag;

    @Value("${linux.distro}")
    private String linuxDistro;

    @Value("${hub.project.name}")
    private String hubProjectName;

    @Value("${hub.project.version}")
    private String hubVersionName;

    @Value("${dry.run}")
    private boolean dryRun;

    @Value("${output.include.dockertarfile}")
    private boolean outputIncludeDockerTarfile;

    @Value("${output.include.containerfilesystem}")
    private boolean outputIncludeContainerFileSystemTarfile;

    @Value("${on.host:true}")
    private boolean onHost;

    @Value("${cleanup.working.dir:true}")
    private boolean cleanupWorkingDirFiles;

    @Autowired
    private HubClient hubClient;

    @Autowired
    private DockerImages dockerImages;

    @Autowired
    private HubDockerManager hubDockerManager;

    @Autowired
    private DockerClientManager dockerClientManager;

    @Autowired
    private ProgramVersion programVersion;

    @Autowired
    private ProgramPaths programPaths;

    @Autowired
    private ResultFile resultFile;

    @Autowired
    private Config config;

    private static int returnCode = -1;
    private static boolean onHostStatic = true;

    public static void main(final String[] args) {
        new SpringApplicationBuilder(Application.class).logStartupInfo(false).run(args);
        if (onHostStatic) {
            logger.info(String.format("Returning %d", returnCode));
            System.exit(returnCode);
        }
    }

    @PostConstruct
    public void inspectImage() {
        try {
            init();
            readConfig();
            final File dockerTarFile = deriveDockerTarFile();
            final List<File> layerTars = hubDockerManager.extractLayerTars(dockerTarFile);
            final List<ManifestLayerMapping> layerMappings = hubDockerManager.getLayerMappings(dockerTarFile.getName(), dockerImageRepo, dockerImageTag);
            fillInMissingImageNameTagFromManifest(layerMappings);
            OperatingSystemEnum targetOsEnum = null;
            if (onHost) {
                targetOsEnum = detectImageOs(layerTars, layerMappings);
                inspectInSubContainer(dockerTarFile, targetOsEnum);
                uploadBdioFiles();
            } else {
                extractAndInspect(dockerTarFile, layerTars, layerMappings);
            }
            provideDockerTarIfRequested(dockerTarFile);
            if (onHost) {
                copyOutputToUserOutputDir();
            }
            returnCode = reportResult();
            if (onHost && cleanupWorkingDirFiles) {
                FileOperations.removeFileOrDirQuietly(programPaths.getHubDockerPgmDirPath());
            }
        } catch (final Throwable e) {
            final String msg = String.format("Error inspecting image: %s", e.getMessage());
            logger.error(msg);
            final String trace = ExceptionUtils.getStackTrace(e);
            logger.debug(String.format("Stack trace: %s", trace));
            resultFile.write(new Gson(), false, msg);
        }
    }

    private void copyOutputToUserOutputDir() throws IOException {
        final String userOutputDirPath = programPaths.getUserOutputDir();
        if (userOutputDirPath == null) {
            logger.debug("User has not specified an output path");
            return;
        }
        logger.debug(String.format("Copying output to %s", userOutputDirPath));
        FileOperations.copyDirContentsToDir(programPaths.getHubDockerOutputPath(), userOutputDirPath, true);
    }

    private void uploadBdioFiles() throws IntegrationException {
        final List<File> bdioFiles = findBdioFiles();
        if (bdioFiles.size() == 0) {
            logger.warn("No BDIO Files generated");
        } else {
            if (dryRun) {
                logger.info("Running in dry run mode; not uploading BDIO to Hub");
            } else {
                logger.info("Uploading BDIO to Hub");
                hubDockerManager.uploadBdioFiles(bdioFiles);
            }
        }
    }

    private OperatingSystemEnum detectImageOs(final List<File> layerTars, final List<ManifestLayerMapping> layerMappings) throws IOException, HubIntegrationException {
        OperatingSystemEnum targetOsEnum;
        targetOsEnum = hubDockerManager.detectOperatingSystem(linuxDistro);
        if (targetOsEnum == null) {
            final File targetImageFileSystemRootDir = hubDockerManager.extractDockerLayers(layerTars, layerMappings);
            targetOsEnum = hubDockerManager.detectOperatingSystem(targetImageFileSystemRootDir);
        }
        return targetOsEnum;
    }

    private void extractAndInspect(final File dockerTarFile, final List<File> layerTars, final List<ManifestLayerMapping> layerMappings)
            throws IOException, HubIntegrationException, InterruptedException, IntegrationException, CompressorException {
        OperatingSystemEnum targetOsEnum;
        final File targetImageFileSystemRootDir = hubDockerManager.extractDockerLayers(layerTars, layerMappings);
        targetOsEnum = hubDockerManager.detectOperatingSystem(targetImageFileSystemRootDir);
        generateBdio(dockerTarFile, targetImageFileSystemRootDir, layerMappings, targetOsEnum);
        createContainerFileSystemTarIfRequested(targetImageFileSystemRootDir);
    }

    private int reportResult() throws HubIntegrationException {
        final Gson gson = new Gson();
        if (onHost) {
            final Result result = resultFile.read(gson);
            if (!result.getSucceeded()) {
                logger.error(String.format("*** Failed: %s", result.getMessage()));
                return -1;
            } else {
                logger.info("*** Succeeded");
                return 0;
            }
        } else {
            resultFile.write(gson, true, "Success");
            return 0;
        }
    }

    private List<File> findBdioFiles() {
        final List<File> bdioFiles = FileOperations.findFilesWithExt(new File(programPaths.getHubDockerOutputPath()), "jsonld");
        logger.info(String.format("Found %d BDIO files produced by the container", bdioFiles.size()));
        return bdioFiles;
    }

    private void clearResult() {
        try {
            final File outputFile = new File(programPaths.getHubDockerResultPath());
            outputFile.delete();
        } catch (final Exception e) {
            logger.warn(String.format("Error clearing result file: %s", e.getMessage()));
        }
    }

    private void provideDockerTarIfRequested(final File dockerTarFile) throws IOException {
        if (outputIncludeDockerTarfile) {
            final File outputDirectory = new File(programPaths.getHubDockerOutputPath());
            if (onHost) {
                logger.debug(String.format("Copying %s to output dir %s", dockerTarFile.getAbsolutePath(), outputDirectory.getAbsolutePath()));
                FileOperations.copyFile(dockerTarFile, outputDirectory);
            } else {
                logger.debug(String.format("Moving %s to output dir %s", dockerTarFile.getAbsolutePath(), outputDirectory.getAbsolutePath()));
                FileOperations.moveFile(dockerTarFile, outputDirectory);
            }
        }
    }

    private void createContainerFileSystemTarIfRequested(final File targetImageFileSystemRootDir) throws IOException, CompressorException {
        if (outputIncludeContainerFileSystemTarfile) {
            final File outputDirectory = new File(programPaths.getHubDockerOutputPath());
            final String containerFileSystemTarFilename = programPaths.getContainerFileSystemTarFilename(dockerImageRepo, dockerImageTag);
            final File containerFileSystemTarFile = new File(outputDirectory, containerFileSystemTarFilename);
            logger.debug(String.format("Creating container filesystem tarfile %s from %s into %s", containerFileSystemTarFile.getAbsolutePath(), targetImageFileSystemRootDir.getAbsolutePath(), outputDirectory.getAbsolutePath()));
            final FileSys containerFileSys = new FileSys(targetImageFileSystemRootDir);
            containerFileSys.createTarGz(containerFileSystemTarFile);
        }
    }

    private void inspectInSubContainer(final File dockerTarFile, final OperatingSystemEnum targetOsEnum) throws InterruptedException, IOException, HubIntegrationException {
        final String runOnImageName = dockerImages.getDockerImageName(targetOsEnum);
        final String runOnImageVersion = dockerImages.getDockerImageVersion(targetOsEnum);
        final String msg = String.format("Image inspection for %s will use docker image %s:%s", targetOsEnum.toString(), runOnImageName, runOnImageVersion);
        logger.info(msg);
        try {
            dockerClientManager.pullImage(runOnImageName, runOnImageVersion);
        } catch (final Exception e) {
            logger.warn(String.format("Unable to pull docker image %s:%s; proceeding anyway since it may already exist locally", runOnImageName, runOnImageVersion));
        }
        logger.debug(String.format("runInSubContainer(): Running subcontainer on image %s, repo %s, tag %s", dockerImage, dockerImageRepo, dockerImageTag));
        dockerClientManager.run(runOnImageName, runOnImageVersion, dockerTarFile, true, dockerImage, dockerImageRepo, dockerImageTag);
    }

    // Runs only in container
    private void generateBdio(final File dockerTarFile, final File targetImageFileSystemRootDir, final List<ManifestLayerMapping> layerMappings, final OperatingSystemEnum targetOsEnum)
            throws IOException, InterruptedException, IntegrationException {
        final String msg = String.format("Target image tarfile: %s; target OS: %s", dockerTarFile.getAbsolutePath(), targetOsEnum.toString());
        logger.info(msg);
        final List<File> bdioFiles = hubDockerManager.generateBdioFromImageFilesDir(dockerImageRepo, dockerImageTag, layerMappings, getHubProjectName(), getHubProjectVersion(), dockerTarFile, targetImageFileSystemRootDir, targetOsEnum);
        logger.info(String.format("%d BDIO Files generated", bdioFiles.size()));
    }

    private String getHubProjectName() {
        return programPaths.unEscape(hubProjectName);
    }

    private String getHubProjectVersion() {
        return programPaths.unEscape(hubVersionName);
    }

    private void init() throws IOException, IntegrationException {
        logger.info(String.format("hub-docker-inspector %s", programVersion.getProgramVersion()));
        logger.debug(String.format("running from dir: %s", System.getProperty("user.dir")));
        logger.debug(String.format("Dry run mode is set to %b", dryRun));
        logger.trace(String.format("dockerImageTag: %s", dockerImageTag));
        onHostStatic = onHost;
        if (onHost) {
            hubDockerManager.phoneHome();
        }
        clearResult();
        initImageName();
        logger.info(String.format("Inspecting image:tag %s:%s", dockerImageRepo, dockerImageTag));
        if (!dryRun) {
            verifyHubConnection();
        }
        hubDockerManager.init();
        FileOperations.removeFileOrDir(programPaths.getHubDockerWorkingDirPath());
    }

    private void readConfig() throws IllegalArgumentException, IllegalAccessException, IOException {

        final List<DockerInspectorOption> configOptions = config.getConfigOptions();
        for (final DockerInspectorOption opt : configOptions) {
            logger.info(String.format("\t--%s: type: %s; default: %s; description: %s; value: %s", opt.getKey(), opt.getValueTypeString(), opt.getDefaultValue(), opt.getDescription(), opt.getResolvedValue()));
        }
    }

    private void verifyHubConnection() throws HubIntegrationException {
        hubClient.testHubConnection();
        logger.info("Your Hub configuration is valid and a successful connection to the Hub was established.");
        return;
    }

    private void initImageName() throws HubIntegrationException {
        logger.debug(String.format("initImageName(): dockerImage: %s, dockerTar: %s", dockerImage, dockerTar));
        if (StringUtils.isNotBlank(dockerImage)) {
            final String[] imageNameAndTag = dockerImage.split(":");
            if ((imageNameAndTag.length > 0) && (StringUtils.isNotBlank(imageNameAndTag[0]))) {
                dockerImageRepo = imageNameAndTag[0];
            }
            if ((imageNameAndTag.length > 1) && (StringUtils.isNotBlank(imageNameAndTag[1]))) {
                dockerImageTag = imageNameAndTag[1];
            } else {
                dockerImageTag = "latest";
            }
        }
        logger.debug(String.format("initImageName(): final: dockerImage: %s; dockerImageRepo: %s; dockerImageTag: %s", dockerImage, dockerImageRepo, dockerImageTag));
    }

    private void fillInMissingImageNameTagFromManifest(final List<ManifestLayerMapping> layerMappings) {
        if ((layerMappings != null) && (layerMappings.size() == 1)) {
            if (StringUtils.isBlank(dockerImageRepo)) {
                dockerImageRepo = layerMappings.get(0).getImageName();
            }
            if (StringUtils.isBlank(dockerImageTag)) {
                dockerImageTag = layerMappings.get(0).getTagName();
            }
        }
        logger.debug(String.format("fillInMissingImageNameTagFromManifest(): final: dockerImage: %s; dockerImageRepo: %s; dockerImageTag: %s", dockerImage, dockerImageRepo, dockerImageTag));
    }

    private File deriveDockerTarFile() throws IOException, HubIntegrationException {
        File dockerTarFile = null;
        if (StringUtils.isNotBlank(dockerTar)) {
            dockerTarFile = new File(dockerTar);
        } else if (StringUtils.isNotBlank(dockerImageId)) {
            dockerTarFile = hubDockerManager.getTarFileFromDockerImageById(dockerImageId);
        } else if (StringUtils.isNotBlank(dockerImageRepo)) {
            dockerTarFile = hubDockerManager.getTarFileFromDockerImage(dockerImageRepo, dockerImageTag);
        }
        return dockerTarFile;
    }
}
