package com.blackducksoftware.integration.hub.docker

import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.bdio.simple.BdioWriter
import com.blackducksoftware.integration.hub.docker.client.DockerClientManager
import com.blackducksoftware.integration.hub.docker.extractor.Extractor
import com.blackducksoftware.integration.hub.docker.tar.DockerTarParser
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

    File extractDockerLayers(File dockerTar) {
        // Parse through the tar and the tar layers
        // Find the package manager files
        // extract the package manager files and put them into the correct locations on the machine that is running this
        //performExtractFromRunningImage()
        tarParser.extractDockerLayers(dockerTar)
    }

    String extractManifestFileContent(String dockerTarName){
        tarParser.extractManifestFileContent(dockerTarName)
    }

    OperatingSystemEnum detectOperatingSystem(String operatingSystem, File layerFilesDir) {
        tarParser.detectOperatingSystem(operatingSystem, layerFilesDir)
    }

    OperatingSystemEnum detectCurrentOperatingSystem() {
        tarParser.detectOperatingSystemFromEtcDir(new File("/etc"))
    }

    List<File> generateBdioFromLayerFilesDir(String projectName, String versionName, File dockerTar, File layerFilesDir, OperatingSystemEnum osEnum) {
        TarExtractionResults packageMgrDirs = tarParser.extractPackageManagerDirs(layerFilesDir, osEnum)
        if(packageMgrDirs.operatingSystemEnum == null){
            throw new HubIntegrationException('Could not determine the Operating System of this Docker tar.')
        }
        generateBdioFromPackageMgrDirs(projectName, versionName, dockerTar.getName(), packageMgrDirs)
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

    private List<File> generateBdioFromPackageMgrDirs(String projectName, String versionName, String tarFileName, TarExtractionResults tarResults) {
        File workingDirectory = new File(workingDirectoryPath)
        // run the package managers
        // extract the bdio from output
        // deploy bdio to the Hub
        def bdioFiles = []
        tarResults.extractionResults.each { extractionResult ->
            String filePath = extractionResult.extractedPackageManagerDirectory.getAbsolutePath()
            filePath = filePath.substring(filePath.indexOf(extractionResult.layer) + 1)
            filePath = filePath.substring(filePath.indexOf('/') + 1)
            filePath = filePath.replace('/', '_')
            stubPackageManagerFiles(extractionResult)
            String codeLocationName = "${projectName}_${versionName}_${extractionResult.layer}_${filePath}_${extractionResult.packageManager}"
            logger.info(codeLocationName)

            def outputFile = new File(workingDirectory, "${extractionResult.layer}_${filePath}_${projectName}_${versionName}_bdio.jsonld")
            bdioFiles.add(outputFile)
            new FileOutputStream(outputFile).withStream { outputStream ->
                BdioWriter writer = new BdioWriter(new Gson(), outputStream)
                try{
                    Extractor extractor = getExtractorByPackageManager(extractionResult.packageManager)
                    extractor.extract(writer, tarResults.operatingSystemEnum, codeLocationName, projectName, versionName)
                }finally{
                    writer.close()
                }
            }
        }
        bdioFiles
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