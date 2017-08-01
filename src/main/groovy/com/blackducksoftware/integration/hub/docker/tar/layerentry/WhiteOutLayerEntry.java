package com.blackducksoftware.integration.hub.docker.tar.layerentry;

import java.io.File;
import java.nio.file.Files;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.io.FileUtils;
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

}
