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
package com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.layerentry;

import java.io.File;
import java.nio.file.Files;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhiteOutLayerEntry implements LayerEntry {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final TarArchiveEntry layerEntry;
    private final File layerOutputDir;

    public WhiteOutLayerEntry(final TarArchiveEntry layerEntry, final File layerOutputDir) {
        this.layerEntry = layerEntry;
        this.layerOutputDir = layerOutputDir;
    }

    @Override
    public void process() {
        final String fileSystemEntryName = layerEntry.getName();
        logger.trace(String.format("Found white-out file %s", fileSystemEntryName));

        final int whiteOutMarkIndex = fileSystemEntryName.indexOf(".wh.");
        final String beforeWhiteOutMark = fileSystemEntryName.substring(0, whiteOutMarkIndex);
        final String afterWhiteOutMark = fileSystemEntryName.substring(whiteOutMarkIndex + ".wh.".length());

        final String filePathToRemove = String.format("%s%s", beforeWhiteOutMark, afterWhiteOutMark);
        final File fileToRemove = new File(layerOutputDir, filePathToRemove);
        logger.trace(String.format("Removing %s from image (this layer whites it out)", filePathToRemove));
        if (fileToRemove.isDirectory()) {
            try {
                FileUtils.deleteDirectory(fileToRemove);
                logger.trace(String.format("Directory %s successfully removed", filePathToRemove));
            } catch (final Exception e) {
                logger.warn(String.format("Error removing whited-out directory %s", filePathToRemove));
            }
        } else {
            try {
                Files.delete(fileToRemove.toPath());
                logger.trace(String.format("File %s successfully removed", filePathToRemove));
            } catch (final Exception e) {
                logger.warn(String.format("Error removing whited-out file %s", filePathToRemove));
            }
        }
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, RecursiveToStringStyle.JSON_STYLE);
    }
}
