package com.blackducksoftware.integration.hub.docker

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.api.bom.BomImportRequestService
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder
import com.blackducksoftware.integration.hub.buildtool.BuildToolConstants
import com.blackducksoftware.integration.hub.exception.HubIntegrationException
import com.blackducksoftware.integration.hub.global.HubServerConfig
import com.blackducksoftware.integration.hub.rest.CredentialsRestConnection
import com.blackducksoftware.integration.hub.service.HubServicesFactory
import com.blackducksoftware.integration.log.Slf4jIntLogger

@Component
class HubClient implements ApplicationContextAware {
    private final Logger logger = LoggerFactory.getLogger(HubClient.class)

    ApplicationContext applicationContext

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext
    }

    boolean isValid() {
        createBuilder().isValid()
    }

    void assertValid() throws IllegalStateException {
        createBuilder().build()
    }

    void testHubConnection() throws HubIntegrationException {
        HubServerConfig hubServerConfig = createBuilder().build()
        CredentialsRestConnection credentialsRestConnection = hubServerConfig.createCredentialsRestConnection(new Slf4jIntLogger(logger))
        credentialsRestConnection.connect()
        logger.info('Successful connection to the Hub!')
    }

    void uploadBdioToHub(File bdioFile) {
        HubServerConfig hubServerConfig = createBuilder().build()

        CredentialsRestConnection credentialsRestConnection = hubServerConfig.createCredentialsRestConnection(new Slf4jIntLogger(logger))
        HubServicesFactory hubServicesFactory = new HubServicesFactory(credentialsRestConnection)
        BomImportRequestService bomImportRequestService = hubServicesFactory.createBomImportRequestService()
        bomImportRequestService.importBomFile(bdioFile, BuildToolConstants.BDIO_FILE_MEDIA_TYPE)
        logger.info("Uploaded bdio file ${bdioFile.getName()} to ${hubServerConfig.hubUrl}")
    }

    private HubServerConfigBuilder createBuilder() {
        Properties props = new Properties()
        props.putAll(applicationContext.getProperties())
        HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder()
        hubServerConfigBuilder.setFromProperties(props)

        hubServerConfigBuilder
    }
}
