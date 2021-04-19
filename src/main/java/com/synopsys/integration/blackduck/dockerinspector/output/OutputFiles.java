/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.output;

import java.io.File;

public class OutputFiles {
    private File bdioFile;
    private File containerFileSystemFile;
    private File squashedImageFile;

    public OutputFiles(final File bdioFile, final File containerFileSystemFile, final File squashedImageFile) {
        this.bdioFile = bdioFile;
        this.containerFileSystemFile = containerFileSystemFile;
        this.squashedImageFile = squashedImageFile;
    }

    public File getBdioFile() {
        return bdioFile;
    }

    public File getContainerFileSystemFile() {
        return containerFileSystemFile;
    }

    public File getSquashedImageFile() {
        return squashedImageFile;
    }
}
