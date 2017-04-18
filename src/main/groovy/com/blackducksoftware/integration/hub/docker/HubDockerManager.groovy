package com.blackducksoftware.integration.hub.docker

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.bdio.simple.BdioWriter
import com.blackducksoftware.integration.hub.docker.extractor.Extractor
import com.blackducksoftware.integration.hub.docker.tar.DockerTarParser
import com.blackducksoftware.integration.hub.docker.tar.TarExtractionResult
import com.blackducksoftware.integration.hub.docker.tar.TarExtractionResults
import com.blackducksoftware.integration.hub.exception.HubIntegrationException
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.PullImageCmd
import com.github.dockerjava.api.command.SaveImageCmd
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.DefaultDockerClientConfig.Builder
import com.github.dockerjava.core.command.PullImageResultCallback
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
    List<Extractor> extractors

    DockerTarParser tarParser

    void init() {
        tarParser = new DockerTarParser()
        tarParser.workingDirectory = new File(workingDirectoryPath)
    }

    File getTarFileFromDockerImage(String imageName, String tagName) {
        // use docker to pull image if necessary
        // use docker to save image to tar
        // performExtractFromDockerTar()
        DockerClient client = getDockerClient()
        File imageTarDirectory = new File(new File(workingDirectoryPath), 'tarDirectory')
        logger.info("Pulling image ${imageName}:${tagName}")
        PullImageCmd pull = client.pullImageCmd("${imageName}").withTag(tagName)
        pull.exec(new PullImageResultCallback()).awaitSuccess()
        File imageTarFile = new File(imageTarDirectory, "${imageName}_${tagName}.tar")
        InputStream tarInputStream = null
        try{
            logger.info("Saving the docker image to : ${imageTarFile.getAbsolutePath()}")
            SaveImageCmd saveCommand = client.saveImageCmd(imageName)
            saveCommand.withTag(tagName)
            tarInputStream = saveCommand.exec()
            FileUtils.copyInputStreamToFile(tarInputStream, imageTarFile);
        } finally{
            IOUtils.closeQuietly(tarInputStream);
        }
        imageTarFile
    }

    File extractDockerLayers(File dockerTar) {
        // Parse through the tar and the tar layers
        // Find the package manager files
        // extract the package manager files and put them into the correct locations on the machine that is running this
        //performExtractFromRunningImage()


        File layerFilesDir = tarParser.extractDockerLayers(dockerTar)
    }

    OperatingSystemEnum detectOperatingSystem(String operatingSystem, File layerFilesDir) {
        tarParser.detectOperatingSystem(operatingSystem, layerFilesDir)
    }

    List<File> generateBdioFromLayerFilesDir(File dockerTar, File layerFilesDir, OperatingSystemEnum osEnum) {
        TarExtractionResults packageMgrDirs = tarParser.extractPackageManagerDirs(layerFilesDir, osEnum)
        if(packageMgrDirs.operatingSystemEnum == null){
            throw new HubIntegrationException('Could not determine the Operating System of this Docker tar.')
        }
        generateBdioFromPackageMgrDirs(dockerTar.getName(), packageMgrDirs)
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

    private DockerClient getDockerClient(){
        Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder()
        DefaultDockerClientConfig config =  builder.build()

        DockerClientBuilder.getInstance(config).build();
    }

    private List<File> generateBdioFromPackageMgrDirs(String tarFileName, TarExtractionResults tarResults) {
        File workingDirectory = new File(workingDirectoryPath)
        // run the package managers
        // extract the bdio from output
        // deploy bdio to the Hub
        def bdioFiles = []
        tarResults.extractionResults.each { extractionResult ->
            logger.info("${extractionResult.layer}_${extractionResult.extractedPackageManagerDirectory.getAbsolutePath()}")
            stubPackageManagerFiles(extractionResult)
            String projectName = "${tarFileName}_${extractionResult.layer}_${extractionResult.packageManager}"
            // TODO this change fails:
            // String version = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDate.now())
            // Reverted back to:
            String version = DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now())
            def outputFile = new File(workingDirectory, "${projectName}_${version}_bdio.jsonld")
            bdioFiles.add(outputFile)
            new FileOutputStream(outputFile).withStream { outputStream ->
                BdioWriter writer = new BdioWriter(new Gson(), outputStream)
                try{
                    Extractor extractor = getExtractorByPackageManager(extractionResult.packageManager)
                    extractor.extract(writer, tarResults.operatingSystemEnum, projectName, version)
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