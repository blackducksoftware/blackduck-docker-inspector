/**
 * blackduck-docker-inspector
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
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
package com.synopsys.integration.blackduck.dockerinspector;

import com.synopsys.integration.blackduck.imageinspector.api.OperatingSystemEnum;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;

@Component
public class InspectorImages {

    @Autowired
    private ProgramVersion programVersion;

    @Autowired
    private Config config;

    private final Map<OperatingSystemEnum, InspectorImage> inspectorImageMap = new HashMap<>();

    @PostConstruct
    void init() {
        String repoWithSeparator;
        final String repo = config.getInspectorRepository();
        if (StringUtils.isBlank(repo)) {
            repoWithSeparator = "";
        } else if (StringUtils.isNotBlank(repo) && repo.endsWith("/")) {
            repoWithSeparator = repo;
        } else {
            repoWithSeparator = String.format("%s/", repo);
        }
        String inspectorImageFamily = config.getInspectorImageFamily();
        if (StringUtils.isBlank(inspectorImageFamily)) {
            inspectorImageFamily = programVersion.getInspectorImageFamily();
        }
        String inspectorImageVersion = config.getInspectorImageVersion();
        if (StringUtils.isBlank(inspectorImageVersion)) {
            inspectorImageVersion = programVersion.getInspectorImageVersion();
        }
        inspectorImageMap.put(OperatingSystemEnum.CENTOS, new InspectorImage(OperatingSystemEnum.CENTOS, String.format("%s%s-centos", repoWithSeparator, inspectorImageFamily), inspectorImageVersion));
        inspectorImageMap.put(OperatingSystemEnum.FEDORA, new InspectorImage(OperatingSystemEnum.CENTOS, String.format("%s%s-centos", repoWithSeparator, inspectorImageFamily), inspectorImageVersion));
        inspectorImageMap.put(OperatingSystemEnum.DEBIAN, new InspectorImage(OperatingSystemEnum.UBUNTU, String.format("%s%s-ubuntu", repoWithSeparator, inspectorImageFamily), inspectorImageVersion));
        inspectorImageMap.put(OperatingSystemEnum.UBUNTU, new InspectorImage(OperatingSystemEnum.UBUNTU, String.format("%s%s-ubuntu", repoWithSeparator, inspectorImageFamily), inspectorImageVersion));
        inspectorImageMap.put(OperatingSystemEnum.ALPINE, new InspectorImage(OperatingSystemEnum.ALPINE, String.format("%s%s-alpine", repoWithSeparator, inspectorImageFamily), inspectorImageVersion));
    }

    OperatingSystemEnum getInspectorImageOs(final OperatingSystemEnum targetImageOs) {
        final InspectorImage image = inspectorImageMap.get(targetImageOs);
        if (image == null) {
            return null;
        }
        return image.getOs();
    }

    public String getInspectorImageName(final OperatingSystemEnum targetImageOs) {
        final InspectorImage image = inspectorImageMap.get(targetImageOs);
        if (image == null) {
            return null;
        }
        return image.getImageName();
    }

    public String getInspectorImageTag(final OperatingSystemEnum targetImageOs) {
        final InspectorImage image = inspectorImageMap.get(targetImageOs);
        if (image == null) {
            return null;
        }
        return image.getImageVersion();
    }
}
