package com.blackducksoftware.integration.hub.linux.extractor

import org.springframework.beans.factory.annotation.Value

import com.blackducksoftware.integration.hub.bdio.simple.model.BdioExternalIdentifier
import com.blackducksoftware.integration.hub.linux.BdioComponentDetails
import com.blackducksoftware.integration.hub.linux.ExtractionResult
import com.blackducksoftware.integration.hub.linux.OperatingSystemEnum
import com.blackducksoftware.integration.hub.linux.PackageManagerEnum

abstract class Extractor {
    @Value('${filename.separator}')
    String filenameSeparator

    PackageManagerEnum packageManagerEnum

    abstract void init()
    abstract List<BdioComponentDetails> extractComponents(OperatingSystemEnum operatingSystem, File inputFile)

    void initValues(PackageManagerEnum packageManagerEnum) {
        this.packageManagerEnum = packageManagerEnum
    }

    boolean shouldAttemptExtract(File file) {
        packageManagerEnum.fileMatches(file)
    }

    ExtractionResult extract(File inputFile) {
        def (hubProjectName, hubProjectVersionName, forge, packageManager) = inputFile.name.split(filenameSeparator)
        OperatingSystemEnum operatingSystemEnum = OperatingSystemEnum.determineOperatingSystem(forge)

        def components = extractComponents(operatingSystemEnum, inputFile)

        new ExtractionResult(hubProjectName: hubProjectName, hubProjectVersionName: hubProjectVersionName, bdioComponentDetailsList: components)
    }

    BdioComponentDetails createBdioComponentDetails(OperatingSystemEnum operatingSystemEnum, String name, String version, String externalId) {
        def externalIdentifier = createLinuxIdentifier(operatingSystemEnum, externalId)
        new BdioComponentDetails(name: name, version: version, externalIdentifier: externalIdentifier)
    }

    BdioExternalIdentifier createLinuxIdentifier(OperatingSystemEnum operatingSystemEnum, String externalId) {
        def externalIdentifier = new BdioExternalIdentifier()
        externalIdentifier.forge = operatingSystemEnum.forge
        externalIdentifier.externalId = externalId

        externalIdentifier
    }
}
