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
package com.blackducksoftware.integration.hub.docker;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.docker.imageinspector.OperatingSystemEnum;
import com.blackducksoftware.integration.hub.docker.imageinspector.config.Config;

@Component
public class InspectorImages {
    private final Logger logger = LoggerFactory.getLogger(InspectorImages.class);

    @Autowired
    ProgramVersion programVersion;

    @Autowired
    Config config;

    private final Map<OperatingSystemEnum, InspectorImage> inspectorImageMap = new HashMap<>();
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
        inspectorImageMap.put(OperatingSystemEnum.CENTOS, new InspectorImage(OperatingSystemEnum.CENTOS, String.format("%shub-docker-inspector-centos", repoWithSeparator), programVersionString));
        inspectorImageMap.put(OperatingSystemEnum.FEDORA, new InspectorImage(OperatingSystemEnum.CENTOS, String.format("%shub-docker-inspector-centos", repoWithSeparator), programVersionString));
        inspectorImageMap.put(OperatingSystemEnum.DEBIAN, new InspectorImage(OperatingSystemEnum.UBUNTU, String.format("%shub-docker-inspector-ubuntu", repoWithSeparator), programVersionString));
        inspectorImageMap.put(OperatingSystemEnum.UBUNTU, new InspectorImage(OperatingSystemEnum.UBUNTU, String.format("%shub-docker-inspector-ubuntu", repoWithSeparator), programVersionString));
        inspectorImageMap.put(OperatingSystemEnum.ALPINE, new InspectorImage(OperatingSystemEnum.ALPINE, String.format("%shub-docker-inspector-alpine", repoWithSeparator), programVersionString));
        initialized = true;
    }

    OperatingSystemEnum getInspectorImageOs(final OperatingSystemEnum targetImageOs) throws IOException {
        init();
        logger.debug(String.format("getInspectorImageOs(%s)", targetImageOs));
        final InspectorImage image = inspectorImageMap.get(targetImageOs);
        if (image == null) {
            return null;
        }
        return image.getOs();
    }

    public String getInspectorImageName(final OperatingSystemEnum targetImageOs) throws IOException {
        logger.debug(String.format("getInspectorImageName(%s)", targetImageOs));
        init();
        logger.info(String.format("getInspectorImageName(%s)", targetImageOs));
        final InspectorImage image = inspectorImageMap.get(targetImageOs);
        if (image == null) {
            return null;
        }
        return image.getImageName();
    }

    public String getInspectorImageTag(final OperatingSystemEnum targetImageOs) throws IOException {
        logger.debug(String.format("getInspectorImageTag(%s)", targetImageOs));
        init();
        logger.info(String.format("getInspectorImageTag(%s)", targetImageOs));
        final InspectorImage image = inspectorImageMap.get(targetImageOs);
        if (image == null) {
            return null;
        }
        return image.getImageVersion();
    }
}
