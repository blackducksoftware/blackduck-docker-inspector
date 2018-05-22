/**
 * hub-docker-inspector
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
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
package com.blackducksoftware.integration.hub.docker.dockerinspector.restclient;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.docker.dockerinspector.config.Config;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.imageinspector.api.ImageInspectorOsEnum;
import com.blackducksoftware.integration.hub.imageinspector.lib.OperatingSystemEnum;

@Component
public class ImageInspectorServices {

    @Autowired
    private Config config;

    public int getImageInspectorHostPort(final OperatingSystemEnum imageInspectorOs) throws HubIntegrationException {
        final String imageInspectorOsName = imageInspectorOs.name();
        if ("alpine".equalsIgnoreCase(imageInspectorOsName)) {
            return config.getImageInspectorHostPortAlpine();
        }
        if ("centos".equalsIgnoreCase(imageInspectorOsName)) {
            return config.getImageInspectorHostPortCentos();
        }
        if ("ubuntu".equalsIgnoreCase(imageInspectorOsName)) {
            return config.getImageInspectorHostPortUbuntu();
        }
        throw new HubIntegrationException(String.format("Unrecognized ImageInspector OS name: %s", imageInspectorOsName));
    }

    public int getImageInspectorContainerPort(final OperatingSystemEnum imageInspectorOs) throws HubIntegrationException {
        final String imageInspectorOsName = imageInspectorOs.name();
        if ("alpine".equalsIgnoreCase(imageInspectorOsName)) {
            return config.getImageInspectorContainerPortAlpine();
        }
        if ("centos".equalsIgnoreCase(imageInspectorOsName)) {
            return config.getImageInspectorContainerPortCentos();
        }
        if ("ubuntu".equalsIgnoreCase(imageInspectorOsName)) {
            return config.getImageInspectorContainerPortUbuntu();
        }
        throw new HubIntegrationException(String.format("Unrecognized ImageInspector OS name: %s", imageInspectorOsName));
    }

    public int getDefaultImageInspectorHostPort() throws IntegrationException {
        final String inspectorOsName = config.getImageInspectorDefault();
        if ("alpine".equalsIgnoreCase(inspectorOsName)) {
            return config.getImageInspectorHostPortAlpine();
        }
        if ("centos".equalsIgnoreCase(inspectorOsName)) {
            return config.getImageInspectorHostPortCentos();
        }
        if ("ubuntu".equalsIgnoreCase(inspectorOsName)) {
            return config.getImageInspectorHostPortUbuntu();
        }
        throw new IntegrationException(String.format("Invalid value for property image.inspector.default: %s", inspectorOsName));
    }

    public ImageInspectorOsEnum getDefaultImageInspectorOs() {
        return ImageInspectorOsEnum.valueOf(config.getImageInspectorDefault().toUpperCase(Locale.US));
    }

    public String getDefaultImageInspectorOsName() {
        return getDefaultImageInspectorOs().name();
    }

}
