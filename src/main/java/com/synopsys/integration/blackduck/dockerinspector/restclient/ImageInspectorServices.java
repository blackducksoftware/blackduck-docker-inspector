/**
 * blackduck-docker-inspector
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
package com.synopsys.integration.blackduck.dockerinspector.restclient;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.exception.BlackDuckIntegrationException;
import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import com.synopsys.integration.exception.IntegrationException;

@Component
public class ImageInspectorServices {

    @Autowired
    private Config config;

    public int getImageInspectorHostPort(final ImageInspectorOsEnum imageInspectorOs) throws BlackDuckIntegrationException {
        if (ImageInspectorOsEnum.ALPINE.equals(imageInspectorOs)) {
            return config.getImageInspectorHostPortAlpine();
        }
        if (ImageInspectorOsEnum.CENTOS.equals(imageInspectorOs)) {
            return config.getImageInspectorHostPortCentos();
        }
        if (ImageInspectorOsEnum.UBUNTU.equals(imageInspectorOs)) {
            return config.getImageInspectorHostPortUbuntu();
        }
        throw new BlackDuckIntegrationException(String.format("Unrecognized ImageInspector OS: %s", imageInspectorOs.toString()));
    }

    public int getImageInspectorContainerPort(final ImageInspectorOsEnum imageInspectorOs) throws BlackDuckIntegrationException {
        if (ImageInspectorOsEnum.ALPINE.equals(imageInspectorOs)) {
            return config.getImageInspectorContainerPortAlpine();
        }
        if (ImageInspectorOsEnum.CENTOS.equals(imageInspectorOs)) {
            return config.getImageInspectorContainerPortCentos();
        }
        if (ImageInspectorOsEnum.UBUNTU.equals(imageInspectorOs)) {
            return config.getImageInspectorContainerPortUbuntu();
        }
        throw new BlackDuckIntegrationException(String.format("Unrecognized ImageInspector OS: %s", imageInspectorOs));
    }

    public int getDefaultImageInspectorHostPortBasedOnDistro() throws IntegrationException {
        final String inspectorOsName = config.getImageInspectorDefaultDistro();
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
        return ImageInspectorOsEnum.valueOf(config.getImageInspectorDefaultDistro().toUpperCase(Locale.US));
    }

    public String getDefaultImageInspectorOsName() {
        return getDefaultImageInspectorOs().name();
    }

}
