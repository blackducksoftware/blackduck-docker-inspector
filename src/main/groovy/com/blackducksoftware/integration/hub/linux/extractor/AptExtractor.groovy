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
package com.blackducksoftware.integration.hub.linux.extractor

import javax.annotation.PostConstruct

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.linux.BdioComponentDetails
import com.blackducksoftware.integration.hub.linux.OperatingSystemEnum
import com.blackducksoftware.integration.hub.linux.PackageManagerEnum

@Component
class AptExtractor extends Extractor {
    private final Logger logger = LoggerFactory.getLogger(AptExtractor.class)

    @PostConstruct
    void init() {
        initValues(PackageManagerEnum.APT)
    }

    @Override
    List<BdioComponentDetails> extractComponents(OperatingSystemEnum operatingSystem, File inputFile) {
        def components = []

        inputFile.eachLine { line ->
            extract(operatingSystem,components, line)
        }

        components
    }

    void extract(OperatingSystemEnum operatingSystem, List<BdioComponentDetails> components, String inputLine) {
        if (inputLine.contains(' ')) {
            def (packageName, version) = inputLine.split(' ')
            def index = packageName.indexOf('/')
            if (index > 0) {
                def component = packageName.substring(0, index)
                String externalId = "${component}/${version}"
                components.add(createBdioComponentDetails(operatingSystem, component, version, externalId))
            }
        }
    }
}
