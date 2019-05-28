package com.synopsys.integration.blackduck.dockerinspector.output;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompressedFile {
    private static final Logger logger = LoggerFactory.getLogger(CompressedFile.class);

    public static void unpackTarGz(final File fileToUnpack, final File destinationDir) {

    }

    public static void gZipFile(final File fileToCompress, final File compressedFile) throws IOException {
        final byte[] buffer = new byte[1024];
        final FileOutputStream fileOutputStream = new FileOutputStream(compressedFile);
        final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fileOutputStream);
        final FileInputStream fileInputStream = new FileInputStream(fileToCompress);
        int bytesRead;
        while ((bytesRead = fileInputStream.read(buffer)) > 0) {
            gzipOutputStream.write(buffer, 0, bytesRead);
        }
        fileInputStream.close();
        gzipOutputStream.finish();;
        gzipOutputStream.close();
    }

    public static void gunZipFile(final File gZippedFile, final File unCompressedFile) throws IOException {
        final FileInputStream fis = new FileInputStream(gZippedFile);
        final GZIPInputStream gZIPInputStream = new GZIPInputStream(fis);
        final FileOutputStream fos = new FileOutputStream(unCompressedFile);
        final byte[] buffer = new byte[1024];
        int len;
        while ((len = gZIPInputStream.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
        }
        fos.close();
        gZIPInputStream.close();
    }
}
