package com.blackducksoftware.integration.hub.docker.tar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.hub.exception.HubIntegrationException;

public class DockerLayerTar {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final File layerTar;

    public DockerLayerTar(final File layerTar) {
        this.layerTar = layerTar;
    }

    // TODO this method needs to be split up
    public void extractToDir(final File layerOutputDir) throws IOException {
        logger.info(String.format("*** layerTar: %s", layerTar.getAbsolutePath()));
        final TarArchiveInputStream layerInputStream = new TarArchiveInputStream(new FileInputStream(layerTar), "UTF-8");
        try {
            layerOutputDir.mkdirs();
            logger.debug(String.format("layerOutputDir: %s", layerOutputDir.getAbsolutePath()));
            final Path layerOutputDirPath = layerOutputDir.toPath();
            TarArchiveEntry layerEntry;
            while (null != (layerEntry = layerInputStream.getNextTarEntry())) {
                try {
                    final String fileSystemEntryName = layerEntry.getName();
                    logger.trace(String.format("Processing layerEntry: %s", fileSystemEntryName));
                    if ((fileSystemEntryName.startsWith(".wh.")) || (fileSystemEntryName.contains("/.wh."))) {
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
                        continue;
                    }
                    if (layerEntry.isSymbolicLink() || layerEntry.isLink()) {
                        logger.trace(String.format("Processing link: %s", fileSystemEntryName));
                        Path startLink = null;
                        try {
                            startLink = Paths.get(layerOutputDir.getAbsolutePath(), fileSystemEntryName);
                        } catch (final InvalidPathException e) {
                            logger.warn(String.format("Error extracting symbolic link %s: Error creating Path object: %s", fileSystemEntryName, e.getMessage()));
                            continue;
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
                                try {
                                    Files.delete(startLink); // remove lower layer's version if exists
                                } catch (final IOException e) {
                                    // expected (most of the time)
                                }
                                Files.createSymbolicLink(startLink, endLink);
                            } catch (final FileAlreadyExistsException e) {
                                final String msg = String.format(
                                        "FileAlreadyExistsException creating symbolic link from %s to %s; " + "this will not affect the results unless it affects a file needed by the package manager; " + "Error: %s", startLink.toString(),
                                        endLink.toString(), e.getMessage());
                                throw new HubIntegrationException(msg);
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
                            } catch (NoSuchFileException | FileAlreadyExistsException e) {
                                logger.warn(String.format("Error creating hard link from %s to %s; " + "this will not affect the results unless it affects a file needed by the package manager; " + "Error: %s", startLink.toString(),
                                        endLink.toString(), e.getMessage()));
                            }
                        }
                    } else {

                        logger.trace(String.format("Processing file/dir: %s", fileSystemEntryName));

                        final File outputFile = new File(layerOutputDir, fileSystemEntryName);
                        if (layerEntry.isFile()) {
                            logger.trace(String.format("Processing file: %s", fileSystemEntryName));
                            if (!outputFile.getParentFile().exists()) {
                                outputFile.getParentFile().mkdirs();
                            }
                            logger.trace(String.format("Creating output stream for %s", outputFile.getName()));
                            final OutputStream outputFileStream = new FileOutputStream(outputFile);
                            try {
                                IOUtils.copy(layerInputStream, outputFileStream);
                            } finally {
                                outputFileStream.close();
                            }
                        } else {
                            outputFile.mkdirs();
                        }

                    }
                } catch (final Exception e) {
                    logger.error(String.format("Error extracting files from layer tar: %s", e.toString()));
                }
            }
        } finally {
            IOUtils.closeQuietly(layerInputStream);
        }
    }
}
