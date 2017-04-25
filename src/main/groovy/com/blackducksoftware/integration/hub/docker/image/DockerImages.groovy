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

import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum

@Component
class DockerImages {
    private Map<OperatingSystemEnum, DockerImage> dockerImageMap = new HashMap<>()

    DockerImages() {
        dockerImageMap.put(OperatingSystemEnum.CENTOS, new DockerImage(OperatingSystemEnum.CENTOS, "hub-docker-centos", "1.0"))
        dockerImageMap.put(OperatingSystemEnum.FEDORA, new DockerImage(OperatingSystemEnum.CENTOS, "hub-docker-centos", "1.0"))
        dockerImageMap.put(OperatingSystemEnum.DEBIAN, new DockerImage(OperatingSystemEnum.UBUNTU, "hub-docker-ubuntu", "1.0"))
        dockerImageMap.put(OperatingSystemEnum.UBUNTU, new DockerImage(OperatingSystemEnum.UBUNTU, "hub-docker-ubuntu", "1.0"))
        dockerImageMap.put(OperatingSystemEnum.ALPINE, new DockerImage(OperatingSystemEnum.ALPINE, "hub-docker-alpine", "1.0"))
    }

    OperatingSystemEnum getDockerImageOs(OperatingSystemEnum targetImageOs) {
        dockerImageMap.get(targetImageOs).os
    }

    String getDockerImageName(OperatingSystemEnum targetImageOs) {
        dockerImageMap.get(targetImageOs).imageName
    }

    String getDockerImageVersion(OperatingSystemEnum targetImageOs) {
        dockerImageMap.get(targetImageOs).imageVersion
    }
}
