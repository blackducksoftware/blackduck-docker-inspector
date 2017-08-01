package com.blackducksoftware.integration.hub.docker.tar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.hub.docker.tar.layerentry.LayerEntries;
import com.blackducksoftware.integration.hub.docker.tar.layerentry.LayerEntry;

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
}
