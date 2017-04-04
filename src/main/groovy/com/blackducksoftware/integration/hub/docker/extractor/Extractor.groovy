package com.blackducksoftware.integration.hub.docker.extractor

import com.blackducksoftware.integration.hub.bdio.simple.BdioNodeFactory
import com.blackducksoftware.integration.hub.bdio.simple.BdioPropertyHelper
import com.blackducksoftware.integration.hub.bdio.simple.BdioWriter
import com.blackducksoftware.integration.hub.bdio.simple.model.BdioComponent
import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum
import com.blackducksoftware.integration.hub.docker.executor.Executor

abstract class Extractor {
    final BdioPropertyHelper bdioPropertyHelper = new BdioPropertyHelper()
    final BdioNodeFactory bdioNodeFactory = new BdioNodeFactory(bdioPropertyHelper)
    PackageManagerEnum packageManagerEnum
    Executor executor

    abstract void init()
    abstract List<BdioComponent> extractComponents(OperatingSystemEnum operatingSystem, String[] packageList)

    void initValues(PackageManagerEnum packageManagerEnum,Executor executor) {
        this.packageManagerEnum = packageManagerEnum
        this.executor = executor
    }

    void extract(BdioWriter bdioWriter, OperatingSystemEnum operatingSystem, String projectName, String version) {
        bdioWriter.writeBdioNode(bdioNodeFactory.createBillOfMaterials(null, projectName, version))
        BdioComponent projectNode = bdioNodeFactory.createProject(projectName, version, "uuid:${UUID.randomUUID()}", null)
        List<BdioComponent> components = extractComponents(operatingSystem, executor.listPackages())
        bdioPropertyHelper.addRelationships(projectNode, components)
        bdioWriter.writeBdioNode(projectNode)
        components.each { bdioWriter.writeBdioNode(it) }
    }
}
