/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.httpclient;

import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;

public class InspectorImage {
    private final ImageInspectorOsEnum os;
    private final String imageName;
    private final String imageVersion;

    public InspectorImage(final ImageInspectorOsEnum os, final String imageName, final String imageVersion) {
        this.os = os;
        this.imageName = imageName;
        this.imageVersion = imageVersion;
    }

    ImageInspectorOsEnum getOs() {
        return os;
    }

    String getImageName() {
        return imageName;
    }

    String getImageVersion() {
        return imageVersion;
    }

}
