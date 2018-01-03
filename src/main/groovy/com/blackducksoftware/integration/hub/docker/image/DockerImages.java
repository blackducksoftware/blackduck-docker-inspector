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
package com.blackducksoftware.integration.hub.docker.image;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum;
import com.blackducksoftware.integration.hub.docker.client.ProgramVersion;
import com.blackducksoftware.integration.hub.docker.config.Config;

@Component
public class DockerImages {
    private final Logger logger = LoggerFactory.getLogger(DockerImages.class);

    @Autowired
    ProgramVersion programVersion;

    @Autowired
    Config config;

    private final Map<OperatingSystemEnum, DockerImage> dockerImageMap = new HashMap<>();
    private boolean initialized = false;

    void init() throws IOException {
        if (initialized) {
            return;
        }
        final String programVersionString = programVersion.getProgramVersion();
        String repoWithSeparator = null;
        final String repo = config.getInspectorRepository();
        if (StringUtils.isBlank(repo)) {
            repoWithSeparator = "";
        } else if (StringUtils.isNotBlank(repo) && repo.endsWith("/")) {
            repoWithSeparator = repo;
        } else {
            repoWithSeparator = String.format("%s/", repo);
        }
        dockerImageMap.put(OperatingSystemEnum.CENTOS, new DockerImage(OperatingSystemEnum.CENTOS, String.format("%shub-docker-inspector-centos", repoWithSeparator), programVersionString));
        dockerImageMap.put(OperatingSystemEnum.FEDORA, new DockerImage(OperatingSystemEnum.CENTOS, String.format("%shub-docker-inspector-centos", repoWithSeparator), programVersionString));
        dockerImageMap.put(OperatingSystemEnum.DEBIAN, new DockerImage(OperatingSystemEnum.UBUNTU, String.format("%shub-docker-inspector-ubuntu", repoWithSeparator), programVersionString));
        dockerImageMap.put(OperatingSystemEnum.UBUNTU, new DockerImage(OperatingSystemEnum.UBUNTU, String.format("%shub-docker-inspector-ubuntu", repoWithSeparator), programVersionString));
        dockerImageMap.put(OperatingSystemEnum.ALPINE, new DockerImage(OperatingSystemEnum.ALPINE, String.format("%shub-docker-inspector-alpine", repoWithSeparator), programVersionString));
        initialized = true;
    }

    OperatingSystemEnum getDockerImageOs(final OperatingSystemEnum targetImageOs) throws IOException {
        init();
        logger.debug(String.format("getDockerImageOs(%s)", targetImageOs));
        final DockerImage image = dockerImageMap.get(targetImageOs);
        if (image == null) {
            return null;
        }
        return image.getOs();
    }

    public String getDockerImageName(final OperatingSystemEnum targetImageOs) throws IOException {
        logger.debug(String.format("getDockerImageName(%s)", targetImageOs));
        init();
        logger.info(String.format("getDockerImageName(%s)", targetImageOs));
        final DockerImage image = dockerImageMap.get(targetImageOs);
        if (image == null) {
            return null;
        }
        return image.getImageName();
    }

    public String getDockerImageVersion(final OperatingSystemEnum targetImageOs) throws IOException {
        logger.debug(String.format("getDockerImageVersion(%s)", targetImageOs));
        init();
        logger.info(String.format("getDockerImageVersion(%s)", targetImageOs));
        final DockerImage image = dockerImageMap.get(targetImageOs);
        if (image == null) {
            return null;
        }
        return image.getImageVersion();
    }
}
