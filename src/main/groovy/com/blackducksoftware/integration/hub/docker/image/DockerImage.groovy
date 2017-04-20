/*
 * Copyright (C) 2017 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Black Duck Software.
 */
package com.blackducksoftware.integration.hub.docker.image
import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum

class DockerImage {
    private final OperatingSystemEnum os
    private final String imageName
    private final String imageVersion
    public DockerImage(OperatingSystemEnum os, String imageName, String imageVersion) {
        this.os = os
        this.imageName = imageName
        this.imageVersion = imageVersion
    }
}
