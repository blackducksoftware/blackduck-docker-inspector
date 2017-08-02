package com.blackducksoftware.integration.hub.docker.linux;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dir {
    private static final Logger logger = LoggerFactory.getLogger(Dir.class);

    public static List<File> findFileWithName(final File dirFile, final String name) {
        logger.info(String.format("Looking in %s for %s", dirFile.getAbsolutePath(), name));

        final File[] fileArray = dirFile.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.matches("^" + name + "$");
            }
        });
        final List<File> fileList = Arrays.asList(fileArray);
        return fileList;
    }
}
