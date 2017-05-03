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
import com.blackducksoftware.integration.hub.docker.tar.LayerMapping
import com.blackducksoftware.integration.hub.docker.tar.manifest.ImageInfo
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
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
                logger.error("Your Hub configuration is not valid: ${e.getMessage()}")
                if(StringUtils.contains(e.getMessage(), 'SunCertPathBuilderException')){
                    //TODO when integration common gets into hub common we can catch a new IntegrationCertificateException rather than doing this String check
                    File certificate = null
                    try{
                        certificate = hubClient.retrieveHttpsCertificate()
                        hubClient.importHttpsCertificate(certificate)
                    } finally{
                        if(certificate != null && certificate.exists()){
                            certificate.delete()
                        }
                    }
                    try {
                        hubClient.testHubConnection()
                        logger.info 'Your Hub configuration is valid and a successful connection to the Hub was established.'
                    } catch (Exception e1) {
                        logger.error("Your Hub configuration is not valid: ${e1.getMessage()}")
                    }
                }
            }
            hubDockerManager.init()
            hubDockerManager.cleanWorkingDirectory()
            def bdioFiles = null
            File dockerTarFile = deriveDockerTarFile()

            if (StringUtils.isBlank(dockerTagName)) {
                // set default if blank
                dockerTagName = 'latest'
            }

            List<File> layerTars = hubDockerManager.extractLayerTars(dockerTarFile)
            List<LayerMapping> layerMappings = getLayerMappings(dockerTarFile.getName())
            File layerFilesDir = hubDockerManager.extractDockerLayers(layerTars, layerMappings)

            OperatingSystemEnum targetOsEnum = hubDockerManager.detectOperatingSystem(linuxDistro, layerFilesDir)
            OperatingSystemEnum requiredOsEnum = dockerImages.getDockerImageOs(targetOsEnum)
            OperatingSystemEnum currentOsEnum = hubDockerManager.detectCurrentOperatingSystem()
            if (currentOsEnum == requiredOsEnum) {
                String msg = sprintf("Image inspection for %s can be run in this %s docker container; tarfile: %s",
                        targetOsEnum.toString(), currentOsEnum.toString(), dockerTarFile.getAbsolutePath())
                logger.info(msg)
                bdioFiles = hubDockerManager.generateBdioFromLayerFilesDir(layerMappings, hubProjectName, hubVersionName, dockerTarFile, layerFilesDir, targetOsEnum)
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
            logger.debug("Stack trace: ${trace}")
        }
    }

    private File deriveDockerTarFile() {
        File dockerTarFile
        if(StringUtils.isNotBlank(dockerTar)) {
            dockerTarFile = new File(dockerTar)
        } else if (StringUtils.isNotBlank(dockerImageName)) {
            dockerTarFile = hubDockerManager.getTarFileFromDockerImage(dockerImageName, dockerTagName)
        }
        dockerTarFile
    }

    private List<LayerMapping> getLayerMappings(String tarFileName){
        List<LayerMapping> mappings = new ArrayList<>()
        try {
            List<ImageInfo> images = getManifestContents(tarFileName)
            for(ImageInfo image : images) {
                LayerMapping mapping = new LayerMapping()

                def specifiedRepoTag = ''
                if (StringUtils.isNotBlank(dockerImageName)) {
                    specifiedRepoTag = "${dockerImageName}:${dockerTagName}"
                }
                def (imageName, tagName) = ['', '']
                def foundRepoTag = image.repoTags.find { repoTag ->
                    StringUtils.compare(repoTag, specifiedRepoTag) == 0
                }
                if(StringUtils.isBlank(foundRepoTag)){
                    (imageName, tagName) = image.repoTags.get(0).split(':')
                } else {
                    (imageName, tagName) = foundRepoTag.split(':')
                }
                logger.info("Image ${imageName} , Tag ${tagName}")
                mapping.imageName =  imageName
                mapping.tagName = tagName
                for(String layer : image.layers){
                    mapping.layers.add(layer.substring(0, layer.indexOf('/')))
                }
                if (StringUtils.isNotBlank(dockerImageName)) {
                    if(StringUtils.compare(imageName, dockerImageName) == 0 && StringUtils.compare(tagName, dockerTagName) == 0){
                        logger.debug('Adding layer mapping')
                        logger.debug("Image ${mapping.imageName} , Tag ${mapping.tagName}")
                        logger.debug("Layers ${mapping.layers}")
                        mappings.add(mapping)
                    }
                } else {
                    logger.debug('Adding layer mapping')
                    logger.debug("Image ${mapping.imageName} , Tag ${mapping.tagName}")
                    logger.debug("Layers ${mapping.layers}")
                    mappings.add(mapping)
                }
            }
        } catch (Exception e) {
            logger.error("Could not parse the image manifest file : ${e.toString()}")
            LayerMapping mapping = new LayerMapping()
            mapping.imageName =  tarFileName
            mapping.tagName = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())
            mappings.add(mapping)
        }
        mappings
    }

    private List<ImageInfo> getManifestContents(String tarFileName){
        List<ImageInfo> images = new ArrayList<>()
        def manifestContentString = hubDockerManager.extractManifestFileContent(tarFileName)
        JsonParser parser = new JsonParser()
        JsonArray manifestContent = parser.parse(manifestContentString).getAsJsonArray()
        Gson gson = new Gson()
        for(JsonElement element : manifestContent){
            images.add(gson.fromJson(element, ImageInfo.class))
        }
        images
    }

}
