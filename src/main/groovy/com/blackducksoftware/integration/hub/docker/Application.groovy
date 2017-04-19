package com.blackducksoftware.integration.hub.docker

import javax.annotation.PostConstruct

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder

import com.blackducksoftware.integration.hub.docker.image.DockerImages

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
        File dockerTarFile = deriveDockerTarFile()
        File layerFilesDir = hubDockerManager.extractDockerLayers(new File(dockerTar))

        DockerImages dockerImages = new DockerImages()
        OperatingSystemEnum targetOsEnum = hubDockerManager.detectOperatingSystem(linuxDistro, layerFilesDir)
        OperatingSystemEnum requiredOsEnum = dockerImages.getDockerImageOs(targetOsEnum)
        OperatingSystemEnum currentOsEnum = hubDockerManager.detectCurrentOperatingSystem()
        if (currentOsEnum == requiredOsEnum) {
            String msg = sprintf("Image inspection for %s can be run in this %s docker container",
                    targetOsEnum.toString(), currentOsEnum.toString())
            logger.info(msg)
        } else {
            String msg = sprintf("Image inspection for %s should not be run in this %s docker container; should use docker image %s:%s",
                    targetOsEnum.toString(), currentOsEnum.toString(),
                    dockerImages.getDockerImageName(targetOsEnum),
                    dockerImages.getDockerImageVersion(targetOsEnum))
            logger.error(msg)
        }

        bdioFiles = hubDockerManager.generateBdioFromLayerFilesDir(dockerTarFile, layerFilesDir, targetOsEnum)
        hubDockerManager.uploadBdioFiles(bdioFiles)
    }

    private File deriveDockerTarFile() {
        File dockerTarFile
        if(StringUtils.isNotBlank(dockerTar)) {
            dockerTarFile = new File(dockerTar)
        } else if (StringUtils.isNotBlank(dockerImageName)) {
            if (StringUtils.isBlank(dockerTagName)) {
                dockerTagName = 'latest'
            }
            dockerTarFile = hubDockerManager.getTarFileFromDockerImage(dockerImageName, dockerTagName)
        }
        return dockerTarFile
    }

    void moveThisIntoInit(String linuxDistro, File layerFilesDir) {
        OperatingSystemEnum targetOsEnum = hubDockerManager.detectOperatingSystem(linuxDistro, layerFilesDir)

        DockerImages osMapper = new DockerImages()
        OperatingSystemEnum requiredOsEnum = osMapper.getDockerImage(targetOsEnum)
        OperatingSystemEnum currentOsEnum = osMapper.getCurrentOs()
        if (currentOsEnum == requiredOsEnum) {
        } else {
        }
    }
}
