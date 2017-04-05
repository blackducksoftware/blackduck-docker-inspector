/*
 * Copyright (C) 2017 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Black Duck Software.
 */
package com.blackducksoftware.integration.hub.docker.extractor

import javax.annotation.PostConstruct

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.bdio.simple.model.BdioComponent
import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum
import com.blackducksoftware.integration.hub.docker.executor.YumExecutor

@Component
class YumExtractor extends Extractor {
    private final Logger yumLogger = LoggerFactory.getLogger(YumExtractor.class)

    @Autowired
    YumExecutor executor

    @PostConstruct
    void init() {
        def forges = [
            OperatingSystemEnum.CENTOS.forge,
            OperatingSystemEnum.FEDORA.forge
        ]
        initValues(PackageManagerEnum.YUM, executor, forges)
    }

    List<BdioComponent> extractComponents(String[] packageList) {
        def components = []
        boolean startOfComponents = false
        def componentColumns = []
        packageList.each { packageLine ->
            if (packageLine != null) {
                if ('Installed Packages' == packageLine) {
                    startOfComponents = true
                } else if (startOfComponents) {
                    componentColumns.addAll(packageLine.tokenize(' '))
                    if ((componentColumns.size() == 3) && (!packageLine.startsWith("Loaded plugins:"))) {
                        String nameArch = componentColumns.get(0)
                        String version = componentColumns.get(1)
                        String name =nameArch.substring(0, nameArch.lastIndexOf("."))
                        String architecture = nameArch.substring(nameArch.lastIndexOf(".") + 1)

                        String externalId = "$name/$version/$architecture"

                        components.addAll(createBdioComponent(name, version, externalId))

                        componentColumns = []
                    } else  if (componentColumns.size() > 3) {
                        yumLogger.error("Parsing multi-line components has failed. $packageLine")
                        componentColumns = []
                    }
                }
            }
        }
        components
    }
}
