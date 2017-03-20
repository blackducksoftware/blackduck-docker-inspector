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

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.blackducksoftware.integration.hub.linux.BdioComponentDetails
import com.blackducksoftware.integration.hub.linux.OperatingSystemEnum
import com.blackducksoftware.integration.hub.linux.PackageManagerEnum
import com.blackducksoftware.integration.hub.linux.extractor.data.DpkgStatusFilePackage

class DpkgStatusFileExtractor extends Extractor {
    private final Logger logger = LoggerFactory.getLogger(DpkgStatusFileExtractor.class)

    @PostConstruct
    void init() {
        initValues(PackageManagerEnum.DPKG_STATUS_FILE)
    }

    List<BdioComponentDetails> extractComponents(OperatingSystemEnum operatingSystem, File yumOutput) {
        def components = []
        boolean startOfComponents = false

        int packageSeparators = 0;
        //TODO read Depends line to determine component relationships
        DpkgStatusFilePackage dpkgPackage = new DpkgStatusFilePackage()
        yumOutput.eachLine { line ->
            if (line != null) {
                if (StringUtils.isBlank(line)) {
                    if (!dpkgPackage.isEmpty()) {
                        logger.error("Component was missing information : ${dpkgPackage.toString()}")
                    }
                    dpkgPackage = new DpkgStatusFilePackage()
                    packageSeparators++
                } else if (line.contains('Package:')) {
                    def name = line.replace('Package:', '')
                    dpkgPackage.name = name.trim()
                } else if (line.contains('Architecture:')) {
                    def architecture = line.replace('Architecture:', '')
                    dpkgPackage.architecture = architecture.trim()
                } else if (line.contains('Version:')) {
                    def version = line.replace('Version:', '')
                    dpkgPackage.version = version.trim()
                } else if (line.contains('Status:')) {
                    if (line.contains('Status: install ok installed')) {
                        dpkgPackage.installed = true
                    } else {
                        logger.error("This Component was not installed successfully : ${dpkgPackage.toString()}")
                        logger.error("$line")
                        dpkgPackage.installed = true
                    }
                } else if (dpkgPackage.isComplete()){
                    if(dpkgPackage.installed){
                        components.add(createBdioComponentDetails(operatingSystem, dpkgPackage.name, dpkgPackage.version, dpkgPackage.getExternalId()))
                    }
                    dpkgPackage = new DpkgStatusFilePackage()
                }
            }
        }
        logger.debug("Package Separators : $packageSeparators")
        components
    }
}
