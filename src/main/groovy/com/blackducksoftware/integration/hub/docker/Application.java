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
        String runOnImageName = null;
        String runOnImageTag = null;
        File dockerTarFile = null;
        String bdioFilename = null;
        try {
            if (!initAndValidate()) {
                return;
            }
            List<File> layerTars = null;
            List<ManifestLayerMapping> layerMappings = null;
            if (config.isIdentifyPkgMgr() || config.isInspect()) {
                dockerTarFile = deriveDockerTarFile();
                layerTars = hubDockerManager.extractLayerTars(dockerTarFile);
                layerMappings = hubDockerManager.getLayerMappings(dockerTarFile.getName(), config.getDockerImageRepo(), config.getDockerImageTag());
                fillInMissingImageNameTagFromManifest(layerMappings);
            }
            OperatingSystemEnum targetOsEnum = null;
            File targetImageFileSystemRootDir = null;
            if (config.isIdentifyPkgMgr()) {
                targetOsEnum = hubDockerManager.detectOperatingSystem(config.getLinuxDistro());
                if (targetOsEnum == null) {
                    targetImageFileSystemRootDir = hubDockerManager.extractDockerLayers(layerTars, layerMappings);
                    targetOsEnum = hubDockerManager.detectOperatingSystem(targetImageFileSystemRootDir);
                }
                runOnImageName = dockerImages.getDockerImageName(targetOsEnum);
                runOnImageTag = dockerImages.getDockerImageVersion(targetOsEnum);
                logger.info(String.format("Identified target OS: %s; corresponding inspection image: %s:%s", targetOsEnum.name(), runOnImageName, runOnImageTag));
                if (StringUtils.isBlank(runOnImageName) || StringUtils.isBlank(runOnImageTag)) {
                    throw new HubIntegrationException("Failed to identify inspection image name and/or tag");
                }
            }
            if (config.isInspect()) {
                if (targetImageFileSystemRootDir == null) {
                    targetImageFileSystemRootDir = hubDockerManager.extractDockerLayers(layerTars, layerMappings);
                }
                if (targetOsEnum == null) {
                    targetOsEnum = hubDockerManager.detectOperatingSystem(targetImageFileSystemRootDir);
                }
                logger.info(String.format("Target image tarfile: %s; target OS: %s", dockerTarFile.getAbsolutePath(), targetOsEnum.toString()));
                final List<File> bdioFiles = hubDockerManager.generateBdioFromImageFilesDir(config.getDockerImageRepo(), config.getDockerImageTag(), layerMappings, getHubProjectName(), getHubProjectVersion(), dockerTarFile,
                        targetImageFileSystemRootDir, targetOsEnum);
                logger.info(String.format("%d BDIO Files generated", bdioFiles.size()));
                bdioFilename = bdioFiles.size() == 1 ? bdioFiles.get(0).getName() : null;
                createContainerFileSystemTarIfRequested(targetImageFileSystemRootDir);
            } else if (config.isInspectInContainer()) {
                logger.info("Inspecting image in container");
                inspectInSubContainer(dockerTarFile, targetOsEnum, runOnImageName, runOnImageTag);
            }
            if (config.isUploadBdio()) {
                logger.info("Uploading BDIO to Hub");
                bdioFilename = uploadBdioFiles();
            }
            provideDockerTarIfRequested(dockerTarFile);
            if (config.isOnHost() && (config.isInspect() || config.isInspectInContainer())) {
                copyOutputToUserOutputDir();
            }
            returnCode = reportResult(runOnImageName, runOnImageTag, dockerTarFile == null ? "" : dockerTarFile.getName(), bdioFilename);
            if (config.isOnHost()) {
                copyResultToUserOutputDir();
            }
            if (config.isOnHost() && config.isInspect() && config.isCleanupWorkingDir()) {
                cleanupWorkingDirs();
            }
        } catch (final Throwable e) {
            final String msg = String.format("Error inspecting image: %s", e.getMessage());
            logger.error(msg);
            final String trace = ExceptionUtils.getStackTrace(e);
            logger.debug(String.format("Stack trace: %s", trace));
            resultFile.write(new Gson(), false, msg, runOnImageName, runOnImageTag, dockerTarFile == null ? "" : dockerTarFile.getName(), bdioFilename);
        }
    }

    private void cleanupWorkingDirs() throws IOException {
        logger.debug(String.format("Removing %s, %s, %s", programPaths.getHubDockerWorkingDirPathHost(), programPaths.getHubDockerTargetDirPathHost(), programPaths.getHubDockerOutputPathHost()));
        FileOperations.removeFileOrDir(programPaths.getHubDockerWorkingDirPathHost());
        FileOperations.removeFileOrDir(programPaths.getHubDockerTargetDirPathHost());
        FileOperations.removeFileOrDir(programPaths.getHubDockerOutputPathHost());
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
        final File srcDir = new File(programPaths.getHubDockerOutputPathHost());
        if (!srcDir.exists()) {
            logger.info(String.format("Output source dir %s does not exist", srcDir.getAbsolutePath()));
            return;
        }
        logger.info(String.format("Copying output from %s to %s", programPaths.getHubDockerOutputPathHost(), userOutputDirPath));
        final File userOutputDir = new File(userOutputDirPath);
        FileOperations.copyDirContentsToDir(programPaths.getHubDockerOutputPathHost(), userOutputDir.getAbsolutePath(), true);
    }

    private void copyResultToUserOutputDir() throws IOException {
        final String userOutputDirPath = programPaths.getUserOutputDir();
        if (userOutputDirPath == null) {
            logger.debug("User has not specified an output path");
            return;
        }
        logger.debug(String.format("Copying result file from %s to %s", programPaths.getHubDockerResultPathHost(), userOutputDirPath));
        final File sourceResultFile = new File(programPaths.getHubDockerResultPathHost());
        final File userOutputDir = new File(userOutputDirPath);
        final File targetFile = new File(userOutputDir, sourceResultFile.getName());
        logger.debug(String.format("Removing %s if it exists", targetFile.getAbsolutePath()));
        FileOperations.removeFileOrDirQuietly(targetFile.getAbsolutePath());
        FileOperations.copyFile(new File(programPaths.getHubDockerResultPathHost()), userOutputDir);
    }

    private String uploadBdioFiles() throws IntegrationException {
        String pathToDirContainingBdio = null;
        if (StringUtils.isBlank(config.getBdioPath())) {
            pathToDirContainingBdio = programPaths.getHubDockerOutputPath();
        } else {
            pathToDirContainingBdio = config.getBdioPath();
        }
        logger.debug(String.format("Uploading BDIO files from %s", pathToDirContainingBdio));
        String bdioFilename = null;
        final List<File> bdioFiles = findBdioFiles(pathToDirContainingBdio);
        if (bdioFiles.size() == 0) {
            logger.warn("No BDIO Files generated");
        } else if (bdioFiles.size() > 1) {
            throw new HubIntegrationException(String.format("Found %d BDIO files in %s", bdioFiles.size(), pathToDirContainingBdio));
        } else {
            bdioFilename = bdioFiles.get(0).getName();
            logger.info(String.format("Uploading BDIO to Hub: %d files; first file: %s", bdioFiles.size(), bdioFiles.get(0).getAbsolutePath()));
            hubDockerManager.uploadBdioFiles(bdioFiles);
        }
        return bdioFilename;
    }

    private int reportResult(final String runOnImageName, final String runOnImageTag, final String dockerTarfilename, final String bdioFilename) throws HubIntegrationException {
        final Gson gson = new Gson();
        if (config.isOnHost() && config.isInspectInContainer()) {
            final Result resultReportedFromContainer = resultFile.read(gson);
            if (!resultReportedFromContainer.isSucceeded()) {
                logger.error(String.format("*** Failed: %s", resultReportedFromContainer.getMessage()));
                return -1;
            } else {
                logger.info("*** Succeeded");
                return 0;
            }
        } else {
            resultFile.write(gson, true, "Success", runOnImageName, runOnImageTag, dockerTarfilename, bdioFilename);
            return 0;
        }
    }

    private List<File> findBdioFiles(final String pathToDirContainingBdio) {
        final List<File> bdioFiles = FileOperations.findFilesWithExt(new File(pathToDirContainingBdio), "jsonld");
        logger.info(String.format("Found %d BDIO files in %s", bdioFiles.size(), pathToDirContainingBdio));
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
            if (config.isOnHost()) {
                final File outputDirectory = new File(programPaths.getHubDockerOutputPathHost());
                logger.debug(String.format("Copying %s to output dir %s", dockerTarFile.getAbsolutePath(), outputDirectory.getAbsolutePath()));
                FileOperations.copyFile(dockerTarFile, outputDirectory);
            } else {
                final File outputDirectory = new File(programPaths.getHubDockerOutputPathContainer());
                logger.debug(String.format("Moving %s to output dir %s", dockerTarFile.getAbsolutePath(), outputDirectory.getAbsolutePath()));
                FileOperations.moveFile(dockerTarFile, outputDirectory);
            }
        }
    }

    private void createContainerFileSystemTarIfRequested(final File targetImageFileSystemRootDir) throws IOException, CompressorException {
        if (config.isOutputIncludeContainerfilesystem()) {
            final File outputDirectory = new File(programPaths.getHubDockerOutputPathContainer());
            final String containerFileSystemTarFilename = programPaths.getContainerFileSystemTarFilename(config.getDockerImageRepo(), config.getDockerImageTag());
            final File containerFileSystemTarFile = new File(outputDirectory, containerFileSystemTarFilename);
            logger.debug(String.format("Creating container filesystem tarfile %s from %s into %s", containerFileSystemTarFile.getAbsolutePath(), targetImageFileSystemRootDir.getAbsolutePath(), outputDirectory.getAbsolutePath()));
            final FileSys containerFileSys = new FileSys(targetImageFileSystemRootDir);
            containerFileSys.createTarGz(containerFileSystemTarFile);
        }
    }

    private void inspectInSubContainer(final File dockerTarFile, final OperatingSystemEnum targetOsEnum, final String runOnImageName, final String runOnImageTag)
            throws InterruptedException, IOException, HubIntegrationException, IllegalArgumentException, IllegalAccessException {

        final String msg = String.format("Image inspection for %s will use docker image %s:%s", targetOsEnum.toString(), runOnImageName, runOnImageTag);
        logger.info(msg);
        try {
            dockerClientManager.pullImage(runOnImageName, runOnImageTag);
        } catch (final Exception e) {
            logger.warn(String.format("Unable to pull docker image %s:%s; proceeding anyway since it may already exist locally", runOnImageName, runOnImageTag));
        }
        logger.debug(String.format("runInSubContainer(): Running subcontainer on image %s, repo %s, tag %s", config.getDockerImage(), config.getDockerImageRepo(), config.getDockerImageTag()));
        dockerClientManager.run(runOnImageName, runOnImageTag, dockerTarFile, true, config.getDockerImage(), config.getDockerImageRepo(), config.getDockerImageTag());
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
        if (config.isDryRun()) {
            logger.warn("dry.run is deprecated. Set upload.bdio=false instead");
            config.setUploadBdio(false);
        }
        onHostStatic = config.isOnHost();
        if (config.isOnHost()) {
            hubDockerManager.phoneHome();
        }
        clearResult();
        initImageName();
        logger.info(String.format("Inspecting image:tag %s:%s", config.getDockerImageRepo(), config.getDockerImageTag()));
        if (config.isOnHost()) {
            verifyHubConnection();
        }
        hubDockerManager.init();
        FileOperations.removeFileOrDir(programPaths.getHubDockerWorkingDirPath());

        logger.debug(String.format("Upload BDIO is set to %b", config.isUploadBdio()));
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
