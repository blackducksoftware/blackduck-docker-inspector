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
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.bdio.simple.BdioWriter
import com.blackducksoftware.integration.hub.bdio.simple.model.BdioComponent
import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum
import com.blackducksoftware.integration.hub.util.ProjectNameVersionGuess
import com.blackducksoftware.integration.hub.util.ProjectNameVersionGuesser

@Component
class YumExtractor extends Extractor {
    private final Logger logger = LoggerFactory.getLogger(YumExtractor.class)

    @PostConstruct
    void init() {
        initValues(PackageManagerEnum.YUM)
    }

    void extractComponents(BdioWriter bdioWriter, OperatingSystemEnum operatingSystem,  File yumdbDirectory) {


        // write all nodes bdioWriter.writeBdioNode(component)
    }

    void extractComponentFromDirectory(BdioWriter bdioWriter, OperatingSystemEnum operatingSystem, File componentDirectory){
        // extract from fileName
        String directoryName = componentDirectory.getName()
        String componentInfo = directoryName.substring(directoryName.indexOf("-") + 1)
        int indexBeforeArchitecture = componentInfo.lastIndexOf("-")
        String architecture =componentInfo.substring(0, indexBeforeArchitecture)
        componentInfo = componentInfo.substring(0, indexBeforeArchitecture)

        ProjectNameVersionGuesser guesser = new ProjectNameVersionGuesser()
        ProjectNameVersionGuess guess= guesser.guessNameAndVersion(componentInfo)
        String name = guess.getProjectName()
        String version = guess.getVersionName()

        String externalId = "$name/$version/$architecture"

        BdioComponent component = bdioNodeFactory.createComponent(name, version, null, operatingSystem.forge, externalId)


    }
}
