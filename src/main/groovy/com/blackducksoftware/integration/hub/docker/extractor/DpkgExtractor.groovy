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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.bdio.simple.model.BdioComponent
import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum
import com.blackducksoftware.integration.hub.docker.executor.DpkgExecutor

@Component
class DpkgExtractor extends Extractor {

    @Autowired
    DpkgExecutor executor

    @PostConstruct
    void init() {
        initValues(PackageManagerEnum.DPKG, executor)
    }

    List<BdioComponent> extractComponents(OperatingSystemEnum operatingSystem, String[] packageList) {
        def components = []
        boolean startOfComponents = false
        packageList.each { packageLine ->
            if (packageLine != null) {
                if (packageLine.matches("\\+\\+\\+-=+-=+-=+-=+")) {
                    startOfComponents = true
                } else if (startOfComponents){
                    String componentInfo = packageLine.substring(3)
                    def(name,version,architecture,description) = componentInfo.tokenize(" ")

                    String externalId = "$name/$version/$architecture"

                    BdioComponent bdioComponent = bdioNodeFactory.createComponent(name, version, null, operatingSystem.forge, externalId)
                    components.add(bdioComponent)
                }
            }
        }
        components
    }
}
