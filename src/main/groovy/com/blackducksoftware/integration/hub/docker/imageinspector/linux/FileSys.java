package com.blackducksoftware.integration.hub.docker.imageinspector.linux;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.hub.docker.imageinspector.PackageManagerEnum;

public class FileSys {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final File root;

    public FileSys(final File root) {
        this.root = root;
    }

    public Set<PackageManagerEnum> getPackageManagers() {
        final Set<PackageManagerEnum> packageManagers = new HashSet<>();

        logger.debug(String.format("Looking in root dir %s for lib dir", root.getAbsolutePath()));
        final List<File> libDirs = FileOperations.findDirWithName(root, "lib");
        if (libDirs != null) {
            for (final File libDir : libDirs) {
                for (final File packageManagerDirectory : libDir.listFiles()) {
                    logger.trace(String.format("Checking dir %s to see if it's a package manager dir", packageManagerDirectory.getAbsolutePath()));
                    try {
                        packageManagers.add(PackageManagerEnum.getPackageManagerEnumByName(packageManagerDirectory.getName()));
                    } catch (final IllegalArgumentException e) {
                        logger.trace(String.format("%s is not a package manager", packageManagerDirectory.getName()));
                    }
                }
            }
        }
        return packageManagers;
    }

    public void createTarGz(final File outputTarFile) throws CompressorException, IOException {
        FileOutputStream fOut = null;
        BufferedOutputStream bOut = null;
        GzipCompressorOutputStream gzOut = null;
        TarArchiveOutputStream tOut = null;
        try {
            fOut = new FileOutputStream(outputTarFile);
            bOut = new BufferedOutputStream(fOut);
            gzOut = new GzipCompressorOutputStream(bOut);
            tOut = new TarArchiveOutputStream(gzOut);
            tOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            addFileToTar(tOut, root, "");
        } finally {
            if (tOut != null) {
                tOut.finish();
                tOut.close();
            }
            if (gzOut != null) {
                gzOut.close();
            }
            if (bOut != null) {
                bOut.close();
            }
            if (fOut != null) {
                fOut.close();
            }
        }
    }

    private void addFileToTar(final TarArchiveOutputStream tOut, final File fileToAdd, final String base) throws IOException {
        final String entryName = base + fileToAdd.getName();
        final TarArchiveEntry tarEntry = new TarArchiveEntry(fileToAdd, entryName);
        tOut.putArchiveEntry(tarEntry);

        if (fileToAdd.isFile()) {
            try (final InputStream fileToAddInputStream = new FileInputStream(fileToAdd)) {
                IOUtils.copy(fileToAddInputStream, tOut);
            }
            tOut.closeArchiveEntry();
        } else {
            tOut.closeArchiveEntry();
            final File[] children = fileToAdd.listFiles();
            if (children != null) {
                for (final File child : children) {
                    logger.trace(String.format("Adding to tar.gz file: %s", child.getAbsolutePath()));
                    addFileToTar(tOut, child, entryName + "/");
                }
            }
        }
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, RecursiveToStringStyle.JSON_STYLE);
    }
}
