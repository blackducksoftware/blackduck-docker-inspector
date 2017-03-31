package com.blackducksoftware.integration.hub.docker.extractor

import com.blackducksoftware.integration.hub.bdio.simple.BdioNodeFactory
import com.blackducksoftware.integration.hub.bdio.simple.BdioPropertyHelper
import com.blackducksoftware.integration.hub.bdio.simple.BdioWriter
import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum
import com.blackducksoftware.integration.hub.docker.creator.Executor

abstract class Extractor {
    final BdioPropertyHelper bdioPropertyHelper = new BdioPropertyHelper()
    final BdioNodeFactory bdioNodeFactory = new BdioNodeFactory(bdioPropertyHelper)
    PackageManagerEnum packageManagerEnum
    Executor creator

    abstract void init()
    abstract void extractComponents(BdioWriter bdioWriter, OperatingSystemEnum operatingSystem, String[] packageList)

    abstract void extractComponentRelationships(String packageName)

    void initValues(PackageManagerEnum packageManagerEnum,Executor creator) {
        this.packageManagerEnum = packageManagerEnum
        this.creator = creator
    }

    void extract(BdioWriter bdioWriter, OperatingSystemEnum operatingSystem) {
        extractComponents(bdioWriter, operatingSystem, creator.listPackages())
    }
}
