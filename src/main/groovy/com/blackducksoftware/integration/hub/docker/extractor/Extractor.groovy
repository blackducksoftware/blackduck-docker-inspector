package com.blackducksoftware.integration.hub.docker.extractor

import org.springframework.beans.factory.annotation.Value

import com.blackducksoftware.integration.hub.bdio.simple.BdioNodeFactory
import com.blackducksoftware.integration.hub.bdio.simple.BdioPropertyHelper
import com.blackducksoftware.integration.hub.bdio.simple.BdioWriter
import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum

abstract class Extractor {
    @Value('${filename.separator}')
    String filenameSeparator
    PackageManagerEnum packageManagerEnum
    final BdioPropertyHelper bdioPropertyHelper = new BdioPropertyHelper()
    final BdioNodeFactory bdioNodeFactory = new BdioNodeFactory(bdioPropertyHelper)

    abstract void init()
    abstract void extractComponents(BdioWriter bdioWriter, OperatingSystemEnum operatingSystem, File inputFile)

    void initValues(PackageManagerEnum packageManagerEnum) {
        this.packageManagerEnum = packageManagerEnum
    }

    boolean shouldAttemptExtract(File file) {
        packageManagerEnum.fileMatches(file)
    }

    void extract(File inputFile) {
        def (hubProjectName, hubProjectVersionName, forge, packageManager) = inputFile.name.split(filenameSeparator)
        OperatingSystemEnum operatingSystemEnum = OperatingSystemEnum.determineOperatingSystem(forge)
        extractComponents(operatingSystemEnum, inputFile)
    }
}
