package com.blackducksoftware.integration.hub.docker.linux;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dir {
    private static final Logger logger = LoggerFactory.getLogger(Dir.class);

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
}
