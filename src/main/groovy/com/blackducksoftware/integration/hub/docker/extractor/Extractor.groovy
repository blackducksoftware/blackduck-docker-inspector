package com.blackducksoftware.integration.hub.docker.extractor

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.blackducksoftware.integration.hub.bdio.simple.BdioNodeFactory
import com.blackducksoftware.integration.hub.bdio.simple.BdioPropertyHelper
import com.blackducksoftware.integration.hub.bdio.simple.BdioWriter
import com.blackducksoftware.integration.hub.bdio.simple.model.BdioBillOfMaterials
import com.blackducksoftware.integration.hub.bdio.simple.model.BdioComponent
import com.blackducksoftware.integration.hub.bdio.simple.model.BdioExternalIdentifier
import com.blackducksoftware.integration.hub.bdio.simple.model.BdioProject
import com.blackducksoftware.integration.hub.bdio.simple.model.externalid.ExternalId
import com.blackducksoftware.integration.hub.bdio.simple.model.externalid.NameVersionExternalId
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum
import com.blackducksoftware.integration.hub.docker.executor.Executor
import com.blackducksoftware.integration.util.IntegrationEscapeUtil
import org.apache.commons.lang3.StringUtils

abstract class Extractor {
    private final Logger logger = LoggerFactory.getLogger(Extractor.class)
	private final IntegrationEscapeUtil integrationEscapeUtil = new IntegrationEscapeUtil()
    final BdioPropertyHelper bdioPropertyHelper = new BdioPropertyHelper()
    final BdioNodeFactory bdioNodeFactory = new BdioNodeFactory(bdioPropertyHelper)

    PackageManagerEnum packageManagerEnum
    Executor executor
    List<String> forges

    abstract void init()
    abstract java.util.List<BdioComponent> extractComponents(ExtractionDetails extractionDetails, String[] packageList)

    void initValues(PackageManagerEnum packageManagerEnum,Executor executor, List<String> forges) {
        this.packageManagerEnum = packageManagerEnum
        this.executor = executor
        this.forges = forges
    }

    void extract(BdioWriter bdioWriter, ExtractionDetails extractionDetails, String codeLocationName, String projectName, String version) {
        BdioBillOfMaterials bom = bdioNodeFactory.createBillOfMaterials(codeLocationName, projectName, version)
        bdioWriter.writeBdioNode(bom)
		String externalId = getExternalId(projectName, version)
		String bdioId = bdioPropertyHelper.createExternalIdentifier(extractionDetails.operatingSystem.forge, externalId)
        BdioProject projectNode = bdioNodeFactory.createProject(projectName, version, bdioId, extractionDetails.operatingSystem.forge, externalId)
        List<BdioComponent> components = extractComponents(extractionDetails, executor.listPackages())
        bdioPropertyHelper.addRelationships(projectNode, components)
        bdioWriter.writeBdioNode(projectNode)
        components.each { component ->
            bdioWriter.writeBdioNode(component)
        }
    }

    java.util.List<BdioComponent> createBdioComponent(String name, String version, String externalId){
        def components = []
        forges.each{ forge ->
			String bdioId = createDataId(forge, name, version)
            BdioComponent bdioComponent = bdioNodeFactory.createComponent(name, version, bdioId, forge, externalId)
            components.add(bdioComponent)
        }
        components
    }
	
	protected String createDataId(final String forge, String name, String version) {
		return "data:" + getExternalId(name, version)
	}
	
	protected String getExternalId(String name, String version) {
		"${name}/${version}"
	}
}
