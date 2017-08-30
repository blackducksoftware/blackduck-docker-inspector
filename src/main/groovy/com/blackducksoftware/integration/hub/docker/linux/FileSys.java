package com.blackducksoftware.integration.hub.docker.linux;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.hub.docker.PackageManagerEnum;

public class FileSys {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final File root;

    public FileSys(final File root) {
        this.root = root;
    }

    public Set<PackageManagerEnum> getPackageManagers() {
        final Set<PackageManagerEnum> packageManagers = new HashSet<>();

        logger.debug(String.format("Looking in root dir %s for lib dir", root.getAbsolutePath()));
        final List<File> libDirs = FileOperations.findFileWithName(root, "lib");
        if (libDirs != null) {
            for (final File libDir : libDirs) {
                for (final File packageManagerDirectory : libDir.listFiles()) {
                    logger.trace(String.format("Checking dir %s to see if it's a package manager dir", packageManagerDirectory.getAbsolutePath()));
                    try {
                        packageManagers.add(PackageManagerEnum.getPackageManagerEnumByName(packageManagerDirectory.getName()));
                    } catch (final IllegalArgumentException e) {
                        logger.trace(e.toString());
                    }
                }
            }
        }
        return packageManagers;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, RecursiveToStringStyle.JSON_STYLE);
    }
}
