package com.blackducksoftware.integration.hub.docker.extractor

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.blackducksoftware.integration.hub.bdio.simple.BdioNodeFactory
import com.blackducksoftware.integration.hub.bdio.simple.BdioPropertyHelper
import com.blackducksoftware.integration.hub.bdio.simple.BdioWriter
import com.blackducksoftware.integration.hub.bdio.simple.model.BdioBillOfMaterials
import com.blackducksoftware.integration.hub.bdio.simple.model.BdioComponent
import com.blackducksoftware.integration.hub.bdio.simple.model.BdioProject
import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum
import com.blackducksoftware.integration.hub.docker.executor.Executor

abstract class Extractor {
    private final Logger logger = LoggerFactory.getLogger(Extractor.class)
    final BdioPropertyHelper bdioPropertyHelper = new BdioPropertyHelper()
    final BdioNodeFactory bdioNodeFactory = new BdioNodeFactory(bdioPropertyHelper)

    PackageManagerEnum packageManagerEnum
    Executor executor
    List<String> forges

    abstract void init()
    abstract java.util.List<BdioComponent> extractComponents(String[] packageList)

    void initValues(PackageManagerEnum packageManagerEnum,Executor executor, List<String> forges) {
        this.packageManagerEnum = packageManagerEnum
        this.executor = executor
        this.forges = forges
    }

    void extract(BdioWriter bdioWriter, OperatingSystemEnum operatingSystem, String codeLocationName, String projectName, String version) {
        BdioBillOfMaterials bom = bdioNodeFactory.createBillOfMaterials(codeLocationName, projectName, version)
        bdioWriter.writeBdioNode(bom)
        String externalId = "${projectName}/${version}"
        BdioProject projectNode = bdioNodeFactory.createProject(projectName, version, bdioPropertyHelper.createBdioId(projectName, version), operatingSystem.forge, externalId)
        List<BdioComponent> components = extractComponents(executor.listPackages())
        bdioPropertyHelper.addRelationships(projectNode, components)
        bdioWriter.writeBdioNode(projectNode)
        components.each { component ->
            bdioWriter.writeBdioNode(component)
        }
    }

    java.util.List<BdioComponent> createBdioComponent(String name, String version, String externalId){
        def components = []
        forges.each{ forge ->
            BdioComponent bdioComponent = bdioNodeFactory.createComponent(name, version, bdioPropertyHelper.createBdioId(name, version), forge, externalId)
            components.add(bdioComponent)
        }
        components
    }
}
