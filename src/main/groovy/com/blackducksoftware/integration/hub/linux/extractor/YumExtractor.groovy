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
class YumExtractor extends Extractor {
    private final Logger logger = LoggerFactory.getLogger(YumExtractor.class)

    @PostConstruct
    void init() {
        initValues(PackageManagerEnum.YUM)
    }

    List<BdioComponentDetails> extractComponents(OperatingSystemEnum operatingSystem,  File yumOutput) {
        def components = []
        boolean startOfComponents = false

        def componentColumns = []
        yumOutput.eachLine { line ->
            if (line != null) {
                if ('Installed Packages' == line) {
                    startOfComponents = true
                } else if (startOfComponents) {
                    componentColumns.addAll(line.tokenize(' '))
                    if ((componentColumns.size() == 3) && (!line.startsWith("Loaded plugins:"))) {
                        String nameArch = componentColumns.get(0)
                        String version = componentColumns.get(1)
                        String name =nameArch.substring(0, nameArch.lastIndexOf("."))
                        String architecture = nameArch.substring(nameArch.lastIndexOf(".") + 1)

                        String externalId = "$name/$version/$architecture"
                        components.add(createBdioComponentDetails(operatingSystem, name, version, externalId))
                        componentColumns = []
                    } else  if (componentColumns.size() > 3) {
                        logger.error("Parsing multi-line components has failed. $line")
                        componentColumns = []
                    }
                }
            }
        }

        components
    }
}
