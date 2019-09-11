/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2019 Synopsys, Inc.
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
package com.synopsys.integration.blackduck.dockerinspector.output;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.imageinspector.api.name.Names;

@Component
public class ContainerFilesystemFilename {
    private Config config;

    @Autowired
    public void setConfig(final Config config) {
        this.config = config;
    }

    public String deriveContainerFilesystemFilename() {
        final String containerFileSystemFilename;
        if (StringUtils.isBlank(config.getDockerPlatformTopLayerId())) {
            containerFileSystemFilename = Names.getContainerFileSystemTarFilename(config.getDockerImage(), config.getDockerTar());
        } else {
            containerFileSystemFilename = Names.getContainerFileSystemAppLayersTarFilename(config.getDockerImage(), config.getDockerTar());
        }
        return containerFileSystemFilename;
    }
}
