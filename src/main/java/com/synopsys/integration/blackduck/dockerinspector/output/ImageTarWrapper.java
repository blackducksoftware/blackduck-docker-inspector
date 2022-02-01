/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.output;

import java.io.File;

public class ImageTarWrapper {

    private final File file;
    private final String imageRepo;
    private final String imageTag;

    public ImageTarWrapper(final File file, final String imageRepo, final String imageTag) {
        this.file = file;
        this.imageRepo = imageRepo;
        this.imageTag = imageTag;
    }

    public ImageTarWrapper(final File file) {
        this.file = file;
        this.imageRepo = null;
        this.imageTag = null;
    }

    public File getFile() {
        return file;
    }

    public String getImageRepo() {
        return imageRepo;
    }

    public String getImageTag() {
        return imageTag;
    }
}
