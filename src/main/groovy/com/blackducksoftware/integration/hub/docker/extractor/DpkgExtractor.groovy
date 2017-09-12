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
package com.blackducksoftware.integration.hub.docker.extractor


import javax.annotation.PostConstruct

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.bdio.simple.DependencyNodeBuilder
import com.blackducksoftware.integration.hub.bdio.simple.model.BdioComponent
import com.blackducksoftware.integration.hub.bdio.simple.model.DependencyNode
import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum
import com.blackducksoftware.integration.hub.docker.executor.DpkgExecutor

@Component
class DpkgExtractor extends Extractor {
    private final Logger logger = LoggerFactory.getLogger(DpkgExtractor.class)

    @Autowired
    DpkgExecutor executor

    @PostConstruct
    void init() {
        def forges = [
            OperatingSystemEnum.DEBIAN.forge,
            OperatingSystemEnum.UBUNTU.forge
        ]
        initValues(PackageManagerEnum.DPKG, executor, forges)
    }

    ExtractionResults extractComponents(String dockerImageRepo, String dockerImageTag, ExtractionDetails extractionDetails, String[] packageList) {
        final List<BdioComponent> components = new ArrayList<>();
        final DependencyNode rootNode = createDependencyNode(OperatingSystemEnum.UBUNTU.forge, dockerImageRepo, dockerImageTag, extractionDetails.architecture);
        final DependencyNodeBuilder dNodeBuilder = new DependencyNodeBuilder(rootNode);
        boolean startOfComponents = false
        packageList.each { packageLine ->
            if (packageLine != null) {
                if (packageLine.matches("\\+\\+\\+-=+-=+-=+-=+")) {
                    startOfComponents = true
                } else if (startOfComponents){
                    char packageStatus = packageLine.charAt(1)
                    if (isInstalledStatus(packageStatus)) {
                        String componentInfo = packageLine.substring(3)
                        def(name,version,architecture,description) = componentInfo.tokenize(" ")
                        if (name.contains(":")) {
                            name = name.substring(0, name.indexOf(":"))
                        }
                        String externalId = "$name/$version/$architecture"

                        createBdioComponent(dNodeBuilder, rootNode, components, name, version, externalId, extractionDetails.architecture)
                    } else {
                        logger.debug("Package \"${packageLine}\" is listed but not installed (package status: ${packageStatus})")
                    }
                }
            }
        }
        logger.trace(String.format("DependencyNode tree: %s", rootNode));
        return new ExtractionResults(components, rootNode);
    }

    boolean isInstalledStatus(Character packageStatus) {
        String packageStatusString = packageStatus.toString()
        if ("iWt".contains(packageStatusString)) {
            return true
        }
        false
    }
}