package com.blackducksoftware.integration.hub.docker.linux;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dir {
    private static final Logger logger = LoggerFactory.getLogger(Dir.class);

    // TODO Implement this using File.listFiles(filter)
    public static List<File> findFileWithName(final File dirFile, final String name) {
        logger.info(String.format("****** Looking in %s for %s", dirFile.getAbsolutePath(), name));
        final List<File> files = new ArrayList<>();
        if (StringUtils.compare(dirFile.getName(), name) == 0) {
            logger.trace("File Name ${name} found ${dirFile.getAbsolutePath()}");
            files.add(dirFile);
        } else if (dirFile.isDirectory()) {

            for (final File subFile : dirFile.listFiles()) {
                final List<File> foundFile = findFileWithName(subFile, name);
                if (foundFile != null) {
                    files.addAll(foundFile);
                }
            }
        }
        return files;
    }
}
