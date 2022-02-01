/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.output;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.synopsys.integration.blackduck.imageinspector.image.common.archive.ImageLayerArchiveExtractor;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CompressedFile {
    private static final Logger logger = LoggerFactory.getLogger(CompressedFile.class);

    public static void gunZipUnTarFile(final File tarGzFile, final File tempTarFile, final File destinationDir) throws IOException {
        gunZipFile(tarGzFile, tempTarFile);
        unTarFile(tempTarFile, destinationDir);
    }

    public static void unTarFile(final File tarFile, final File destinationDir) throws IOException {
        // TODO this should call a simpler method, not an image-archive-specific method
        final ImageLayerArchiveExtractor tarExtractor = new ImageLayerArchiveExtractor();
        tarExtractor.extractLayerTarToDir(new FileOperations(), tarFile, destinationDir);
    }

    public static void gZipFile(final File fileToCompress, final File compressedFile) throws IOException {
        final byte[] buffer = new byte[1024];
        try (final FileOutputStream fileOutputStream = new FileOutputStream(compressedFile);
            final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fileOutputStream);
            final FileInputStream fileInputStream = new FileInputStream(fileToCompress)) {
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) > 0) {
                gzipOutputStream.write(buffer, 0, bytesRead);
            }
            gzipOutputStream.finish();
        }
    }

    public static void gunZipFile(final File gZippedFile, final File unCompressedFile) throws IOException {
        try (final FileInputStream fis = new FileInputStream(gZippedFile);
            final GZIPInputStream gZIPInputStream = new GZIPInputStream(fis);
            final FileOutputStream fos = new FileOutputStream(unCompressedFile)) {
            final byte[] buffer = new byte[1024];
            int len;
            while ((len = gZIPInputStream.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }
    }
}
