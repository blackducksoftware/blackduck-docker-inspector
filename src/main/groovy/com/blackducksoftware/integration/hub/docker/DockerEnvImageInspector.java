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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
import com.blackducksoftware.integration.hub.docker.dockerclient.DockerClientManager;
import com.blackducksoftware.integration.hub.docker.help.formatter.UsageFormatter;
import com.blackducksoftware.integration.hub.docker.hubclient.HubClient;
import com.blackducksoftware.integration.hub.docker.imageinspector.DissectedImage;
import com.blackducksoftware.integration.hub.docker.imageinspector.ImageInspector;
import com.blackducksoftware.integration.hub.docker.imageinspector.OperatingSystemEnum;
import com.blackducksoftware.integration.hub.docker.imageinspector.config.Config;
import com.blackducksoftware.integration.hub.docker.imageinspector.config.ProgramPaths;
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.FileOperations;
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.FileSys;
import com.blackducksoftware.integration.hub.docker.imageinspector.result.Result;
import com.blackducksoftware.integration.hub.docker.imageinspector.result.ResultFile;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.google.gson.Gson;

@SpringBootApplication
public class DockerEnvImageInspector {
    private static final Logger logger = LoggerFactory.getLogger(DockerEnvImageInspector.class);

    public static final String PROGRAM_NAME = "hub-docker-inspector.sh"; // TODO unhardcode

    @Autowired
    private HubClient hubClient;

    @Autowired
    private InspectorImages dockerImages;

