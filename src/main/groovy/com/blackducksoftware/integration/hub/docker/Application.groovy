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
import com.blackducksoftware.integration.hub.docker.client.ProgramVersion
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

    @Value('${docker.image}')
    String dockerImage

    @Value('${linux.distro}')
    String linuxDistro

    @Value('${dev.mode}')
    boolean devMode

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
	
	@Autowired
	ProgramVersion programVersion

    String dockerImageName
    String dockerTagName

    static void main(final String[] args) {
        new SpringApplicationBuilder(Application.class).logStartupInfo(false).run(args)
    }

    @PostConstruct
    void inspectImage() {
        try {
            init()
            File dockerTarFile = deriveDockerTarFile()

            List<File> layerTars = hubDockerManager.extractLayerTars(dockerTarFile)
            List<LayerMapping> layerMappings = getLayerMappings(dockerTarFile.getName())
            File imageFilesDir = hubDockerManager.extractDockerLayers(layerTars, layerMappings)

            OperatingSystemEnum targetOsEnum = hubDockerManager.detectOperatingSystem(linuxDistro, imageFilesDir)
            OperatingSystemEnum requiredOsEnum = dockerImages.getDockerImageOs(targetOsEnum)
            OperatingSystemEnum currentOsEnum = hubDockerManager.detectCurrentOperatingSystem()
            if (currentOsEnum == requiredOsEnum) {
                generateBdio(dockerTarFile, imageFilesDir, layerMappings, currentOsEnum, targetOsEnum)
            } else {
                runInSubContainer(dockerTarFile, currentOsEnum, targetOsEnum)
            }
        } catch (Exception e) {
            logger.error("Error inspecting image: ${e.message}")
            String trace = ExceptionUtils.getStackTrace(e)
            logger.debug("Stack trace: ${trace}")
        }
    }

	private runInSubContainer(File dockerTarFile, OperatingSystemEnum currentOsEnum, OperatingSystemEnum targetOsEnum) {
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
		dockerClientManager.run(runOnImageName, runOnImageVersion, dockerTarFile, devMode)
	}

	private generateBdio(File dockerTarFile, File imageFilesDir, List layerMappings, OperatingSystemEnum currentOsEnum, OperatingSystemEnum targetOsEnum) {
		String msg = sprintf("Image inspection for %s can be run in this %s docker container; tarfile: %s",
				targetOsEnum.toString(), currentOsEnum.toString(), dockerTarFile.getAbsolutePath())
		logger.info(msg)
		List<File> bdioFiles = hubDockerManager.generateBdioFromImageFilesDir(layerMappings, hubProjectName, hubVersionName, dockerTarFile, imageFilesDir, targetOsEnum)
		if (bdioFiles.size() == 0) {
			logger.warn("No BDIO Files generated")
		} else {
			hubDockerManager.uploadBdioFiles(bdioFiles)
		}
	}

	private init() {
		logger.info("hub-docker-inspector ${programVersion.getProgramVersion()}")
		if (devMode) {
			logger.info("Running in development mode")
		}
		if(StringUtils.isBlank(dockerTagName)){
			dockerTagName = 'latest'
		}
		initImageName()
		logger.info("Inspecting image/tag ${dockerImageName}/${dockerTagName}")
		verifyHubConnection()
		hubDockerManager.init()
		hubDockerManager.cleanWorkingDirectory()
	}
	
	private void verifyHubConnection() {
		try {
			hubClient.testHubConnection()
			logger.info 'Your Hub configuration is valid and a successful connection to the Hub was established.'
			return
		} catch (Exception e) {
			logger.error("The attempt to connect to the Hub was unsuccessful: ${e.getMessage()}")
			if(StringUtils.contains(e.getMessage(), 'SunCertPathBuilderException')){
				logger.info("The error is certificate-related; attempting to correct it...")
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
					return
				} catch (Exception e1) {
					logger.error("Unable to connect to the Hub: ${e1.getMessage()}")
					throw e1
				}
			}
			throw e
		}
	}

    private void initImageName() {
        if (StringUtils.isNotBlank(dockerImage)) {
            String[] imageNameAndTag = dockerImage.split(':')
            if ( (imageNameAndTag.length > 0) && (StringUtils.isNotBlank(imageNameAndTag[0])) ) {
                dockerImageName = imageNameAndTag[0]
            }
            if ( (imageNameAndTag.length > 1) && (StringUtils.isNotBlank(imageNameAndTag[1]))) {
                dockerTagName = imageNameAndTag[1]
            }
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

                String specifiedRepoTag = ''
                if (StringUtils.isNotBlank(dockerImageName)) {
                    specifiedRepoTag = "${dockerImageName}:${dockerTagName}"
                }
                def (imageName, tagName) = ['', '']
                String foundRepoTag = image.repoTags.find { repoTag ->
                    StringUtils.compare(repoTag, specifiedRepoTag) == 0
                }
                if(StringUtils.isBlank(foundRepoTag)){
                    def repoTag = image.repoTags.get(0)
                    imageName = repoTag.substring(0, repoTag.lastIndexOf(':'))
                    tagName = repoTag.substring(repoTag.lastIndexOf(':') + 1)
                } else {
                    imageName = foundRepoTag.substring(0, foundRepoTag.lastIndexOf(':'))
                    tagName = foundRepoTag.substring(foundRepoTag.lastIndexOf(':') + 1)
                }
                logger.info("Image: ${imageName}, Tag: ${tagName}")
                mapping.imageName =  imageName.replaceAll(':', '_').replaceAll('/', '_')
                mapping.tagName = tagName
                for(String layer : image.layers){
                    mapping.layers.add(layer.substring(0, layer.indexOf('/')))
                }
                if (StringUtils.isNotBlank(dockerImageName)) {
                    if(StringUtils.compare(imageName, dockerImageName) == 0 && StringUtils.compare(tagName, dockerTagName) == 0){
                        logger.debug('Adding layer mapping')
                        logger.debug("Image: ${mapping.imageName}:${mapping.tagName}")
                        logger.debug("Layers: ${mapping.layers}")
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
        // TODO TEMP; useful for debugging, but can probably remove once we're
        // confident in layer targeting
        logger.debug("getLayerMappings(): # mappings found: ${mappings.size()}")
        for (LayerMapping m : mappings) {
            logger.debug("getLayerMappings():\t${m.imageName}/${m.tagName}: ")
            for (String layerId : m.layers) {
                logger.debug("getLayerMappings():\t\t${layerId}")
            }
        }
        //////////////////
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
