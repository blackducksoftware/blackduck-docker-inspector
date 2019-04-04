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
package com.synopsys.integration.blackduck.dockerinspector.programversion;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProgramVersion {
    private final Logger logger = LoggerFactory.getLogger(ProgramVersion.class);
    private String programVersion;
    private String inspectorImageFamily;
    private String inspectorImageVersion;

    @PostConstruct
    public void init() throws IOException {
        final ClassPathPropertiesFile versionProperties = new ClassPathPropertiesFile("version.properties");
        programVersion = versionProperties.getProperty("program.version");
        inspectorImageFamily = versionProperties.getProperty("inspector.image.family");
        inspectorImageVersion = versionProperties.getProperty("inspector.image.version");
        logger.debug(String.format("programVersion: %s", programVersion));
    }

    public String getProgramVersion() {
        return programVersion;
    }

    public String getInspectorImageFamily() {
        return inspectorImageFamily;
    }

    public String getInspectorImageVersion() {
        return inspectorImageVersion;
    }
}
