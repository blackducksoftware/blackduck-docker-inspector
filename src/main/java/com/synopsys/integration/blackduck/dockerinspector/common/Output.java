/**
 * blackduck-docker-inspector
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
package com.synopsys.integration.blackduck.dockerinspector.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.dockerinspector.blackduckclient.BlackDuckClient;
import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.ProgramPaths;
import com.synopsys.integration.blackduck.dockerinspector.dockerexec.DissectedImage;
import com.synopsys.integration.blackduck.exception.HubIntegrationException;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageInfoDerived;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageInspector;
import com.synopsys.integration.blackduck.imageinspector.lib.OperatingSystemEnum;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.blackduck.imageinspector.linux.FileSys;
import com.synopsys.integration.blackduck.imageinspector.name.Names;
import com.synopsys.integration.blackduck.imageinspector.result.Result;
import com.synopsys.integration.blackduck.imageinspector.result.ResultFile;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.BdioWriter;
import com.synopsys.integration.hub.bdio.model.SimpleBdioDocument;

@Component
public class Output {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Config config;

    @Autowired
    private BlackDuckClient blackDuckClient;

    @Autowired
    private ProgramPaths programPaths;

    @Autowired
    private ImageInspector imageInspector;

    @Autowired
    private ResultFile resultFile;

    @Autowired
    private Gson gson;

    public void ensureWriteability() {
        // TODO all isOnHost tests go away when docker exec mode goes away
        if (config.isOnHost()) {
            final File outputDir = new File(programPaths.getDockerInspectorOutputPathHost());
            final boolean dirCreated = outputDir.mkdirs();
            final boolean dirMadeWriteable = outputDir.setWritable(true, false);
            final boolean dirMadeExecutable = outputDir.setExecutable(true, false);
            logger.debug(String.format("Output dir: %s; created: %b; successfully made writeable: %b; make executable: %b", outputDir.getAbsolutePath(), dirCreated, dirMadeWriteable, dirMadeExecutable));
        }
    }

    // TODO this becomes a private method when docker exec mode goes away
    public void provideOutput() throws IOException {
        if (config.isOnHost()) {
            copyOutputToUserOutputDir();
        }
    }

    public File provideBdioFileOutput(final SimpleBdioDocument bdioDocument, final String outputBdioFilename) throws IOException, IntegrationException {
        // if user specified an output dir, use that; else use the temp output dir
        File outputDir;
        if (StringUtils.isNotBlank(config.getOutputPath())) {
            outputDir = new File(config.getOutputPath());
            provideOutput();
        } else {
            outputDir = new File(programPaths.getDockerInspectorOutputPathHost());
        }
        final File outputBdioFile = new File(outputDir, outputBdioFilename);
        final FileOutputStream outputBdioStream = new FileOutputStream(outputBdioFile);
        logger.info(String.format("Writing BDIO to %s", outputBdioFile.getAbsolutePath()));
        try (BdioWriter bdioWriter = new BdioWriter(gson, outputBdioStream)) {
            bdioWriter.writeSimpleBdioDocument(bdioDocument);
        }
        return outputBdioFile;
    }

    // TODO This method is only used in docker exec mode
    public void writeBdioFile(final DissectedImage dissectedImage, final ImageInfoDerived imageInfoDerived) throws FileNotFoundException, IOException {
        final File bdioFile = imageInspector.writeBdioFile(new File(programPaths.getDockerInspectorOutputPath()), imageInfoDerived);
        logger.info(String.format("BDIO File generated: %s", bdioFile.getAbsolutePath()));
        dissectedImage.setBdioFilename(bdioFile.getName());
    }

    // TODO This method is only used in docker exec mode
    public void uploadBdio(final DissectedImage dissectedImage) throws IntegrationException {
        if (config.isUploadBdio()) {
            logger.info("Uploading BDIO to Black Duck");
            dissectedImage.setBdioFilename(uploadBdioFiles());
        }
    }

    // TODO this method is only used in docker exec mode
    public void createContainerFileSystemTarIfRequested(final File targetImageFileSystemRootDir) throws IOException, CompressorException {
        if (config.isOutputIncludeContainerfilesystem()) {
            logger.info("Including container file system in output");
            final File outputDirectory = new File(programPaths.getDockerInspectorOutputPath());
            logger.debug(String.format("outputDirectory: %s", outputDirectory.getAbsolutePath()));
            final String containerFileSystemTarFilename = Names.getContainerFileSystemTarFilename(config.getDockerImage(), config.getDockerTar());
            final File containerFileSystemTarFile = new File(outputDirectory, containerFileSystemTarFilename);
            logger.debug(String.format("Creating container filesystem tarfile %s from %s into %s", containerFileSystemTarFile.getAbsolutePath(), targetImageFileSystemRootDir.getAbsolutePath(), outputDirectory.getAbsolutePath()));
            final FileSys containerFileSys = new FileSys(targetImageFileSystemRootDir);
            containerFileSys.createTarGz(containerFileSystemTarFile);
        }
    }

    // TODO for docker exec mode only
    public void cleanUp(final Future<String> deferredCleanup) {
        if (config.isOnHost() && config.isCleanupWorkingDir()) {
            cleanupWorkingDirs();
        }
        if (deferredCleanup != null) {
            try {
                logger.debug("Waiting for completion of concurrent inspector container/image cleanup");
                logger.info(String.format("Status from concurrent cleanup: %s", deferredCleanup.get(120, TimeUnit.SECONDS)));
            } catch (final TimeoutException e) {
                logger.error("Container cleanup timed out; You may need to stop and/or remove image inspector containers manually");
            } catch (InterruptedException | ExecutionException e) {
                logger.error(String.format("Error during concurrent cleanup: %s", e.getMessage()), e);
            }
        }
    }

    // TODO docker exec mode only
    public int reportResultsPkgMgrDataNotFound(final DissectedImage dissectedImage) throws IOException, IntegrationException {
        reportResult(null, null, null,
                dissectedImage.getDockerTarFile() == null ? "" : dissectedImage.getDockerTarFile().getName(), dissectedImage.getBdioFilename(), true);
        copyResultToUserOutputDir();
        return 0;
    }

    // TODO docker exec mode only
    public int reportResults(final DissectedImage dissectedImage) throws IOException, IntegrationException {
        final int returnCode = reportResult(dissectedImage.getTargetOs(), dissectedImage.getRunOnImageName(), dissectedImage.getRunOnImageTag(),
                dissectedImage.getDockerTarFile() == null ? "" : dissectedImage.getDockerTarFile().getName(), dissectedImage.getBdioFilename(), false);
        if (config.isOnHost()) {
            copyResultToUserOutputDir();
        }
        return returnCode;
    }

    // TODO docker exec mode only
    private void cleanupWorkingDirs() {
        logger.debug(String.format("Removing %s, %s, %s", programPaths.getDockerInspectorWorkingDirPathHost(), programPaths.getDockerInspectorTargetDirPathHost(), programPaths.getDockerInspectorOutputPathHost()));
        try {
            FileOperations.removeFileOrDir(programPaths.getDockerInspectorWorkingDirPathHost());
            FileOperations.removeFileOrDir(programPaths.getDockerInspectorTargetDirPathHost());
            FileOperations.removeFileOrDir(programPaths.getDockerInspectorOutputPathHost());
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
        logger.debug(String.format("Copying result file from %s to %s", programPaths.getDockerInspectorHostResultPath(), userOutputDirPath));
        final File sourceResultFile = new File(programPaths.getDockerInspectorHostResultPath());
        final File userOutputDir = new File(userOutputDirPath);
        final File targetFile = new File(userOutputDir, sourceResultFile.getName());
        logger.debug(String.format("Removing %s if it exists", targetFile.getAbsolutePath()));
        FileOperations.removeFileOrDirQuietly(targetFile.getAbsolutePath());
        FileOperations.copyFile(new File(programPaths.getDockerInspectorHostResultPath()), userOutputDir);
    }

    private int reportResult(final OperatingSystemEnum targetOs, final String runOnImageName, final String runOnImageTag, final String dockerTarfilename, final String bdioFilename, final boolean forceSuccess)
            throws IntegrationException {
        final Gson gson = new Gson();
        if (forceSuccess) {
            writeSuccessResultFile(gson, programPaths.getDockerInspectorHostResultPath(), targetOs, runOnImageName, runOnImageTag, dockerTarfilename, bdioFilename);
            return 0;
        }
        if (config.isOnHost()) {
            final Result resultReportedFromContainer = resultFile.read(gson, programPaths.getDockerInspectorContainerResultPathOnHost());
            if (!resultReportedFromContainer.isSucceeded()) {
                logger.error(String.format("*** Failed: %s", resultReportedFromContainer.getMessage()));
                writeFailureResultFile(gson, programPaths.getDockerInspectorHostResultPath(), targetOs, runOnImageName, runOnImageTag, dockerTarfilename, bdioFilename, resultReportedFromContainer.getMessage());
                return -1;
            } else {
                logger.info("*** Succeeded");
                writeSuccessResultFile(gson, programPaths.getDockerInspectorHostResultPath(), targetOs, runOnImageName, runOnImageTag, dockerTarfilename, resultReportedFromContainer.getBdioFilename());
                return 0;
            }
        } else {
            writeSuccessResultFile(gson, programPaths.getDockerInspectorContainerResultPathInContainer(), targetOs, runOnImageName, runOnImageTag, dockerTarfilename, bdioFilename);
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
        final File srcDir = new File(programPaths.getDockerInspectorOutputPathHost());
        if (!srcDir.exists()) {
            logger.info(String.format("Output source dir %s does not exist", srcDir.getAbsolutePath()));
            return;
        }
        logger.info(String.format("Copying output from %s to %s", programPaths.getDockerInspectorOutputPathHost(), userOutputDirPath));
        final File userOutputDir = new File(userOutputDirPath);
        FileOperations.copyDirContentsToDir(programPaths.getDockerInspectorOutputPathHost(), userOutputDir.getAbsolutePath(), true);
    }

    private String uploadBdioFiles() throws IntegrationException {
        String pathToDirContainingBdio = null;
        pathToDirContainingBdio = programPaths.getDockerInspectorOutputPath();
        logger.debug(String.format("Uploading BDIO files from %s", pathToDirContainingBdio));
        String bdioFilename = null;
        final List<File> bdioFiles = findBdioFiles(pathToDirContainingBdio);
        if (bdioFiles.size() == 0) {
            logger.warn("No BDIO Files generated");
        } else if (bdioFiles.size() > 1) {
            throw new HubIntegrationException(String.format("Found %d BDIO files in %s", bdioFiles.size(), pathToDirContainingBdio));
        } else {
            bdioFilename = bdioFiles.get(0).getName();
            logger.info(String.format("Uploading BDIO to Black Duck: %d files; first file: %s", bdioFiles.size(), bdioFiles.get(0).getAbsolutePath()));
            uploadBdioFiles(bdioFiles);
        }
        return bdioFilename;
    }

    private void uploadBdioFiles(final List<File> bdioFiles) throws IntegrationException {
        if (blackDuckClient.isValid()) {
            if (bdioFiles != null) {
                for (final File file : bdioFiles) {
                    blackDuckClient.uploadBdio(file);
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
