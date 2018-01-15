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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LayerEntries {
    private static final Logger logger = LoggerFactory.getLogger(LayerEntries.class);

    public static LayerEntry createLayerEntry(final TarArchiveInputStream layerInputStream, final TarArchiveEntry layerEntry, final File layerOutputDir) {
        final String fileSystemEntryName = layerEntry.getName();
        logger.trace(String.format("Processing layerEntry: %s", fileSystemEntryName));
        if ((fileSystemEntryName.startsWith(".wh.")) || (fileSystemEntryName.contains("/.wh."))) {
            return new WhiteOutLayerEntry(layerEntry, layerOutputDir);
        } else if (layerEntry.isSymbolicLink() || layerEntry.isLink()) {
            return new LinkLayerEntry(layerEntry, layerOutputDir);
        } else {
            return new FileDirLayerEntry(layerInputStream, layerEntry, layerOutputDir);
        }
    }
}
