package com.blackducksoftware.integration.hub.docker

import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.bdio.simple.BdioWriter
import com.blackducksoftware.integration.hub.docker.client.DockerClientManager
import com.blackducksoftware.integration.hub.docker.extractor.ExtractionDetails
import com.blackducksoftware.integration.hub.docker.extractor.Extractor
import com.blackducksoftware.integration.hub.docker.tar.DockerTarParser
import com.blackducksoftware.integration.hub.docker.tar.LayerMapping
import com.blackducksoftware.integration.hub.docker.tar.TarExtractionResult
import com.blackducksoftware.integration.hub.docker.tar.TarExtractionResults
import com.blackducksoftware.integration.hub.exception.HubIntegrationException
import com.google.gson.Gson

@Component
class HubDockerManager {
    private final Logger logger = LoggerFactory.getLogger(HubDockerManager.class)

    @Value('${working.directory}')
    String workingDirectoryPath

    @Value('${linux.distro}')
    String linuxDistro

    @Autowired
    HubClient hubClient

    @Autowired
    DockerClientManager dockerClientManager

    @Autowired
    List<Extractor> extractors

    DockerTarParser tarParser

    void init() {
        tarParser = new DockerTarParser()
        tarParser.workingDirectory = new File(workingDirectoryPath)
    }

    File getTarFileFromDockerImage(String imageName, String tagName) {
        dockerClientManager.getTarFileFromDockerImage(imageName, tagName)
    }

    List<File> extractLayerTars(File dockerTar){
        tarParser.extractLayerTars(dockerTar)
    }

    File extractDockerLayers(List<File> layerTars, List<LayerMapping> layerMappings) {
        // Parse through the tar and the tar layers
        // Find the package manager files
        // extract the package manager files and put them into the correct locations on the machine that is running this
        //performExtractFromRunningImage()
        tarParser.extractDockerLayers(layerTars, layerMappings)
    }

    String extractManifestFileContent(String dockerTarName){
        tarParser.extractManifestFileContent(dockerTarName)
    }

    OperatingSystemEnum detectOperatingSystem(String operatingSystem, File extractedFilesDir) {
        tarParser.detectOperatingSystem(operatingSystem, extractedFilesDir)
    }

    OperatingSystemEnum detectCurrentOperatingSystem() {
        tarParser.detectOperatingSystemFromEtcDir(new File("/etc"))
    }

    List<File> generateBdioFromImageFilesDir(List<LayerMapping> mappings, String projectName, String versionName, File dockerTar, File imageFilesDir, OperatingSystemEnum osEnum) {
        TarExtractionResults tarExtractionResults = tarParser.extractPackageManagerDirs(imageFilesDir, osEnum)
        if(tarExtractionResults.operatingSystemEnum == null){
            throw new HubIntegrationException('Could not determine the Operating System of this Docker tar.')
        }
        String architecture = null
        if(osEnum == OperatingSystemEnum.ALPINE){
            List<File> etcDirectories = tarParser.findFileWithName(imageFilesDir, "etc")
            for(File etc : etcDirectories){
                File architectureFile = new File(etc, 'apk')
                architectureFile = new File(architectureFile, 'arch')
                if(architectureFile.exists()){
                    architecture = architectureFile.readLines().get(0)
                    break
                }
            }
        }
        generateBdioFromPackageMgrDirs(mappings, projectName, versionName, dockerTar.getName(), tarExtractionResults, architecture)
    }

    void uploadBdioFiles(List<File> bdioFiles){
        if(bdioFiles != null){
            bdioFiles.each { file ->
                if (hubClient.isValid()) {
                    hubClient.uploadBdioToHub(file)
                }
            }
        }
    }

    void cleanWorkingDirectory(){
        File workingDirectory = new File(workingDirectoryPath)
        if(workingDirectory.exists()){
            FileUtils.deleteDirectory(workingDirectory)
        }
    }

