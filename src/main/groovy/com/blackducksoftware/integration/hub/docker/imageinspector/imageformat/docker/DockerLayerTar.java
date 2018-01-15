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
package com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.layerentry.LayerEntries;
import com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.layerentry.LayerEntry;

public class DockerLayerTar {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final File layerTar;

    public DockerLayerTar(final File layerTar) {
        this.layerTar = layerTar;
    }

    public void extractToDir(final File layerOutputDir) throws IOException {
        logger.debug(String.format("layerTar: %s", layerTar.getAbsolutePath()));
        final TarArchiveInputStream layerInputStream = new TarArchiveInputStream(new FileInputStream(layerTar), "UTF-8");
        try {
            layerOutputDir.mkdirs();
            logger.debug(String.format("layerOutputDir: %s", layerOutputDir.getAbsolutePath()));
            TarArchiveEntry layerEntry;
            while (null != (layerEntry = layerInputStream.getNextTarEntry())) {
                try {
                    final LayerEntry layerEntryHandler = LayerEntries.createLayerEntry(layerInputStream, layerEntry, layerOutputDir);
                    layerEntryHandler.process();
                } catch (final Exception e) {
                    logger.error(String.format("Error extracting files from layer tar: %s", e.toString()));
                }
            }
        } finally {
            IOUtils.closeQuietly(layerInputStream);
        }
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, RecursiveToStringStyle.JSON_STYLE);
    }
}
