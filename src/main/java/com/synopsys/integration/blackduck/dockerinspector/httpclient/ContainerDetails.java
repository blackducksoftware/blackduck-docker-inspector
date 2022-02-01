/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.httpclient;

public class ContainerDetails {
    private final String imageId;
    private final String containerId;

    public ContainerDetails(final String imageId, final String containerId) {
        this.imageId = imageId;
        this.containerId = containerId;
    }

    public String getImageId() {
        return imageId;
    }

    public String getContainerId() {
        return containerId;
    }
}
