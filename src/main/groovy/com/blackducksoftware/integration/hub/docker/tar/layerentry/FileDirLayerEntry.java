package com.blackducksoftware.integration.hub.docker.tar.layerentry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileDirLayerEntry implements LayerEntry {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final TarArchiveInputStream layerInputStream;
    private final TarArchiveEntry layerEntry;
    private final File layerOutputDir;

    public FileDirLayerEntry(final TarArchiveInputStream layerInputStream, final TarArchiveEntry layerEntry, final File layerOutputDir) {
        this.layerInputStream = layerInputStream;
        this.layerEntry = layerEntry;
        this.layerOutputDir = layerOutputDir;
    }

    @Override
    public void process() {
        final String fileSystemEntryName = layerEntry.getName();
        logger.trace(String.format("Processing file/dir: %s", fileSystemEntryName));

        final File outputFile = new File(layerOutputDir, fileSystemEntryName);
        if (layerEntry.isFile()) {
            logger.trace(String.format("Processing file: %s", fileSystemEntryName));
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }
            logger.trace(String.format("Creating output stream for %s", outputFile.getAbsolutePath()));
            OutputStream outputFileStream = null;
            try {
                outputFileStream = new FileOutputStream(outputFile);
            } catch (final FileNotFoundException e1) {
                logger.error(String.format("Error creating output stream for %s", outputFile.getAbsolutePath()), e1);
                return;
            }
            try {
                IOUtils.copy(layerInputStream, outputFileStream);
            } catch (final IOException e) {
                logger.error(String.format("Error copying file %s to %s: %s", fileSystemEntryName, outputFile.getAbsolutePath(), e.getMessage()));
                return;
            } finally {
                if (outputFileStream != null) {
                    try {
                        outputFileStream.close();
                    } catch (final IOException e) {
                        logger.error(String.format("Error closing output file stream for: %s: %s", outputFile.getAbsolutePath(), e.getMessage()));
                    }
                }
            }
        } else {
            final boolean mkdirSucceeded = outputFile.mkdirs();
            if (!mkdirSucceeded) {
                logger.trace(String.format("mkdir of %s didn't succeed, but it might have already existed", outputFile.getAbsolutePath()));
            }
        }

    }

}
