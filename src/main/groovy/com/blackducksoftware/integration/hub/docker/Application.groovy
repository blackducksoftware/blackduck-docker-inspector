package com.blackducksoftware.integration.hub.docker

import javax.annotation.PostConstruct

import org.apache.commons.lang.exception.ExceptionUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder

import com.blackducksoftware.integration.hub.docker.client.DockerClientManager
import com.blackducksoftware.integration.hub.docker.image.DockerImages

@SpringBootApplication
class Application {
    // These values must be consistent with the image building code in build.gradle

    public static final String HUB_DOCKER_EXTRACTOR_CONTAINER = 'hub-docker-extractor'

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
    DockerImages dockerImages

    @Autowired
    HubDockerManager hubDockerManager

    @Autowired
    DockerClientManager dockerClientManager

    static void main(final String[] args) {
        new SpringApplicationBuilder(Application.class).logStartupInfo(false).run(args)
    }

    @PostConstruct
    void init() {
        try {
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
            File layerFilesDir = hubDockerManager.extractDockerLayers(dockerTarFile)


            OperatingSystemEnum targetOsEnum = hubDockerManager.detectOperatingSystem(linuxDistro, layerFilesDir)
            OperatingSystemEnum requiredOsEnum = dockerImages.getDockerImageOs(targetOsEnum)
            OperatingSystemEnum currentOsEnum = hubDockerManager.detectCurrentOperatingSystem()
            if (currentOsEnum == requiredOsEnum) {
                String msg = sprintf("Image inspection for %s can be run in this %s docker container; tarfile: %s",
                        targetOsEnum.toString(), currentOsEnum.toString(), dockerTarFile.getAbsolutePath())
                logger.info(msg)
                bdioFiles = hubDockerManager.generateBdioFromLayerFilesDir(dockerTarFile, layerFilesDir, targetOsEnum)
                hubDockerManager.uploadBdioFiles(bdioFiles)
            } else {
                //TODO remove the prefix before release. Only used for testing pulling from our internal Artifactory
                // String runOnImageName = "int-docker-repo.docker-repo/${dockerImages.getDockerImageName(targetOsEnum)}"
                String runOnImageName = dockerImages.getDockerImageName(targetOsEnum)
                String runOnImageVersion = dockerImages.getDockerImageVersion(targetOsEnum)
                String msg = sprintf("Image inspection for %s should not be run in this %s docker container; will use docker image %s:%s",
                        targetOsEnum.toString(), currentOsEnum.toString(),
                        runOnImageName, runOnImageVersion)
                logger.info(msg)
                try {
                    dockerClientManager.pullImage(runOnImageName, runOnImageVersion)
                } catch (Exception e) {
                    logger.warn(sprintf(
                            "Unable to pull docker image %s:%s; proceeding anyway since it may already exist locally",
                            runOnImageName, runOnImageVersion))
                }
                dockerClientManager.run(runOnImageName, runOnImageVersion, dockerTarFile)
            }
        } catch (Exception e) {
            logger.error("Error inspecting image: ${e.message}")
            String trace = ExceptionUtils.getStackTrace(e)
            logger.debug("Stack trace: ${trace}")
        }
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
        dockerTarFile
    }
}