    private List<File> generateBdioFromPackageMgrDirs(List<LayerMapping> layerMappings, String projectName, String versionName, String tarFileName, TarExtractionResults tarResults, String architecture) {
        File workingDirectory = new File(workingDirectoryPath)
        // run the package managers
        // extract the bdio from output
        // deploy bdio to the Hub
        def bdioFiles = []
        tarResults.extractionResults.each { extractionResult ->
            def mapping = layerMappings.find { mapping ->
                StringUtils.compare(mapping.getImageDirectory(), extractionResult.imageDirectoryName) == 0
            }
            String imageDirectoryName = mapping.getImageDirectory()
            String filePath = extractionResult.extractedPackageManagerDirectory.getAbsolutePath()
            filePath = filePath.substring(filePath.indexOf(imageDirectoryName) + 1)
            filePath = filePath.substring(filePath.indexOf('/') + 1)
            filePath = filePath.replaceAll('/', '_')
            String cleanedImageName = mapping.getImageDirectory().replaceAll('/', '_')
            stubPackageManagerFiles(extractionResult)
            String codeLocationName, hubProjectName, hubVersionName = ''
            codeLocationName = "${cleanedImageName}_${mapping.tagName}_${imageDirectoryName}_${filePath}_${extractionResult.packageManager}"
            hubProjectName = deriveHubProject(cleanedImageName, projectName)
            hubVersionName = deriveHubProjectVersion(mapping, versionName)

            logger.info("Hub project/version: ${hubProjectName}/${hubVersionName}; Code location : ${codeLocationName}")

            String newFileName = "${imageDirectoryName}_${filePath}_${hubProjectName}_${hubVersionName}_bdio.jsonld"
            def outputFile = new File(workingDirectory, newFileName)
            bdioFiles.add(outputFile)
            new FileOutputStream(outputFile).withStream { outputStream ->
                BdioWriter writer = new BdioWriter(new Gson(), outputStream)
                try{
                    Extractor extractor = getExtractorByPackageManager(extractionResult.packageManager)
                    ExtractionDetails extractionDetails = new ExtractionDetails()
                    extractionDetails.operatingSystem = tarResults.operatingSystemEnum
                    extractionDetails.architecture = architecture
                    extractor.extract(writer, extractionDetails, codeLocationName, hubProjectName, hubVersionName)
                }finally{
                    writer.close()
                }
            }
        }
        bdioFiles
    }

    private String deriveHubProject(String cleanedImageName, String projectName) {
        String hubProjectName
        if (StringUtils.isBlank(projectName)) {
            hubProjectName = cleanedImageName
        } else {
            logger.debug("Using project from config property")
            hubProjectName = projectName
        }
        return hubProjectName
    }

    private String deriveHubProjectVersion(LayerMapping mapping, String versionName) {
        String hubVersionName
        if (StringUtils.isBlank(versionName)) {
            hubVersionName = mapping.tagName
        } else {
            logger.debug("Using project version from config property")
            hubVersionName = versionName
        }
        return hubVersionName
    }


    private void stubPackageManagerFiles(TarExtractionResult result){
        File packageManagerDirectory = new File(result.packageManager.directory)
        if(packageManagerDirectory.exists()){
            deleteFilesOnly(packageManagerDirectory)
            if(result.packageManager == PackageManagerEnum.DPKG){
                File statusFile = new File(packageManagerDirectory, 'status')
                statusFile.createNewFile()
                File updatesDir = new File(packageManagerDirectory, 'updates')
                updatesDir.mkdir()
            }
        }
        FileUtils.copyDirectory(result.extractedPackageManagerDirectory, packageManagerDirectory)
    }

    private void deleteFilesOnly(File file){
        if (file.isDirectory()){
            for (File subFile: file.listFiles()) {
                deleteFilesOnly(subFile)
            }
        } else{
            file.delete()
        }
    }

    private Extractor getExtractorByPackageManager(PackageManagerEnum packageManagerEnum){
        extractors.find { currentExtractor ->
            currentExtractor.packageManagerEnum == packageManagerEnum
        }
    }
}