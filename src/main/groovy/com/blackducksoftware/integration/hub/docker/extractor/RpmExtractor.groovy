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
import com.blackducksoftware.integration.hub.docker.executor.RpmExecutor

@Component
class RpmExtractor extends Extractor {
    private final Logger logger = LoggerFactory.getLogger(RpmExtractor.class)

    @Autowired
    RpmExecutor executor

    @PostConstruct
    void init() {
        initValues(PackageManagerEnum.RPM, executor)
    }

    boolean valid(String packageLine) {
        packageLine.matches(".+-.+-.+\\..*")
    }

    BdioComponent[] extractComponents(OperatingSystemEnum operatingSystem, String[] packageList) {
        BdioComponent[] components = []
        packageList.each { packageLine ->
            if (valid(packageLine)) {
                def lastDotIndex = packageLine.lastIndexOf('.')
                def arch = packageLine.substring(lastDotIndex + 1)
                def lastDashIndex = packageLine.lastIndexOf('-')
                def nameVersion = packageLine.substring(0, lastDashIndex)
                def secondToLastDashIndex = nameVersion.lastIndexOf('-')

                def versionRelease = packageLine.substring(secondToLastDashIndex + 1, lastDotIndex)
                def artifact = packageLine.substring(0, secondToLastDashIndex)

                String externalId = "${artifact}/${versionRelease}/${arch}"

                BdioComponent bdioComponent = bdioNodeFactory.createComponent(artifact, versionRelease, null, operatingSystem.forge, externalId)
                components.add(bdioComponent)
            }
        }
        components
    }
}
