package com.blackducksoftware.integration.hub.docker

import org.apache.commons.lang3.StringUtils
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
import com.blackducksoftware.integration.log.Slf4jIntLogger

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

    @Value('${command.timeout}')
    long commandTimeout

    @Value('${key.store}')
    String keyStore

    @Value('${key.store.pass}')
    String keyStorePass

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


    File retrieveHttpsCertificate(){
        File certificateFile = new File('certificate.txt')
        URL url = new URL(hubUrl)
        String certificate = ''
        def standardOut = new StringBuilder()
        def standardError = new StringBuilder()
        String command = "keytool -printcert -rfc -sslserver ${url.getHost()}"
        Process proc = command.execute()
        proc.consumeProcessOutput(standardOut, standardError)
        proc.waitForOrKill(commandTimeout)
        certificate = standardOut.toString()
        logger.info("Retrieving the certificate from ${url.getHost()}")
        logger.debug(certificate)
        def error = standardError.toString()
        if(StringUtils.isNotBlank(error)){
            logger.warn(standardError.toString())
        }
        logger.debug("Exit code ${proc.exitValue()}")
        if(proc.exitValue() != 0){
            throw new HubIntegrationException("Failed to retrieve the certificate from ${hubUrl}")
        }
        certificateFile.write(certificate)
        certificateFile
    }

    void importHttpsCertificate(File certificate){
        URL url = new URL(hubUrl)

        if(StringUtils.isBlank(keyStore)){
            String javaHome = System.getProperty('java.home')
            File jssecacerts = new File("${javaHome}")
            jssecacerts = new File(jssecacerts, "lib")
            jssecacerts = new File(jssecacerts, "security")
            jssecacerts = new File(jssecacerts, "jssecacerts")
            keyStore = jssecacerts.getAbsolutePath()
        }
        if(StringUtils.isBlank(keyStorePass)){
            keyStorePass = 'changeit'
        }
        def standardOut = new StringBuilder()
        def standardError = new StringBuilder()

        String command = "keytool -importcert -keystore ${keyStore} -storepass ${keyStorePass} -alias ${url.getHost()} -noprompt -file ${certificate.getAbsolutePath()}"
        Process proc = command.execute()
        proc.consumeProcessOutput(standardOut, standardError)
        proc.waitForOrKill(commandTimeout)
        logger.info("Importing the certificate from ${certificate.getAbsolutePath()}")
        logger.debug(standardOut.toString())
        def error = standardError.toString()
        if(StringUtils.isNotBlank(error)){
            logger.warn(standardError.toString())
        }
        logger.debug("Exit code ${proc.exitValue()}")
        if(proc.exitValue() != 0){
            throw new HubIntegrationException("Failed to import the certificate into ${keyStore}")
        }
    }
}
