package com.blackducksoftware.integration.hub.linux

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.bdio.simple.BdioNodeFactory
import com.blackducksoftware.integration.hub.bdio.simple.BdioPropertyHelper
import com.blackducksoftware.integration.hub.bdio.simple.BdioWriter
import com.google.gson.Gson

@Component
class BdioFileWriter {
    private final Logger logger = LoggerFactory.getLogger(BdioFileWriter.class)
    private final Gson gson = new Gson()
    private final BdioPropertyHelper bdioPropertyHelper = new BdioPropertyHelper()
    private final BdioNodeFactory bdioNodeFactory = new BdioNodeFactory(bdioPropertyHelper)


    BdioWriter createBdioWriter(final OutputStream outputStream, final String projectName, final String projectVersion) {
        def bdioWriter = new BdioWriter(gson, outputStream)

        def bom = bdioNodeFactory.createBillOfMaterials(projectName)

        bdioWriter.writeBdioNode(bom)

        def project = bdioNodeFactory.createProject(projectName, projectVersion, "uuid:${UUID.randomUUID()}", null)
        bdioWriter.writeBdioNode(project)

        bdioWriter
    }

    void writeComponent(BdioWriter bdioWriter, BdioComponentDetails bdioComponentDetails) {
        if (bdioComponentDetails == null) {
            logger.warn("writeComponent(): bdioComponentDetails is null")
            return;
        }
        def component = bdioComponentDetails.createBdioComponent()
        bdioWriter.writeBdioNode(component)
    }
}