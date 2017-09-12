package com.blackducksoftware.integration.hub.docker.linux;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileOperations {
    private static final Logger logger = LoggerFactory.getLogger(FileOperations.class);

    public static List<File> findFileWithName(final File dirFile, final String targetName) {
        logger.trace(String.format("Looking in %s for %s", dirFile.getAbsolutePath(), targetName));
        final List<File> results = new ArrayList<>();

        try (Stream<Path> stream = Files.find(dirFile.toPath(), 100, (path, attr) -> path.getFileName().toString().equals(targetName))) {
            stream.forEach(path -> results.add(path.toFile()));
        } catch (final IOException e) {
            e.printStackTrace();
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
}
