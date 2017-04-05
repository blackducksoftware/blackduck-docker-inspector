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

    abstract void init()
    abstract List<BdioComponent> extractComponents(OperatingSystemEnum operatingSystem, String[] packageList)

    void initValues(PackageManagerEnum packageManagerEnum,Executor executor) {
        this.packageManagerEnum = packageManagerEnum
        this.executor = executor
    }

    void extract(BdioWriter bdioWriter, OperatingSystemEnum operatingSystem, String projectName, String version) {
        logger.info("Running extract")
        BdioBillOfMaterials bom = bdioNodeFactory.createBillOfMaterials(null, projectName, version)
        logger.info(bom.toString()  +" :: "+ bom.id)
        bdioWriter.writeBdioNode(bom)
        String externalId = "${projectName}/${version}"
        BdioProject projectNode = bdioNodeFactory.createProject(projectName, version, bdioPropertyHelper.createBdioId(projectName, version), operatingSystem.forge, externalId)
        logger.info(projectNode.toString()  +" :: "+ projectNode.id)
        List<BdioComponent> components = extractComponents(operatingSystem, executor.listPackages())
        bdioPropertyHelper.addRelationships(projectNode, components)
        bdioWriter.writeBdioNode(projectNode)
        components.each { component ->
            logger.info(component.toString()  +" :: "+ component.id)
            bdioWriter.writeBdioNode(component)
        }
    }
}
