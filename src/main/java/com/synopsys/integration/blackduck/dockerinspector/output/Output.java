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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.ProgramPaths;
import com.synopsys.integration.bdio.BdioWriter;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.blackduck.imageinspector.api.name.Names;
import com.synopsys.integration.exception.IntegrationException;

@Component
public class Output {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Config config;

    @Autowired
    private ProgramPaths programPaths;

    @Autowired
    private SquashedImage squashedImage;

    @Autowired
    private Gson gson;

    public void ensureOutputDirIsWriteable() {
        final File outputDir = new File(programPaths.getDockerInspectorDefaultOutputPath());
        final boolean dirCreated = outputDir.mkdirs();
        final boolean dirMadeWriteable = outputDir.setWritable(true, false);
        final boolean dirMadeExecutable = outputDir.setExecutable(true, false);
        logger.debug(String.format("Output dir: %s; created: %b; successfully made writeable: %b; make executable: %b", outputDir.getAbsolutePath(), dirCreated, dirMadeWriteable, dirMadeExecutable));
    }

    public File addOutputToOutputDir(final SimpleBdioDocument bdioDocument) throws IOException, IntegrationException {
        // if user specified an output dir, use that; else use the temp output dir
        File outputDir;
        if (StringUtils.isNotBlank(config.getOutputPath())) {
            outputDir = new File(config.getOutputPath());
            copyOutputToUserProvidedOutputDir();
        } else {
            outputDir = new File(programPaths.getDockerInspectorDefaultOutputPath());
        }
        final String bdioFilename = new BdioFilename(bdioDocument.billOfMaterials.spdxName).getBdioFilename();
        final File outputBdioFile = new File(outputDir, bdioFilename);
        final FileOutputStream outputBdioStream = new FileOutputStream(outputBdioFile);
        logger.info(String.format("Writing BDIO to %s", outputBdioFile.getAbsolutePath()));
        try (BdioWriter bdioWriter = new BdioWriter(gson, outputBdioStream)) {
            bdioWriter.writeSimpleBdioDocument(bdioDocument);
        }
        final String containerFileSystemFilename = Names.getContainerFileSystemTarFilename(config.getDockerImage(), config.getDockerTar());
        final File containerFileSystemFile = new File(outputDir, containerFileSystemFilename);
        addSquashedImage(outputDir, containerFileSystemFile);
        removeContainerFileSystemIfNotRequested(containerFileSystemFile);
        return outputBdioFile;
    }

    private void addSquashedImage(final File outputDir, final File containerFileSystemFile) throws IntegrationException {
        if (!config.isOutputIncludeSquashedImage()) {
            return;
        }
        if (!containerFileSystemFile.exists()) {
            throw new IntegrationException(String.format("Squashed image requested, but container file system not generated, so can't generate squashed image"));
        }
        logger.debug(String.format("adding squashed image to output in %s", outputDir.getAbsolutePath()));
        final String squashedImageFilename = deriveSquashedImageFilename(containerFileSystemFile.getName());
        final File squashedImageFile = new File(outputDir, squashedImageFilename);
        final File tempTarFile = new File(programPaths.getDockerInspectorSquashedImageTarFilePath());
        tempTarFile.getParentFile().mkdirs();
        logger.debug(String.format("Temp tarfile: %s", tempTarFile.getAbsolutePath()));
        final File tempWorkingDir = new File(programPaths.getDockerInspectorSquashedImageDirPath());
        tempWorkingDir.mkdirs();
        logger.debug(String.format("Temp working dir: %s", tempWorkingDir.getAbsolutePath()));
        try {
            squashedImage.createSquashedImageTarGz(containerFileSystemFile, squashedImageFile, tempTarFile, tempWorkingDir);
        } catch (IOException e) {
            throw new IntegrationException(String.format("Error generating squashed image: %s", e.getMessage()), e);
        }
    }

    private String deriveSquashedImageFilename(final String containerFileSystemFilename) throws IntegrationException {
        if (!containerFileSystemFilename.contains("containerfilesystem")) {
            logger.warn(String.format("Unable to generate squashed image filename from container file system filename %s; using the default name"));
            return "target_squashedimage.tar.gz";
        }
        final String squashedImageFilename = containerFileSystemFilename.replace("containerfilesystem", "squashedimage");
        logger.debug(String.format("Generated squashed image filename %s from container file system name %s", squashedImageFilename, containerFileSystemFilename));
        return squashedImageFilename;
    }

    private void removeContainerFileSystemIfNotRequested(final File containerFileSystemFile) {
        if (!config.isOutputIncludeContainerfilesystem()) {
            logger.debug(String.format("Target image file system file %s was generated only for generation of the squashed image; deleting it", containerFileSystemFile.getName()));
            containerFileSystemFile.delete();
        }
    }

    private void copyOutputToUserProvidedOutputDir() throws IOException {
        final String userOutputDirPath = programPaths.getUserOutputDirPath();
        if (userOutputDirPath == null) {
            logger.debug("User has not specified an output path");
            return;
        }
        final File srcDir = new File(programPaths.getDockerInspectorDefaultOutputPath());
        if (!srcDir.exists()) {
            logger.info(String.format("Output source dir %s does not exist", srcDir.getAbsolutePath()));
            return;
        }
        logger.info(String.format("Copying output from %s to %s", srcDir.getAbsolutePath(), userOutputDirPath));
        final File userOutputDir = new File(userOutputDirPath);
        copyDirContentsToDir(programPaths.getDockerInspectorDefaultOutputPath(), userOutputDir.getAbsolutePath(), true);
    }

    private void copyDirContentsToDir(final String fromDirPath, final String toDirPath, final boolean createIfNecessary) throws IOException {
        final File srcDir = new File(fromDirPath);
        final File destDir = new File(toDirPath);
        if (createIfNecessary && !destDir.exists()) {
            destDir.mkdirs();
        }
        FileUtils.copyDirectory(srcDir, destDir);
    }
}
