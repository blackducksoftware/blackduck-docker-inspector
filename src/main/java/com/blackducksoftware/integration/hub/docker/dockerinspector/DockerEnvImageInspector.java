/**
 * hub-docker-inspector
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
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
package com.blackducksoftware.integration.hub.docker.dockerinspector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
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
import org.springframework.context.annotation.ComponentScan;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.docker.dockerinspector.config.Config;
import com.blackducksoftware.integration.hub.docker.dockerinspector.config.ProgramPaths;
import com.blackducksoftware.integration.hub.docker.dockerinspector.dockerclient.DockerClientManager;
import com.blackducksoftware.integration.hub.docker.dockerinspector.help.formatter.UsageFormatter;
import com.blackducksoftware.integration.hub.docker.dockerinspector.hubclient.HubClient;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.imageinspector.imageformat.docker.manifest.ManifestLayerMapping;
import com.blackducksoftware.integration.hub.imageinspector.lib.DissectedImage;
import com.blackducksoftware.integration.hub.imageinspector.lib.ImageInfoDerived;
import com.blackducksoftware.integration.hub.imageinspector.lib.ImageInspector;
import com.blackducksoftware.integration.hub.imageinspector.lib.OperatingSystemEnum;
import com.blackducksoftware.integration.hub.imageinspector.linux.FileOperations;
import com.blackducksoftware.integration.hub.imageinspector.linux.FileSys;
import com.blackducksoftware.integration.hub.imageinspector.name.ImageNameResolver;
import com.blackducksoftware.integration.hub.imageinspector.name.Names;
import com.blackducksoftware.integration.hub.imageinspector.result.Result;
import com.blackducksoftware.integration.hub.imageinspector.result.ResultFile;
import com.google.gson.Gson;

@SpringBootApplication
@ComponentScan(basePackages = { "com.blackducksoftware.integration.hub.imageinspector", "com.blackducksoftware.integration.hub.docker.dockerinspector" })
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

    public static void main(final String[] args) {
        new SpringApplicationBuilder(DockerEnvImageInspector.class).logStartupInfo(false).run(args);
        logger.warn("The program is not expected to get here.");
    }

    @PostConstruct
    public void newInspectImage() {
        int returnCode = -1;
        final DissectedImage dissectedImage = new DissectedImage();
        try {
            if (!initAndValidate(config)) {
                System.exit(0);
            }
            // TODO get BDIO via container (later: starting them if necessary)
            final File dockerTarFile = deriveDockerTarFile(config);
            final String dockerTarFilePathInContainer = toContainer(dockerTarFile.getCanonicalPath(), new File(config.getWorkingDirPath()).getCanonicalPath(), config.getWorkingDirPathImageInspector());
            if (StringUtils.isBlank(config.getImageInspectorUrl())) {
                throw new IntegrationException("The imageinspector URL property must be set");
            }
            new ImageInspectorWebServiceClient().getBdio(config.getImageInspectorUrl(), dockerTarFilePathInContainer, config.isCleanupWorkingDir());
            // TODO take what that returns and do something with it
            returnCode = 0;
        } catch (final Throwable e) {
            final String msg = String.format("Error inspecting image: %s", e.getMessage());
            logger.error(msg);
            final String trace = ExceptionUtils.getStackTrace(e);
            logger.debug(String.format("Stack trace: %s", trace));
            resultFile.write(new Gson(), programPaths.getHubDockerResultPath(), false, msg, dissectedImage.getTargetOs(), dissectedImage.getRunOnImageName(), dissectedImage.getRunOnImageTag(),
                    dissectedImage.getDockerTarFile() == null ? "" : dissectedImage.getDockerTarFile().getName(), dissectedImage.getBdioFilename());
        }
        logger.info(String.format("Returning %d", returnCode));
        System.exit(returnCode);
    }

    // TODO move to ProgramPaths or something
    /*
     * Translate a local path to a container path ASSUMING the local working dir is mounted for the container as it's working dir. Find path to the given localPath RELATIVE to the local working dir. Convert that to the container's path by
     * appending that relative path to the container's working dir
     */
    private String toContainer(final String localPath, final String workingDirPath, final String workingDirPathImageInspector) {
        logger.debug(String.format("localPath: %s", localPath));
        if (StringUtils.isBlank(workingDirPathImageInspector)) {
            logger.debug(String.format("config.getWorkingDirPathImageInspector() is BLANK"));
            return localPath;
        }
        final String trimmedWorkingDirPath = trimTrailingFileSeparator(workingDirPath);
        final String trimmedWorkingDirPathImageInspector = trimTrailingFileSeparator(workingDirPathImageInspector);
        logger.debug(String.format("config.getWorkingDirPath(): %s", trimmedWorkingDirPath));
        final String localRelPath = localPath.substring(trimmedWorkingDirPath.length());
        logger.debug(String.format("localRelPath: %s", localRelPath));
        final String containerPath = String.format("%s%s", trimmedWorkingDirPathImageInspector, localRelPath);

        return containerPath;
    }

    String trimTrailingFileSeparator(final String path) {
        if (StringUtils.isBlank(path) || !path.endsWith("/")) {
            return path;
        }
        return path.substring(0, path.length() - 1);
    }

    private boolean initAndValidate(final Config config) throws IOException, IntegrationException, IllegalArgumentException, IllegalAccessException {
        logger.info(String.format("hub-docker-inspector %s", programVersion.getProgramVersion()));
        if (helpInvoked()) {
            showUsage();
            return false;
        }
        logger.debug(String.format("running from dir: %s", System.getProperty("user.dir")));
        logger.trace(String.format("dockerImageTag: %s", config.getDockerImageTag()));
        if (config.isDryRun()) {
            logger.error("dry.run is deprecated. Set upload.bdio=false instead");
            return false;
        }
        hubClient.phoneHome(dockerClientManager.getDockerEngineVersion());
        initImageName();
        logger.info(String.format("Inspecting image:tag %s:%s", config.getDockerImageRepo(), config.getDockerImageTag()));
        hubClient.testHubConnection();
        return true;
    }

    private void initImageName() throws HubIntegrationException {
        logger.debug(String.format("initImageName(): dockerImage: %s, dockerTar: %s", config.getDockerImage(), config.getDockerTar()));
        final ImageNameResolver resolver = new ImageNameResolver(config.getDockerImage());
        resolver.getNewImageRepo().ifPresent(repoName -> config.setDockerImageRepo(repoName));
        resolver.getNewImageTag().ifPresent(tagName -> config.setDockerImageTag(tagName));
        logger.debug(String.format("initImageName(): final: dockerImage: %s; dockerImageRepo: %s; dockerImageTag: %s", config.getDockerImage(), config.getDockerImageRepo(), config.getDockerImageTag()));
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

    /////////////////// Old / Suspect code: //////////////////
    // public void inspectImage() {
    // int returnCode = -1;
    // final DissectedImage dissectedImage = new DissectedImage();
    // try {
    // if (!initAndValidate(config)) {
    // System.exit(0);
    // }
    // parseManifest(config, dissectedImage);
    // checkForGivenTargetOs(config, dissectedImage);
    // constructContainerFileSystem(config, dissectedImage);
    // try {
    // determineTargetOsFromContainerFileSystem(config, dissectedImage);
    // final Future<String> deferredCleanup = inspect(config, dissectedImage);
    // uploadBdio(config, dissectedImage);
    // provideDockerTar(config, dissectedImage.getDockerTarFile());
    // provideOutput(config);
    // returnCode = reportResults(config, dissectedImage);
    // cleanUp(config, deferredCleanup);
    // } catch (final PkgMgrDataNotFoundException e) {
    // logger.info("Pkg mgr not found; generating empty BDIO file");
    // final ImageInfoDerived imageInfoDerived = imageInspector.generateEmptyBdio(config.getDockerImageRepo(), config.getDockerImageTag(), dissectedImage.getLayerMappings(), getHubProjectName(config),
    // getHubProjectVersion(config), dissectedImage.getDockerTarFile(), dissectedImage.getTargetImageFileSystemRootDir(), dissectedImage.getTargetOs(), config.getHubCodelocationPrefix());
    // writeBdioFile(dissectedImage, imageInfoDerived);
    // uploadBdio(config, dissectedImage);
    // createContainerFileSystemTarIfRequested(config, dissectedImage.getTargetImageFileSystemRootDir());
    // provideOutput(config);
    // returnCode = reportResultsPkgMgrDataNotFound(config, dissectedImage);
    // cleanUp(config, null);
    // }
    // } catch (final Throwable e) {
    // final String msg = String.format("Error inspecting image: %s", e.getMessage());
    // logger.error(msg);
    // final String trace = ExceptionUtils.getStackTrace(e);
    // logger.debug(String.format("Stack trace: %s", trace));
    // resultFile.write(new Gson(), programPaths.getHubDockerResultPath(), false, msg, dissectedImage.getTargetOs(), dissectedImage.getRunOnImageName(), dissectedImage.getRunOnImageTag(),
    // dissectedImage.getDockerTarFile() == null ? "" : dissectedImage.getDockerTarFile().getName(), dissectedImage.getBdioFilename());
    // }
    // logger.info(String.format("Returning %d", returnCode));
    // System.exit(returnCode);
    // }

    private void checkForGivenTargetOs(final Config config, final DissectedImage dissectedImage) {
        dissectedImage.setTargetOs(imageInspector.detectOperatingSystem(config.getLinuxDistro()));
    }

    private void cleanUp(final Config config, final Future<String> deferredCleanup) {
        if (config.isOnHost() && config.isCleanupWorkingDir()) {
            cleanupWorkingDirs();
        }
        if (deferredCleanup != null) {
            try {
                logger.debug("Waiting for completion of concurrent inspector container/image cleanup");
                logger.info(String.format("Status from concurrent cleanup: %s", deferredCleanup.get(120, TimeUnit.SECONDS)));
            } catch (final TimeoutException e) {
                logger.error("Container cleanup timed out; You may need to stop and/or remove hub-docker-inspector containers manually");
            } catch (InterruptedException | ExecutionException e) {
                logger.error(String.format("Error during concurrent cleanup: %s", e.getMessage()), e);
            }
        }
    }

    private int reportResultsPkgMgrDataNotFound(final Config config, final DissectedImage dissectedImage) throws IOException, IntegrationException {
        reportResult(config, null, null, null,
                dissectedImage.getDockerTarFile() == null ? "" : dissectedImage.getDockerTarFile().getName(), dissectedImage.getBdioFilename(), true);
        copyResultToUserOutputDir();
        return 0;
    }

    private int reportResults(final Config config, final DissectedImage dissectedImage) throws IOException, IntegrationException {
        final int returnCode = reportResult(config, dissectedImage.getTargetOs(), dissectedImage.getRunOnImageName(), dissectedImage.getRunOnImageTag(),
                dissectedImage.getDockerTarFile() == null ? "" : dissectedImage.getDockerTarFile().getName(), dissectedImage.getBdioFilename(), false);
        if (config.isOnHost()) {
            copyResultToUserOutputDir();
        }
        return returnCode;
    }

    private void provideOutput(final Config config) throws IOException {
        if (config.isOnHost()) {
            copyOutputToUserOutputDir();
        }
    }

    private void uploadBdio(final Config config, final DissectedImage dissectedImage) throws IntegrationException {
        if (config.isUploadBdio()) {
            logger.info("Uploading BDIO to Hub");
            dissectedImage.setBdioFilename(uploadBdioFiles(config));
        }
    }

    // private Future<String> inspect(final Config config, final DissectedImage dissectedImage) throws IOException, InterruptedException, CompressorException, IllegalAccessException, IntegrationException {
    // Future<String> deferredCleanup = null;
    // if (config.isOnHost()) {
    // logger.info("Inspecting image in container");
    // deferredCleanup = inspectInSubContainer(config, dissectedImage);
    // } else {
    // if (dissectedImage.getTargetImageFileSystemRootDir() == null) {
    // dissectedImage.setTargetImageFileSystemRootDir(
    // imageInspector.extractDockerLayers(new File(programPaths.getHubDockerWorkingDirPath()), config.getDockerImageRepo(), config.getDockerImageTag(), dissectedImage.getLayerTars(), dissectedImage.getLayerMappings()));
    // }
    // if (dissectedImage.getTargetOs() == null) {
    // dissectedImage.setTargetOs(imageInspector.detectOperatingSystem(dissectedImage.getTargetImageFileSystemRootDir()));
    // }
    // logger.info(String.format("Target image tarfile: %s; target OS: %s", dissectedImage.getDockerTarFile().getAbsolutePath(), dissectedImage.getTargetOs().toString()));
    // final ImageInfoDerived imageInfoDerived = imageInspector.generateBdioFromImageFilesDir(config.getDockerImageRepo(), config.getDockerImageTag(), dissectedImage.getLayerMappings(), getHubProjectName(config),
    // getHubProjectVersion(config), dissectedImage.getDockerTarFile(), dissectedImage.getTargetImageFileSystemRootDir(), dissectedImage.getTargetOs(), config.getHubCodelocationPrefix());
    // writeBdioFile(dissectedImage, imageInfoDerived);
    // createContainerFileSystemTarIfRequested(config, dissectedImage.getTargetImageFileSystemRootDir());
    // }
    // return deferredCleanup;
    // }

    private void writeBdioFile(final DissectedImage dissectedImage, final ImageInfoDerived imageInfoDerived) throws FileNotFoundException, IOException {
        final File bdioFile = imageInspector.writeBdioFile(new File(programPaths.getHubDockerOutputPath()), imageInfoDerived);
        logger.info(String.format("BDIO File generated: %s", bdioFile.getAbsolutePath()));
        dissectedImage.setBdioFilename(bdioFile.getName());
    }

    private void constructContainerFileSystem(final Config config, final DissectedImage dissectedImage) throws IOException, IntegrationException {
        if (config.isOnHost() && dissectedImage.getTargetOs() != null && !config.isOutputIncludeContainerfilesystem()) {
            // don't need to construct container File System
            return;
        }
        dissectedImage.setTargetImageFileSystemRootDir(
                imageInspector.extractDockerLayers(new File(programPaths.getHubDockerWorkingDirPath()), config.getDockerImageRepo(), config.getDockerImageTag(), dissectedImage.getLayerTars(), dissectedImage.getLayerMappings()));
    }

    private void determineTargetOsFromContainerFileSystem(final Config config, final DissectedImage dissectedImage) throws IOException, IntegrationException {
        if (dissectedImage.getTargetOs() == null) {
            dissectedImage.setTargetOs(imageInspector.detectOperatingSystem(dissectedImage.getTargetImageFileSystemRootDir()));
        }
        dissectedImage.setRunOnImageName(dockerImages.getInspectorImageName(dissectedImage.getTargetOs()));
        dissectedImage.setRunOnImageTag(dockerImages.getInspectorImageTag(dissectedImage.getTargetOs()));
        logger.info(String.format("Identified target OS: %s; corresponding inspection image: %s:%s", dissectedImage.getTargetOs().name(), dissectedImage.getRunOnImageName(), dissectedImage.getRunOnImageTag()));
        if (StringUtils.isBlank(dissectedImage.getRunOnImageName()) || StringUtils.isBlank(dissectedImage.getRunOnImageTag())) {
            throw new HubIntegrationException("Failed to identify inspection image name and/or tag");
        }
    }

    // private void parseManifest(final Config config, final DissectedImage dissectedImage) throws IOException, HubIntegrationException, Exception {
    // dissectedImage.setDockerTarFile(deriveDockerTarFile(config));
    // dissectedImage.setLayerTars(imageInspector.extractLayerTars(new File(programPaths.getHubDockerWorkingDirPath()), dissectedImage.getDockerTarFile()));
    // dissectedImage.setLayerMappings(imageInspector.getLayerMappings(new File(programPaths.getHubDockerWorkingDirPath()), dissectedImage.getDockerTarFile().getName(), config.getDockerImageRepo(), config.getDockerImageTag()));
    // adjustImageNameTagFromLayerMappings(dissectedImage.getLayerMappings());
    // }

    private void cleanupWorkingDirs() {
        logger.debug(String.format("Removing %s, %s, %s", programPaths.getHubDockerWorkingDirPathHost(), programPaths.getHubDockerTargetDirPathHost(), programPaths.getHubDockerOutputPathHost()));
        try {
            FileOperations.removeFileOrDir(programPaths.getHubDockerWorkingDirPathHost());
            FileOperations.removeFileOrDir(programPaths.getHubDockerTargetDirPathHost());
            FileOperations.removeFileOrDir(programPaths.getHubDockerOutputPathHost());
        } catch (final IOException e) {
            logger.error(String.format("Error cleaning up working directories: %s", e.getMessage()));
        }
    }

    private boolean helpInvoked() {
        logger.debug("Checking to see if help argument passed");
        if (applicationArguments == null) {
            logger.debug("applicationArguments is null");
            return false;
        }
        final String[] args = applicationArguments.getSourceArgs();
        if (contains(args, "-h") || contains(args, "--help")) {
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

    private String uploadBdioFiles(final Config config) throws IntegrationException {
        String pathToDirContainingBdio = null;
        pathToDirContainingBdio = programPaths.getHubDockerOutputPath();
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
            uploadBdioFiles(bdioFiles);
        }
        return bdioFilename;
    }

    private void uploadBdioFiles(final List<File> bdioFiles) throws IntegrationException {
        if (hubClient.isValid()) {
            if (bdioFiles != null) {
                for (final File file : bdioFiles) {
                    hubClient.uploadBdioToHub(file);
                }
            }
            logger.info(" ");
            logger.info("Successfully uploaded all of the bdio files!");
            logger.info(" ");
        }
    }

    private int reportResult(final Config config, final OperatingSystemEnum targetOs, final String runOnImageName, final String runOnImageTag, final String dockerTarfilename, final String bdioFilename, final boolean forceSuccess)
            throws IntegrationException {
        final Gson gson = new Gson();
        if (forceSuccess) {
            writeSuccessResultFile(gson, targetOs, runOnImageName, runOnImageTag, dockerTarfilename, bdioFilename);
            return 0;
        }
        if (config.isOnHost()) {
            final Result resultReportedFromContainer = resultFile.read(gson, programPaths.getHubDockerResultPath());
            if (!resultReportedFromContainer.isSucceeded()) {
                logger.error(String.format("*** Failed: %s", resultReportedFromContainer.getMessage()));
                return -1;
            } else {
                logger.info("*** Succeeded");
                return 0;
            }
        } else {
            writeSuccessResultFile(gson, targetOs, runOnImageName, runOnImageTag, dockerTarfilename, bdioFilename);
            return 0;
        }
    }

    private void writeSuccessResultFile(final Gson gson, final OperatingSystemEnum targetOs, final String runOnImageName, final String runOnImageTag, final String dockerTarfilename, final String bdioFilename) {
        resultFile.write(gson, programPaths.getHubDockerResultPath(), true, "Success", targetOs, runOnImageName, runOnImageTag, dockerTarfilename, bdioFilename);
    }

    private List<File> findBdioFiles(final String pathToDirContainingBdio) {
        final List<File> bdioFiles = FileOperations.findFilesWithExt(new File(pathToDirContainingBdio), "jsonld");
        logger.info(String.format("Found %d BDIO files in %s", bdioFiles.size(), pathToDirContainingBdio));
        return bdioFiles;
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
            logger.info("Including container file system in output");
            final File outputDirectory = new File(programPaths.getHubDockerOutputPath());
            final String containerFileSystemTarFilename = Names.getContainerFileSystemTarFilename(config.getDockerImageRepo(), config.getDockerImageTag());
            final File containerFileSystemTarFile = new File(outputDirectory, containerFileSystemTarFilename);
            logger.debug(String.format("Creating container filesystem tarfile %s from %s into %s", containerFileSystemTarFile.getAbsolutePath(), targetImageFileSystemRootDir.getAbsolutePath(), outputDirectory.getAbsolutePath()));
            final FileSys containerFileSys = new FileSys(targetImageFileSystemRootDir);
            containerFileSys.createTarGz(containerFileSystemTarFile);
        }
    }

    // TODO: In container scenario: do initial eval in container instead of on "host"?
    // private Future<String> inspectInSubContainer(final Config config, final DissectedImage imageDetails)
    // throws IntegrationException {
    // // TODO choose implementation, but use injection
    // final Inspector inspector = new DockerExecInspector();
    // return inspector.inspectInSubContainer(config, imageDetails);
    // }

    private String getHubProjectName(final Config config) {
        return programPaths.unEscape(config.getHubProjectName());
    }

    private String getHubProjectVersion(final Config config) {
        return programPaths.unEscape(config.getHubProjectVersion());
    }

    private void adjustImageNameTagFromLayerMappings(final List<ManifestLayerMapping> layerMappings) {
        if (layerMappings != null && layerMappings.size() == 1) {
            if (StringUtils.isBlank(config.getDockerImageRepo())) {
                config.setDockerImageRepo(layerMappings.get(0).getImageName());
            }
            if (StringUtils.isBlank(config.getDockerImageTag())) {
                config.setDockerImageTag(layerMappings.get(0).getTagName());
            }
        }
        logger.debug(String.format("adjustImageNameTagFromLayerMappings(): final: dockerImage: %s; dockerImageRepo: %s; dockerImageTag: %s", config.getDockerImage(), config.getDockerImageRepo(), config.getDockerImageTag()));
    }

}
