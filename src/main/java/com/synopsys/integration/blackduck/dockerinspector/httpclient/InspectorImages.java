/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.httpclient;

import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;
import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
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

    private final Map<ImageInspectorOsEnum, InspectorImage> inspectorImageMap = new HashMap<>();

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
        inspectorImageMap.put(ImageInspectorOsEnum.CENTOS, new InspectorImage(ImageInspectorOsEnum.CENTOS, String.format("%s%s-centos", repoWithSeparator, inspectorImageFamily), inspectorImageVersion));
        inspectorImageMap.put(ImageInspectorOsEnum.UBUNTU, new InspectorImage(ImageInspectorOsEnum.UBUNTU, String.format("%s%s-ubuntu", repoWithSeparator, inspectorImageFamily), inspectorImageVersion));
        inspectorImageMap.put(ImageInspectorOsEnum.ALPINE, new InspectorImage(ImageInspectorOsEnum.ALPINE, String.format("%s%s-alpine", repoWithSeparator, inspectorImageFamily), inspectorImageVersion));
    }

    public String getInspectorImageName(final ImageInspectorOsEnum targetImageOs) {
        final InspectorImage image = inspectorImageMap.get(targetImageOs);
        if (image == null) {
            return null;
        }
        return image.getImageName();
    }

    public String getInspectorImageTag(final ImageInspectorOsEnum targetImageOs) {
        final InspectorImage image = inspectorImageMap.get(targetImageOs);
        if (image == null) {
            return null;
        }
        return image.getImageVersion();
    }
}
