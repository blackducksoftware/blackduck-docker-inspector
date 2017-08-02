package com.blackducksoftware.integration.hub.docker.linux;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        ///// TODO: This seems dumb to loop and recurse; just look recursively under root
        for (final File rootLevelDir : root.listFiles()) {
            logger.info(String.format("========= *** Looking in rootLevelDir %s for lib dir", rootLevelDir.getAbsolutePath()));
            final List<File> libDirs = Dir.findFileWithName(rootLevelDir, "lib");
            if (libDirs != null) {
                for (final File libDir : libDirs) {
                    for (final File packageManagerDirectory : libDir.listFiles()) {
                        try {
                            packageManagers.add(PackageManagerEnum.getPackageManagerEnumByName(packageManagerDirectory.getName()));
                        } catch (final IllegalArgumentException e) {
                            logger.trace(e.toString());
                        }
                    }
                }
            }
        }
        return packageManagers;
    }
}
