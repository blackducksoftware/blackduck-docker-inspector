/**
 * Hub Docker Inspector
 *
 * Copyright (C) 2017 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.hub.docker.image

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum
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
        dockerImageMap.put(OperatingSystemEnum.DEBIAN, new DockerImage(OperatingSystemEnum.UBUNTU, "blackducksoftware/hub-docker-inspector-ubuntu", programVersion))
        dockerImageMap.put(OperatingSystemEnum.UBUNTU, new DockerImage(OperatingSystemEnum.UBUNTU, "blackducksoftware/hub-docker-inspector-ubuntu", programVersion))
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