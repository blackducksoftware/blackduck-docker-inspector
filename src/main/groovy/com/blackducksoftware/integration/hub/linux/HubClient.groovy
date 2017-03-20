package com.blackducksoftware.integration.hub.linux

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.api.bom.BomImportRequestService
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder
import com.blackducksoftware.integration.hub.buildtool.BuildToolConstants
import com.blackducksoftware.integration.hub.exception.HubIntegrationException
import com.blackducksoftware.integration.hub.global.HubServerConfig
import com.blackducksoftware.integration.hub.rest.CredentialsRestConnection
import com.blackducksoftware.integration.hub.service.HubServicesFactory

@Component
class HubClient {
    private final Logger logger = LoggerFactory.getLogger(HubClient.class)

    @Value('${hub.url}')
    String hubUrl

    @Value('${hub.timeout}')
    String hubTimeout

    @Value('${hub.username}')
    String hubUsername

    @Value('${hub.password}')
    String hubPassword

    @Value('${hub.proxy.host}')
    String hubProxyHost

    @Value('${hub.proxy.port}')
    String hubProxyPort

    @Value('${hub.proxy.username}')
    String hubProxyUsername

    @Value('${hub.proxy.password}')
    String hubProxyPassword

    boolean isValid() {
        createBuilder().isValid()
    }

    void assertValid() throws IllegalStateException {
        createBuilder().build()
    }

    void testHubConnection() throws HubIntegrationException {
        HubServerConfig hubServerConfig = createBuilder().build()
        CredentialsRestConnection credentialsRestConnection = new CredentialsRestConnection(hubServerConfig)
        credentialsRestConnection.connect()
        logger.info('Successful connection to the Hub!')
    }

    void uploadBdioToHub(File bdioFile) {
        HubServerConfig hubServerConfig = createBuilder().build()

        CredentialsRestConnection credentialsRestConnection = new CredentialsRestConnection(hubServerConfig)
        HubServicesFactory hubServicesFactory = new HubServicesFactory(credentialsRestConnection)
        BomImportRequestService bomImportRequestService = hubServicesFactory.createBomImportRequestService()
        bomImportRequestService.importBomFile(bdioFile, BuildToolConstants.BDIO_FILE_MEDIA_TYPE)
    }

    private HubServerConfigBuilder createBuilder() {
        HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder()
        hubServerConfigBuilder.hubUrl = hubUrl
        hubServerConfigBuilder.username = hubUsername
        hubServerConfigBuilder.password = hubPassword

        hubServerConfigBuilder.timeout = hubTimeout
        hubServerConfigBuilder.proxyHost = hubProxyHost
        hubServerConfigBuilder.proxyPort = hubProxyPort
        hubServerConfigBuilder.proxyUsername = hubProxyUsername
        hubServerConfigBuilder.proxyPassword = hubProxyPassword

        hubServerConfigBuilder
    }
}
