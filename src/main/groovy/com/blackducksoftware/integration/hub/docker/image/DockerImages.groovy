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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.client.ClassPathPropertiesFile
import com.blackducksoftware.integration.hub.docker.client.ProgramVersion

@Component
class DockerImages {
    private final Logger logger = LoggerFactory.getLogger(DockerImages.class)
	
	@Autowired
	ProgramVersion programVersion
	
    private Map<OperatingSystemEnum, DockerImage> dockerImageMap = new HashMap<>()
	private boolean initialized=false
	
	void init() {
		String programVersion = programVersion.getProgramVersion()
		dockerImageMap.put(OperatingSystemEnum.CENTOS, new DockerImage(OperatingSystemEnum.CENTOS, "blackducksoftware/hub-docker-inspector-centos", programVersion))
		dockerImageMap.put(OperatingSystemEnum.FEDORA, new DockerImage(OperatingSystemEnum.CENTOS, "blackducksoftware/hub-docker-inspector-centos", programVersion))
		dockerImageMap.put(OperatingSystemEnum.DEBIAN, new DockerImage(OperatingSystemEnum.UBUNTU, "blackducksoftware/hub-docker-inspector", programVersion))
		dockerImageMap.put(OperatingSystemEnum.UBUNTU, new DockerImage(OperatingSystemEnum.UBUNTU, "blackducksoftware/hub-docker-inspector", programVersion))
		dockerImageMap.put(OperatingSystemEnum.ALPINE, new DockerImage(OperatingSystemEnum.ALPINE, "blackducksoftware/hub-docker-inspector-alpine", programVersion))
		initialized=true
	}
	
    OperatingSystemEnum getDockerImageOs(OperatingSystemEnum targetImageOs) {
		if (!initialized) {
			init()
		}
        logger.debug("getDockerImageOs(${targetImageOs})")
        DockerImage image = dockerImageMap.get(targetImageOs)
        if (image == null) {
            null
        }
        image.os
    }

    String getDockerImageName(OperatingSystemEnum targetImageOs) {
		if (!initialized) {
			init()
		}
        logger.info("getDockerImageName(${targetImageOs})")
        DockerImage image = dockerImageMap.get(targetImageOs)
        if (image == null) {
            null
        }
        image.imageName
    }

    String getDockerImageVersion(OperatingSystemEnum targetImageOs) {
		if (!initialized) {
			init()
		}
        logger.info("getDockerImageVersion(${targetImageOs})")
        DockerImage image = dockerImageMap.get(targetImageOs)
        if (image == null) {
            null
        }
        image.imageVersion
    }
}
