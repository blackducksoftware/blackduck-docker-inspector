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
    HubDockerClient hubDockerClient

    @Autowired
    List<Extractor> extractors

    File[] performExtractOfDockerImage(String imageName) {
        // use docker to pull image if necessary
        // use docker to save image to tar
        // performExtractFromDockerTar()
        DockerClient client = hubDockerClient.getDockerClient()

        File imageTarGzFile = new File(imageName + ".tar")
        InputStream tarGzInputStream = null
        try{
            tarGzInputStream = client.saveImageCmd(imageName).exec()
            FileUtils.copyInputStreamToFile(tarGzInputStream, imageTarGzFile);
        } finally{
            IOUtils.closeQuietly(tarGzInputStream);
        }

        performExtractOfDockerTar(imageTarGzFile)
    }

    File[] performExtractOfDockerTar(File dockerTar) {
        // Parse through the tar and the tar layers
        // Find the package manager files
        // extract the package manager files and put them into the correct locations on the machine that is running this
        //performExtractFromRunningImage()
        DockerTarParser tarParser = new DockerTarParser()
        tarParser.workingDirectory = new File(workingDirectoryPath)

        TarExtractionResults results = tarParser.parseImageTar(linuxDistro, dockerTar)
        if(results.operatingSystemEnum == null){
            throw new HubIntegrationException('Could not determine the Operating System of this Docker tar.')
        }
        stubPackageManagerFiles(results.extractionResults)

        performExtractFromRunningImage(dockerTar.getName(), results)
    }

    void uploadBdioFiles(File[] bdioFiles){
        bdioFiles.each {
            if (hubClient.isValid()) {
                hubClient.uploadBdioToHub(it)
            }
        }
    }

    private File[] performExtractFromRunningImage(String tarFileName, TarExtractionResults tarResults) {
        File workingDirectory = new File(workingDirectoryPath)
        // run the package managers
        // extract the bdio from output
        // deploy bdio to the Hub
        File[] bdioFiles = []
        tarResults.extractionResults.each { extractionResult ->
            String projectName = "${tarFileName}_${extractionResult.packageManager}"
            String version = DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now())
            def outputFile = new File(workingDirectory, "${projectName}_${version}_bdio.jsonld")
            bdioFiles.add(outputFile)
            new FileOutputStream(outputFile).withStream { outputStream ->
                BdioWriter writer = new BdioWriter(new Gson(), outputStream)
                Extractor extractor = getExtractorByPackageManager(extractionResult.packageManager)
                extractor.extract(writer, tarResults.operatingSystemEnum, projectName, version)
            }
        }

    }

    private void stubPackageManagerFiles(List<TarExtractionResult> results){
        results.each { result ->
            File packageManagerDirectory = new File(result.packageManager.directory)
            if(packageManagerDirectory.exists()){
                FileUtils.deleteDirectory(packageManagerDirectory)
            }
            FileUtils.copyDirectory(result.extractedPackageManagerDirectory, packageManagerDirectory)
        }
    }

    private Extractor getExtractorByPackageManager(PackageManagerEnum packageManagerEnum){
        extractors.each { extractor ->
            if(extractor.packageManagerEnum == packageManagerEnum){
                return extractor
            }
        }
    }
}