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
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.docker.client.DockerClientManager;
import com.blackducksoftware.integration.hub.docker.client.ProgramVersion;
import com.blackducksoftware.integration.hub.docker.config.Config;
import com.blackducksoftware.integration.hub.docker.config.formatter.UsageFormatter;
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

    public static final String PROGRAM_NAME = "hub-docker-inspector.sh"; // TODO unhardcode

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
    ApplicationArguments applicationArguments;

    @Autowired
    private Config config;

    @Autowired
    private UsageFormatter usageFormatter;

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
            if (!initAndValidate()) {
                return;
            }
            final File dockerTarFile = deriveDockerTarFile();
            final List<File> layerTars = hubDockerManager.extractLayerTars(dockerTarFile);
            final List<ManifestLayerMapping> layerMappings = hubDockerManager.getLayerMappings(dockerTarFile.getName(), config.getDockerImageRepo(), config.getDockerImageTag());
            fillInMissingImageNameTagFromManifest(layerMappings);
            OperatingSystemEnum targetOsEnum = null;
            if (config.isOnHost()) {
                targetOsEnum = detectImageOs(layerTars, layerMappings);
                inspectInSubContainer(dockerTarFile, targetOsEnum);
                uploadBdioFiles();
            } else {
                extractAndInspect(dockerTarFile, layerTars, layerMappings);
            }
            provideDockerTarIfRequested(dockerTarFile);
            if (config.isOnHost()) {
                copyOutputToUserOutputDir();
            }
            returnCode = reportResult();
            if (config.isOnHost() && config.isCleanupWorkingDir()) {
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

    private boolean helpInvoked() {
        logger.debug("Checking to see if help argument passed");
        if (applicationArguments == null) {
            logger.debug("applicationArguments is null");
            return false;
        }
        final String[] args = applicationArguments.getSourceArgs();
        if (contains(args, "-h") || (contains(args, "--help"))) {
            logger.debug("Help argument passed");
            return true;
        }
        return false;
    }

    private boolean contains(final String[] stringsToSearch, final String targetString) {
        for (final String stringToTest : stringsToSearch) {
            if (targetString.equals(stringToTest)) {
                return true;
            }
        }
        return false;
    }

    private void showUsage() throws IllegalArgumentException, IllegalAccessException, IOException {
        final List<String> usage = usageFormatter.getStringList();
        System.out.println("----------");
        for (final String line : usage) {
            System.out.println(line);
        }
        System.out.println("----------");
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
            if (config.isDryRun()) {
                logger.info("Running in dry run mode; not uploading BDIO to Hub");
            } else {
                logger.info("Uploading BDIO to Hub");
                hubDockerManager.uploadBdioFiles(bdioFiles);
            }
        }
    }

    private OperatingSystemEnum detectImageOs(final List<File> layerTars, final List<ManifestLayerMapping> layerMappings) throws IOException, HubIntegrationException {
        OperatingSystemEnum targetOsEnum;
        targetOsEnum = hubDockerManager.detectOperatingSystem(config.getLinuxDistro());
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
        if (config.isOnHost()) {
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
        if (config.isOutputIncludeDockertarfile()) {
            final File outputDirectory = new File(programPaths.getHubDockerOutputPath());
            if (config.isOnHost()) {
                logger.debug(String.format("Copying %s to output dir %s", dockerTarFile.getAbsolutePath(), outputDirectory.getAbsolutePath()));
                FileOperations.copyFile(dockerTarFile, outputDirectory);
            } else {
                logger.debug(String.format("Moving %s to output dir %s", dockerTarFile.getAbsolutePath(), outputDirectory.getAbsolutePath()));
                FileOperations.moveFile(dockerTarFile, outputDirectory);
            }
        }
    }

    private void createContainerFileSystemTarIfRequested(final File targetImageFileSystemRootDir) throws IOException, CompressorException {
        if (config.isOutputIncludeContainerfilesystem()) {
            final File outputDirectory = new File(programPaths.getHubDockerOutputPath());
            final String containerFileSystemTarFilename = programPaths.getContainerFileSystemTarFilename(config.getDockerImageRepo(), config.getDockerImageTag());
            final File containerFileSystemTarFile = new File(outputDirectory, containerFileSystemTarFilename);
            logger.debug(String.format("Creating container filesystem tarfile %s from %s into %s", containerFileSystemTarFile.getAbsolutePath(), targetImageFileSystemRootDir.getAbsolutePath(), outputDirectory.getAbsolutePath()));
            final FileSys containerFileSys = new FileSys(targetImageFileSystemRootDir);
            containerFileSys.createTarGz(containerFileSystemTarFile);
        }
    }

    private void inspectInSubContainer(final File dockerTarFile, final OperatingSystemEnum targetOsEnum) throws InterruptedException, IOException, HubIntegrationException, IllegalArgumentException, IllegalAccessException {
        final String runOnImageName = dockerImages.getDockerImageName(targetOsEnum);
        final String runOnImageVersion = dockerImages.getDockerImageVersion(targetOsEnum);
        final String msg = String.format("Image inspection for %s will use docker image %s:%s", targetOsEnum.toString(), runOnImageName, runOnImageVersion);
        logger.info(msg);
        try {
            dockerClientManager.pullImage(runOnImageName, runOnImageVersion);
        } catch (final Exception e) {
            logger.warn(String.format("Unable to pull docker image %s:%s; proceeding anyway since it may already exist locally", runOnImageName, runOnImageVersion));
        }
        logger.debug(String.format("runInSubContainer(): Running subcontainer on image %s, repo %s, tag %s", config.getDockerImage(), config.getDockerImageRepo(), config.getDockerImageTag()));
        dockerClientManager.run(runOnImageName, runOnImageVersion, dockerTarFile, true, config.getDockerImage(), config.getDockerImageRepo(), config.getDockerImageTag());
    }

    // Runs only in container
    private void generateBdio(final File dockerTarFile, final File targetImageFileSystemRootDir, final List<ManifestLayerMapping> layerMappings, final OperatingSystemEnum targetOsEnum)
            throws IOException, InterruptedException, IntegrationException {
        final String msg = String.format("Target image tarfile: %s; target OS: %s", dockerTarFile.getAbsolutePath(), targetOsEnum.toString());
        logger.info(msg);
        final List<File> bdioFiles = hubDockerManager.generateBdioFromImageFilesDir(config.getDockerImageRepo(), config.getDockerImageTag(), layerMappings, getHubProjectName(), getHubProjectVersion(), dockerTarFile,
                targetImageFileSystemRootDir, targetOsEnum);
        logger.info(String.format("%d BDIO Files generated", bdioFiles.size()));
    }

    private String getHubProjectName() {
        return programPaths.unEscape(config.getHubProjectName());
    }

    private String getHubProjectVersion() {
        return programPaths.unEscape(config.getHubProjectVersion());
    }

    private boolean initAndValidate() throws IOException, IntegrationException, IllegalArgumentException, IllegalAccessException {
        logger.info(String.format("hub-docker-inspector %s", programVersion.getProgramVersion()));
        if (helpInvoked()) {
            showUsage();
            returnCode = 0;
            return false;
        }
        logger.debug(String.format("running from dir: %s", System.getProperty("user.dir")));
        logger.debug(String.format("Dry run mode is set to %b", config.isDryRun()));
        logger.trace(String.format("dockerImageTag: %s", config.getDockerImageTag()));
        onHostStatic = config.isOnHost();
        if (config.isOnHost()) {
            hubDockerManager.phoneHome();
        }
        clearResult();
        initImageName();
        logger.info(String.format("Inspecting image:tag %s:%s", config.getDockerImageRepo(), config.getDockerImageTag()));
        if (!config.isDryRun()) {
            verifyHubConnection();
        }
        hubDockerManager.init();
        FileOperations.removeFileOrDir(programPaths.getHubDockerWorkingDirPath());
        return true;
    }

    private void verifyHubConnection() throws HubIntegrationException {
        hubClient.testHubConnection();
        logger.info("Your Hub configuration is valid and a successful connection to the Hub was established.");
        return;
    }

    private void initImageName() throws HubIntegrationException {
        logger.debug(String.format("initImageName(): dockerImage: %s, dockerTar: %s", config.getDockerImage(), config.getDockerTar()));
        if (StringUtils.isNotBlank(config.getDockerImage())) {
            final String[] imageNameAndTag = config.getDockerImage().split(":");
            if ((imageNameAndTag.length > 0) && (StringUtils.isNotBlank(imageNameAndTag[0]))) {
                config.setDockerImageRepo(imageNameAndTag[0]);
            }
            if ((imageNameAndTag.length > 1) && (StringUtils.isNotBlank(imageNameAndTag[1]))) {
                config.setDockerImageTag(imageNameAndTag[1]);
            } else {
                config.setDockerImageTag("latest");
            }
        }
        logger.debug(String.format("initImageName(): final: dockerImage: %s; dockerImageRepo: %s; dockerImageTag: %s", config.getDockerImage(), config.getDockerImageRepo(), config.getDockerImageTag()));
    }

    private void fillInMissingImageNameTagFromManifest(final List<ManifestLayerMapping> layerMappings) {
        if ((layerMappings != null) && (layerMappings.size() == 1)) {
            if (StringUtils.isBlank(config.getDockerImageRepo())) {
                config.setDockerImageRepo(layerMappings.get(0).getImageName());
            }
            if (StringUtils.isBlank(config.getDockerImageTag())) {
                config.setDockerImageTag(layerMappings.get(0).getTagName());
            }
        }
        logger.debug(String.format("fillInMissingImageNameTagFromManifest(): final: dockerImage: %s; dockerImageRepo: %s; dockerImageTag: %s", config.getDockerImage(), config.getDockerImageRepo(), config.getDockerImageTag()));
    }

    private File deriveDockerTarFile() throws IOException, HubIntegrationException {
        File dockerTarFile = null;
        if (StringUtils.isNotBlank(config.getDockerTar())) {
            dockerTarFile = new File(config.getDockerTar());
        } else if (StringUtils.isNotBlank(config.getDockerImageId())) {
            dockerTarFile = hubDockerManager.getTarFileFromDockerImageById(config.getDockerImageId());
        } else if (StringUtils.isNotBlank(config.getDockerImageRepo())) {
            dockerTarFile = hubDockerManager.getTarFileFromDockerImage(config.getDockerImageRepo(), config.getDockerImageTag());
        }
        return dockerTarFile;
    }
}
