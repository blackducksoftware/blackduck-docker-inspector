package com.blackducksoftware.integration.hub.docker

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
import com.google.gson.JsonArray
import com.google.gson.JsonParser

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

    @Value('${dev.mode:false}')
    Boolean devMode

    @Value('${hub.project.name}')
    String hubProjectName

    @Value('${hub.project.version}')
    String hubVersionName

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
            if (devMode) {
                logger.info("Running in development mode")
            }
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

            getProjectNameAndVersion(dockerTarFile.getName())

            OperatingSystemEnum targetOsEnum = hubDockerManager.detectOperatingSystem(linuxDistro, layerFilesDir)
            OperatingSystemEnum requiredOsEnum = dockerImages.getDockerImageOs(targetOsEnum)
            OperatingSystemEnum currentOsEnum = hubDockerManager.detectCurrentOperatingSystem()
            if (currentOsEnum == requiredOsEnum) {
                String msg = sprintf("Image inspection for %s can be run in this %s docker container; tarfile: %s",
                        targetOsEnum.toString(), currentOsEnum.toString(), dockerTarFile.getAbsolutePath())
                logger.info(msg)
                bdioFiles = hubDockerManager.generateBdioFromLayerFilesDir(hubProjectName, hubVersionName, dockerTarFile, layerFilesDir, targetOsEnum)
                if (bdioFiles.size() == 0) {
                    logger.warn("No BDIO Files generated")
                } else {
                    hubDockerManager.uploadBdioFiles(bdioFiles)
                }
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
                dockerClientManager.run(runOnImageName, runOnImageVersion, dockerTarFile, linuxDistro, devMode, hubProjectName, hubVersionName)
            }
        } catch (Exception e) {
            logger.error("Error inspecting image: ${e.message}")
            String trace = ExceptionUtils.getStackTrace(e)
            logger.error("Stack trace: ${trace}")
        }
    }

    private File deriveDockerTarFile() {
        File dockerTarFile
        if(StringUtils.isNotBlank(dockerTar)) {
            dockerTarFile = new File(dockerTar)
        } else if (StringUtils.isNotBlank(dockerImageName)) {
            if (StringUtils.isBlank(dockerTagName)) {
                // set default if blank
                dockerTagName = 'latest'
            }
            dockerTarFile = hubDockerManager.getTarFileFromDockerImage(dockerImageName, dockerTagName)
        }
        dockerTarFile
    }

    private void getProjectNameAndVersion(String tarFileName){
        if (StringUtils.isBlank(hubProjectName) || StringUtils.isBlank(hubVersionName) ){
            try{
                def manifestContentString = hubDockerManager.extractManifestFileContent(tarFileName)
                JsonParser parser = new JsonParser()
                def manifestContent = parser.parse(manifestContentString).getAsJsonArray().get(0).getAsJsonObject()
                JsonArray repoArray = manifestContent.get('RepoTags').getAsJsonArray()
                def repoTag = repoArray.get(0).getAsString()
                if(StringUtils.isBlank(hubProjectName)){
                    hubProjectName  = repoTag.substring(0, repoTag.indexOf(':'))
                }
                if(StringUtils.isBlank(hubVersionName)){
                    hubVersionName  = repoTag.substring(repoTag.indexOf(':') + 1)
                }
            } catch (Exception e){
                logger.error("Could not parse the image manifest file : ${e.toString()}")
                if(StringUtils.isNotBlank(dockerImageName)){
                    hubProjectName = dockerImageName
                } else {
                    hubProjectName = tarFileName
                }
                if(StringUtils.isNotBlank(dockerTagName)){
                    hubVersionName = dockerTagName
                } else {
                    hubVersionName = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())
                }
            }
        }
    }

}