    @Autowired
    private ImageInspector imageInspector;

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
        new SpringApplicationBuilder(DockerEnvImageInspector.class).logStartupInfo(false).run(args);
        if (onHostStatic) {
            logger.info(String.format("Returning %d", returnCode));
            System.exit(returnCode);
        }
    }

    @PostConstruct
    public void inspectImage() {
        final DissectedImage dissectedImage = new DissectedImage();
        try {
            if (!initAndValidate(config)) {
                return;
            }
            parseManifest(config, dissectedImage);
            extractLayers(config, dissectedImage);
            final Future<String> deferredCleanup = inspect(config, dissectedImage);
            uploadBdio(config, dissectedImage);
            provideDockerTar(config, dissectedImage.getDockerTarFile());
            provideOutput(config);
            reportResults(config, dissectedImage);
            cleanUp(config, deferredCleanup);
        } catch (final Throwable e) {
            final String msg = String.format("Error inspecting image: %s", e.getMessage());
            logger.error(msg);
            final String trace = ExceptionUtils.getStackTrace(e);
            logger.debug(String.format("Stack trace: %s", trace));
            resultFile.write(new Gson(), false, msg, dissectedImage.getTargetOs(), dissectedImage.getRunOnImageName(), dissectedImage.getRunOnImageTag(),
                    dissectedImage.getDockerTarFile() == null ? "" : dissectedImage.getDockerTarFile().getName(), dissectedImage.getBdioFilename());
        }
    }

    private void cleanUp(final Config config, final Future<String> deferredCleanup) throws IOException {
        if (config.isOnHost() && config.isInspect() && config.isCleanupWorkingDir()) {
            cleanupWorkingDirs();
        }
        if (deferredCleanup != null) {
            try {
                logger.debug("Waiting for completion of concurrent inspector container/image cleanup");
                logger.info(String.format("Status from concurrent cleanup: %s", deferredCleanup.get(15, TimeUnit.SECONDS)));
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                logger.error(String.format("Error during concurrent cleanup: %s", e.getMessage()));
            }
        }
    }

    private void reportResults(final Config config, final DissectedImage dissectedImage) throws HubIntegrationException, IOException {
        returnCode = reportResult(config, dissectedImage.getTargetOs(), dissectedImage.getRunOnImageName(), dissectedImage.getRunOnImageTag(), dissectedImage.getDockerTarFile() == null ? "" : dissectedImage.getDockerTarFile().getName(),
                dissectedImage.getBdioFilename());
        if (config.isOnHost()) {
            copyResultToUserOutputDir();
        }
    }

    private void provideOutput(final Config config) throws IOException {
        if (config.isOnHost() && (config.isInspect() || config.isInspectInContainer())) {
            copyOutputToUserOutputDir();
        }
    }

    private void uploadBdio(final Config config, final DissectedImage dissectedImage) throws IntegrationException {
        if (config.isUploadBdio()) {
            logger.info("Uploading BDIO to Hub");
            dissectedImage.setBdioFilename(uploadBdioFiles(config));
        }
    }

    private Future<String> inspect(final Config config, final DissectedImage dissectedImage) throws IOException, HubIntegrationException, InterruptedException, CompressorException, IllegalAccessException {
        Future<String> deferredCleanup = null;
        if (config.isInspect()) {
            if (dissectedImage.getTargetImageFileSystemRootDir() == null) {
                dissectedImage.setTargetImageFileSystemRootDir(imageInspector.extractDockerLayers(dissectedImage.getLayerTars(), dissectedImage.getLayerMappings()));
            }
            if (dissectedImage.getTargetOs() == null) {
                dissectedImage.setTargetOs(imageInspector.detectOperatingSystem(dissectedImage.getTargetImageFileSystemRootDir()));
            }
            logger.info(String.format("Target image tarfile: %s; target OS: %s", dissectedImage.getDockerTarFile().getAbsolutePath(), dissectedImage.getTargetOs().toString()));
            final File bdioFile = imageInspector.generateBdioFromImageFilesDir(config.getDockerImageRepo(), config.getDockerImageTag(), dissectedImage.getLayerMappings(), getHubProjectName(config), getHubProjectVersion(config),
                    dissectedImage.getDockerTarFile(), dissectedImage.getTargetImageFileSystemRootDir(), dissectedImage.getTargetOs());
            logger.info(String.format("BDIO File generated: %s", bdioFile.getAbsolutePath()));
            dissectedImage.setBdioFilename(bdioFile.getName());
            createContainerFileSystemTarIfRequested(config, dissectedImage.getTargetImageFileSystemRootDir());
        } else if (config.isInspectInContainer()) {
            logger.info("Inspecting image in container");
            deferredCleanup = inspectInSubContainer(config, dissectedImage.getDockerTarFile(), dissectedImage.getTargetOs(), dissectedImage.getRunOnImageName(), dissectedImage.getRunOnImageTag());
        }
        return deferredCleanup;
    }

    private void extractLayers(final Config config, final DissectedImage dissectedImage) throws IOException, HubIntegrationException {
        if (config.isIdentifyPkgMgr()) {
            dissectedImage.setTargetOs(imageInspector.detectOperatingSystem(config.getLinuxDistro()));
            if (dissectedImage.getTargetOs() == null) {
                dissectedImage.setTargetImageFileSystemRootDir(imageInspector.extractDockerLayers(dissectedImage.getLayerTars(), dissectedImage.getLayerMappings()));
                dissectedImage.setTargetOs(imageInspector.detectOperatingSystem(dissectedImage.getTargetImageFileSystemRootDir()));
            }
            dissectedImage.setRunOnImageName(dockerImages.getInspectorImageName(dissectedImage.getTargetOs()));
            dissectedImage.setRunOnImageTag(dockerImages.getInspectorImageTag(dissectedImage.getTargetOs()));
            logger.info(String.format("Identified target OS: %s; corresponding inspection image: %s:%s", dissectedImage.getTargetOs().name(), dissectedImage.getRunOnImageName(), dissectedImage.getRunOnImageTag()));
            if (StringUtils.isBlank(dissectedImage.getRunOnImageName()) || StringUtils.isBlank(dissectedImage.getRunOnImageTag())) {
                throw new HubIntegrationException("Failed to identify inspection image name and/or tag");
            }
        }
    }

    private void parseManifest(final Config config, final DissectedImage dissectedImage) throws IOException, HubIntegrationException, Exception {
        if (config.isIdentifyPkgMgr() || config.isInspect()) {
            dissectedImage.setDockerTarFile(deriveDockerTarFile(config));
            dissectedImage.setLayerTars(imageInspector.extractLayerTars(dissectedImage.getDockerTarFile()));
            dissectedImage.setLayerMappings(imageInspector.getLayerMappings(dissectedImage.getDockerTarFile().getName(), config.getDockerImageRepo(), config.getDockerImageTag()));
            imageInspector.adjustImageNameTagFromLayerMappings(dissectedImage.getLayerMappings());
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

    // TODO move this?
    private String uploadBdioFiles(final Config config) throws IntegrationException {
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
            imageInspector.uploadBdioFiles(bdioFiles);
        }
        return bdioFilename;
    }

    private int reportResult(final Config config, final OperatingSystemEnum targetOs, final String runOnImageName, final String runOnImageTag, final String dockerTarfilename, final String bdioFilename) throws HubIntegrationException {
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
            resultFile.write(gson, true, "Success", targetOs, runOnImageName, runOnImageTag, dockerTarfilename, bdioFilename);
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

    private void provideDockerTar(final Config config, final File dockerTarFile) throws IOException {
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

    private void createContainerFileSystemTarIfRequested(final Config config, final File targetImageFileSystemRootDir) throws IOException, CompressorException {
        if (config.isOutputIncludeContainerfilesystem()) {
            final File outputDirectory = new File(programPaths.getHubDockerOutputPathContainer());
            final String containerFileSystemTarFilename = programPaths.getContainerFileSystemTarFilename(config.getDockerImageRepo(), config.getDockerImageTag());
            final File containerFileSystemTarFile = new File(outputDirectory, containerFileSystemTarFilename);
            logger.debug(String.format("Creating container filesystem tarfile %s from %s into %s", containerFileSystemTarFile.getAbsolutePath(), targetImageFileSystemRootDir.getAbsolutePath(), outputDirectory.getAbsolutePath()));
            final FileSys containerFileSys = new FileSys(targetImageFileSystemRootDir);
            containerFileSys.createTarGz(containerFileSystemTarFile);
        }
    }

    private Future<String> inspectInSubContainer(final Config config, final File dockerTarFile, final OperatingSystemEnum targetOs, final String runOnImageName, final String runOnImageTag)
            throws InterruptedException, IOException, HubIntegrationException, IllegalArgumentException, IllegalAccessException {
        final String msg = String.format("Image inspection for %s will use docker image %s:%s", targetOs.toString(), runOnImageName, runOnImageTag);
        logger.info(msg);
        String runOnImageId = null;
        try {
            runOnImageId = dockerClientManager.pullImage(runOnImageName, runOnImageTag);
        } catch (final Exception e) {
            logger.warn(String.format("Unable to pull docker image %s:%s; proceeding anyway since it may already exist locally", runOnImageName, runOnImageTag));
        }
        logger.debug(String.format("runInSubContainer(): Running subcontainer on image %s, repo %s, tag %s", config.getDockerImage(), config.getDockerImageRepo(), config.getDockerImageTag()));
        final String containerId = dockerClientManager.run(runOnImageName, runOnImageTag, runOnImageId, dockerTarFile, true, config.getDockerImage(), config.getDockerImageRepo(), config.getDockerImageTag());

        // spin the inspector container/image cleanup off in it's own parallel thread
        final ContainerCleaner containerCleaner = new ContainerCleaner(dockerClientManager, runOnImageId, containerId, config.isCleanupInspectorImage());
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future<String> containerCleanerFuture = executor.submit(containerCleaner);
        return containerCleanerFuture;

    }

    private String getHubProjectName(final Config config) {
        return programPaths.unEscape(config.getHubProjectName());
    }

    private String getHubProjectVersion(final Config config) {
        return programPaths.unEscape(config.getHubProjectVersion());
    }

    private boolean initAndValidate(final Config config) throws IOException, IntegrationException, IllegalArgumentException, IllegalAccessException {
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
            hubClient.phoneHome(dockerClientManager.getDockerEngineVersion());
        }
        clearResult();
        imageInspector.initImageName();
        logger.info(String.format("Inspecting image:tag %s:%s", config.getDockerImageRepo(), config.getDockerImageTag()));
        if (config.isOnHost()) {
            hubClient.testHubConnection();
        }
        imageInspector.init();
        FileOperations.removeFileOrDir(programPaths.getHubDockerWorkingDirPath());

        logger.debug(String.format("Upload BDIO is set to %b", config.isUploadBdio()));
        return true;
    }

    private File deriveDockerTarFile(final Config config) throws IOException, HubIntegrationException {
        File dockerTarFile = null;
        if (StringUtils.isNotBlank(config.getDockerTar())) {
            dockerTarFile = new File(config.getDockerTar());
        } else if (StringUtils.isNotBlank(config.getDockerImageId())) {
            dockerTarFile = dockerClientManager.getTarFileFromDockerImageById(config.getDockerImageId());
        } else if (StringUtils.isNotBlank(config.getDockerImageRepo())) {
            dockerTarFile = dockerClientManager.getTarFileFromDockerImage(config.getDockerImageRepo(), config.getDockerImageTag());
        }
        return dockerTarFile;
    }
}
