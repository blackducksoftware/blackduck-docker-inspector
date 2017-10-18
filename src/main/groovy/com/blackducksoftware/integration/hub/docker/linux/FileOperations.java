package com.blackducksoftware.integration.hub.docker.linux;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileOperations {
    private static final Logger logger = LoggerFactory.getLogger(FileOperations.class);

    public static List<File> findDirWithName(final File dirFile, final String targetName) {
        final List<File> results = new ArrayList<>();
        logger.trace(String.format("Looking in %s for Dir %s", dirFile.getAbsolutePath(), targetName));
        final IOFileFilter fileFilter = new NameFileFilter(targetName);
        final IOFileFilter dirFilter = TrueFileFilter.INSTANCE;
        final Iterator<File> iter = FileUtils.iterateFilesAndDirs(dirFile, fileFilter, dirFilter);
        while (iter.hasNext()) {
            final File f = iter.next();
            if (targetName.equals(f.getName()) && (f.isDirectory())) {
                logger.trace(String.format("Match: %s", f.getAbsolutePath()));
                results.add(f);
            }
        }
        return results;
    }

    public static List<File> findFilesWithExt(final File dirFile, final String fileExtension) {
        final List<File> results = new ArrayList<>();
        logger.trace(String.format("Looking in %s for files with extension %s", dirFile.getAbsolutePath(), fileExtension));
        final IOFileFilter fileFilter = new WildcardFileFilter("*." + fileExtension);
        final IOFileFilter dirFilter = TrueFileFilter.INSTANCE;
        final Iterator<File> iter = FileUtils.iterateFilesAndDirs(dirFile, fileFilter, dirFilter);
        while (iter.hasNext()) {
            final File f = iter.next();
            if (f.isFile()) {
                logger.trace(String.format("Match: %s", f.getAbsolutePath()));
                results.add(f);
            }
        }
        return results;
    }

    public static void copyFile(final File fileToCopy, final File destination) throws IOException {
        final String filename = fileToCopy.getName();
        logger.debug(String.format("Copying %s to %s", fileToCopy.getAbsolutePath(), destination.getAbsolutePath()));
        final Path destPath = destination.toPath().resolve(filename);
        Files.copy(fileToCopy.toPath(), destPath);
    }

    public static void moveFile(final File fileToMove, final File destination) throws IOException {
        final String filename = fileToMove.getName();
        logger.debug(String.format("Moving %s to %s", fileToMove.getAbsolutePath(), destination.getAbsolutePath()));
        final Path destPath = destination.toPath().resolve(filename);
        Files.move(fileToMove.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void copyDirContentsToDir(final String fromDirPath, final String toDirPath, final boolean createIfNecessary) throws IOException {
        final File srcDir = new File(fromDirPath);
        final File destDir = new File(toDirPath);
        if (createIfNecessary && !destDir.exists()) {
            destDir.mkdirs();
        }
        FileUtils.copyDirectory(srcDir, destDir);
    }
}
