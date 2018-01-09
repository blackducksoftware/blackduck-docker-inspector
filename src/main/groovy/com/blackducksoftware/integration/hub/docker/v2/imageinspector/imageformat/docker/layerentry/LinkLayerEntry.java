package com.blackducksoftware.integration.hub.docker.v2.imageinspector.imageformat.docker.layerentry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkLayerEntry implements LayerEntry {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final TarArchiveEntry layerEntry;
    private final File layerOutputDir;

    public LinkLayerEntry(final TarArchiveEntry layerEntry, final File layerOutputDir) {
        this.layerEntry = layerEntry;
        this.layerOutputDir = layerOutputDir;

    }

    @Override
    public void process() {
        final String fileSystemEntryName = layerEntry.getName();
        logger.trace(String.format("Processing link: %s", fileSystemEntryName));
        final Path layerOutputDirPath = layerOutputDir.toPath();
        Path startLink = null;
        try {
            startLink = Paths.get(layerOutputDir.getAbsolutePath(), fileSystemEntryName);
        } catch (final InvalidPathException e) {
            logger.warn(String.format("Error extracting symbolic link %s: Error creating Path object: %s", fileSystemEntryName, e.getMessage()));
            return;
        }
        Path endLink = null;
        logger.trace("Getting link name from layer entry");
        final String linkPath = layerEntry.getLinkName();
        logger.trace(String.format("layerEntry.getLinkName(): %s", linkPath));
        logger.trace("Checking link type");
        if (layerEntry.isSymbolicLink()) {
            logger.trace(String.format("%s is a symbolic link", layerEntry.getName()));
            logger.trace(String.format("Calculating endLink: startLink: %s; layerEntry.getLinkName(): %s", startLink.toString(), layerEntry.getLinkName()));
            if (linkPath.startsWith("/")) {
                final String relLinkPath = "." + linkPath;
                logger.trace(String.format("endLink made relative: %s", relLinkPath));
                endLink = layerOutputDirPath.resolve(relLinkPath);
            } else {
                endLink = startLink.resolveSibling(layerEntry.getLinkName());
            }
            logger.trace(String.format("normalizing %s", endLink.toString()));
            endLink = endLink.normalize();
            logger.trace(String.format("endLink: %s", endLink.toString()));
            try {
                Files.delete(startLink); // remove lower layer's version if exists
            } catch (final IOException e) {
                // expected (most of the time)
            }
            try {
                Files.createSymbolicLink(startLink, endLink);
            } catch (final IOException e) {
                final String msg = String.format("Error creating symbolic link from %s to %s; " + "this will not affect the results unless it affects a file needed by the package manager; " + "Error: %s", startLink.toString(),
                        endLink.toString(), e.getMessage());
                logger.warn(msg);
            }
        } else if (layerEntry.isLink()) {
            logger.trace(String.format("%s is a hard link", layerEntry.getName()));
            logger.trace(String.format("Calculating endLink: startLink: %s; layerEntry.getLinkName(): %s", startLink.toString(), layerEntry.getLinkName()));
            endLink = layerOutputDirPath.resolve(layerEntry.getLinkName());
            logger.trace(String.format("normalizing %s", endLink.toString()));
            endLink = endLink.normalize();
            logger.trace(String.format("endLink: %s", endLink.toString()));

            logger.trace(String.format("%s is a hard link: %s -> %s", layerEntry.getName(), startLink.toString(), endLink.toString()));
            final File targetFile = endLink.toFile();
            if (!targetFile.exists()) {
                logger.warn(String.format("Attempting to create a link to %s, but it does not exist", targetFile));
            }
            try {
                Files.createLink(startLink, endLink);
            } catch (final IOException e) {
                logger.warn(String.format("Error creating hard link from %s to %s; " + "this will not affect the results unless it affects a file needed by the package manager; " + "Error: %s", startLink.toString(), endLink.toString(),
                        e.getMessage()));
            }
        }

    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, RecursiveToStringStyle.JSON_STYLE);
    }
}
