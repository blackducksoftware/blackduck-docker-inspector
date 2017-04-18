package com.blackducksoftware.integration.hub.docker

import javax.annotation.PostConstruct

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder

@SpringBootApplication
class Application {
    private final Logger logger = LoggerFactory.getLogger(Application.class)

    @Value('${docker.tar}')
    String dockerTar

    @Value('${docker.image.name}')
    String dockerImageName

    @Value('${docker.tag.name}')
    String dockerTagName

    @Value('${linux.distro}')
    String linuxDistro

    @Autowired
    HubClient hubClient

    @Autowired
    HubDockerManager hubDockerManager

    static void main(final String[] args) {
        new SpringApplicationBuilder(Application.class).logStartupInfo(false).run(args)
    }

    @PostConstruct
    void init() {
        try {
            hubClient.testHubConnection()
            logger.info 'Your Hub configuration is valid and a successful connection to the Hub was established.'
        } catch (Exception e) {
            logger.error("Your Hub configuration is not valid: ${e.message}")
        }
        hubDockerManager.init()
        hubDockerManager.cleanWorkingDirectory()
        def bdioFiles = null
        if(StringUtils.isNotBlank(dockerTar)) {
            File dockerTarFile = new File(dockerTar)
            File layerFilesDir = hubDockerManager.extractDockerLayers(new File(dockerTar))
            OperatingSystemEnum targetOsEnum = hubDockerManager.detectOperatingSystem(linuxDistro, layerFilesDir)
            bdioFiles = hubDockerManager.generateBdioFromLayerFilesDir(dockerTarFile, layerFilesDir, targetOsEnum)
        } else if (StringUtils.isNotBlank(dockerImageName)) {
            if (StringUtils.isBlank(dockerTagName)) {
                dockerTagName = 'latest'
            }
            File dockerTarFile = hubDockerManager.getTarFileFromDockerImage(dockerImageName, dockerTagName)
            File layerFilesDir = hubDockerManager.extractDockerLayers(dockerTarFile)
            OperatingSystemEnum targetOsEnum = hubDockerManager.detectOperatingSystem(linuxDistro, layerFilesDir)
            bdioFiles = hubDockerManager.generateBdioFromLayerFilesDir(dockerTarFile, layerFilesDir, targetOsEnum)
        }
        hubDockerManager.uploadBdioFiles(bdioFiles)
    }

    void moveThisIntoInit(String linuxDistro, File layerFilesDir) {
        OperatingSystemEnum targetOsEnum = hubDockerManager.detectOperatingSystem(linuxDistro, layerFilesDir)

        OsMapper osMapper = new OsMapper()
        OperatingSystemEnum requiredOsEnum = osMapper.getRuntimeOsForTargetImageOs(targetOsEnum)
        OperatingSystemEnum currentOsEnum = osMapper.getCurrentOs()
        if (currentOsEnum == requiredOsEnum) {
        } else {
        }
    }
}
