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
package com.blackducksoftware.integration.hub.docker.dockerinspector.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.compress.compressors.CompressorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.docker.dockerinspector.config.Config;
import com.blackducksoftware.integration.hub.docker.dockerinspector.config.ProgramPaths;
import com.blackducksoftware.integration.hub.docker.dockerinspector.hubclient.HubClient;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.imageinspector.lib.DissectedImage;
import com.blackducksoftware.integration.hub.imageinspector.lib.ImageInfoDerived;
import com.blackducksoftware.integration.hub.imageinspector.lib.ImageInspector;
import com.blackducksoftware.integration.hub.imageinspector.lib.OperatingSystemEnum;
import com.blackducksoftware.integration.hub.imageinspector.linux.FileOperations;
import com.blackducksoftware.integration.hub.imageinspector.linux.FileSys;
import com.blackducksoftware.integration.hub.imageinspector.name.Names;
import com.blackducksoftware.integration.hub.imageinspector.result.Result;
import com.blackducksoftware.integration.hub.imageinspector.result.ResultFile;
import com.google.gson.Gson;

@Component
public class Output {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private HubClient hubClient;

    @Autowired
    private ProgramPaths programPaths;

    @Autowired
    private ImageInspector imageInspector;

    @Autowired
    private ResultFile resultFile;

    public void provideOutput(final Config config) throws IOException {
        if (config.isOnHost()) {
            copyOutputToUserOutputDir();
        }
    }

    public void ensureWriteability(final Config config) {
        if (config.isOnHost()) {
            final File outputDir = new File(programPaths.getHubDockerOutputPathHost());
            final boolean dirCreated = outputDir.mkdirs();
            //// TODO write and execute permission may not be needed, since its mounted for container as rwx
            final boolean dirMadeWriteable = outputDir.setWritable(true, false);
            final boolean dirMadeExecutable = outputDir.setExecutable(true, false);
            //////////////
            logger.debug(String.format("Output dir: %s; created: %b; successfully made writeable: %b; make executable: %b", outputDir.getAbsolutePath(), dirCreated, dirMadeWriteable, dirMadeExecutable));
        }
    }

    public void writeBdioFile(final DissectedImage dissectedImage, final ImageInfoDerived imageInfoDerived) throws FileNotFoundException, IOException {
        final File bdioFile = imageInspector.writeBdioFile(new File(programPaths.getHubDockerOutputPath()), imageInfoDerived);
        logger.info(String.format("BDIO File generated: %s", bdioFile.getAbsolutePath()));
        dissectedImage.setBdioFilename(bdioFile.getName());
    }

    public void uploadBdio(final Config config, final DissectedImage dissectedImage) throws IntegrationException {
        if (config.isUploadBdio()) {
            logger.info("Uploading BDIO to Hub");
            dissectedImage.setBdioFilename(uploadBdioFiles(config));
        }
    }

    public void createContainerFileSystemTarIfRequested(final Config config, final File targetImageFileSystemRootDir) throws IOException, CompressorException {
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

    public void cleanUp(final Config config, final Future<String> deferredCleanup) {
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

    public int reportResultsPkgMgrDataNotFound(final Config config, final DissectedImage dissectedImage) throws IOException, IntegrationException {
        reportResult(config, null, null, null,
                dissectedImage.getDockerTarFile() == null ? "" : dissectedImage.getDockerTarFile().getName(), dissectedImage.getBdioFilename(), true);
        copyResultToUserOutputDir();
        return 0;
    }

    public int reportResults(final Config config, final DissectedImage dissectedImage) throws IOException, IntegrationException {
        final int returnCode = reportResult(config, dissectedImage.getTargetOs(), dissectedImage.getRunOnImageName(), dissectedImage.getRunOnImageTag(),
                dissectedImage.getDockerTarFile() == null ? "" : dissectedImage.getDockerTarFile().getName(), dissectedImage.getBdioFilename(), false);
        if (config.isOnHost()) {
            copyResultToUserOutputDir();
        }
        return returnCode;
    }

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

    private void copyResultToUserOutputDir() throws IOException {
        final String userOutputDirPath = programPaths.getUserOutputDir();
        if (userOutputDirPath == null) {
            logger.debug("User has not specified an output path");
            return;
        }
        logger.debug(String.format("Copying result file from %s to %s", programPaths.getHubDockerHostResultPath(), userOutputDirPath));
        final File sourceResultFile = new File(programPaths.getHubDockerHostResultPath());
        final File userOutputDir = new File(userOutputDirPath);
        final File targetFile = new File(userOutputDir, sourceResultFile.getName());
        logger.debug(String.format("Removing %s if it exists", targetFile.getAbsolutePath()));
        FileOperations.removeFileOrDirQuietly(targetFile.getAbsolutePath());
        FileOperations.copyFile(new File(programPaths.getHubDockerHostResultPath()), userOutputDir);
    }

    private int reportResult(final Config config, final OperatingSystemEnum targetOs, final String runOnImageName, final String runOnImageTag, final String dockerTarfilename, final String bdioFilename, final boolean forceSuccess)
            throws IntegrationException {
        final Gson gson = new Gson();
        if (forceSuccess) {
            writeSuccessResultFile(gson, programPaths.getHubDockerHostResultPath(), targetOs, runOnImageName, runOnImageTag, dockerTarfilename, bdioFilename);
            return 0;
        }
        if (config.isOnHost()) {
            final Result resultReportedFromContainer = resultFile.read(gson, programPaths.getHubDockerContainerResultPathOnHost());
            if (!resultReportedFromContainer.isSucceeded()) {
                logger.error(String.format("*** Failed: %s", resultReportedFromContainer.getMessage()));
                writeFailureResultFile(gson, programPaths.getHubDockerHostResultPath(), targetOs, runOnImageName, runOnImageTag, dockerTarfilename, bdioFilename, resultReportedFromContainer.getMessage());
                return -1;
            } else {
                logger.info("*** Succeeded");
                writeSuccessResultFile(gson, programPaths.getHubDockerHostResultPath(), targetOs, runOnImageName, runOnImageTag, dockerTarfilename, resultReportedFromContainer.getBdioFilename());
                return 0;
            }
        } else {
            writeSuccessResultFile(gson, programPaths.getHubDockerContainerResultPathInContainer(), targetOs, runOnImageName, runOnImageTag, dockerTarfilename, bdioFilename);
            return 0;
        }

    }

    private void writeSuccessResultFile(final Gson gson, final String resultFilePath, final OperatingSystemEnum targetOs, final String runOnImageName, final String runOnImageTag, final String dockerTarfilename, final String bdioFilename) {
        resultFile.write(gson, resultFilePath, true, "Success", targetOs, runOnImageName, runOnImageTag, dockerTarfilename, bdioFilename);
    }

    private void writeFailureResultFile(final Gson gson, final String resultFilePath, final OperatingSystemEnum targetOs, final String runOnImageName, final String runOnImageTag, final String dockerTarfilename, final String bdioFilename,
            final String msg) {
        resultFile.write(new Gson(), resultFilePath, false, msg, targetOs, runOnImageName, runOnImageTag,
                dockerTarfilename, bdioFilename);
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

    private List<File> findBdioFiles(final String pathToDirContainingBdio) {
        final List<File> bdioFiles = FileOperations.findFilesWithExt(new File(pathToDirContainingBdio), "jsonld");
        logger.info(String.format("Found %d BDIO files in %s", bdioFiles.size(), pathToDirContainingBdio));
        return bdioFiles;
    }
}
