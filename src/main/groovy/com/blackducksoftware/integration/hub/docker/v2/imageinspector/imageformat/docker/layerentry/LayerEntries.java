package com.blackducksoftware.integration.hub.docker.v2.imageinspector.imageformat.docker.layerentry;

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
