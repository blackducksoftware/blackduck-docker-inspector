package com.blackducksoftware.integration.hub.docker

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
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.DockerClientConfig
import com.google.gson.Gson

@Component
class HubDockerManager {
    private final Logger logger = LoggerFactory.getLogger(HubDockerManager.class)

    @Value('${working.directory}')
    String workingDirectoryPath

    @Value('${command.timeout}')
    long commandTimeout

    @Value('${operating.system}')
    String operatingSystem

    @Autowired
    HubClient hubClient

    @Autowired
    List<Extractor> extractors



    void performExtractOfDockerImage(String imageName) {
        // use docker to pull image if necessary
        // use docker to save image to tar
        // performExtractFromDockerTar()
        DockerClient client = getDockerClient()

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

    void performExtractOfDockerTar(File dockerTar) {
        // Parse through the tar and the tar layers
        // Find the package manager files
        // extract the package manager files and put them into the correct locations on the machine that is running this
        //performExtractFromRunningImage()
        DockerTarParser tarParser = new DockerTarParser()
        tarParser.workingDirectory = new File("docker")

        TarExtractionResults results = tarParser.parseImageTar(operatingSystem,dockerTar)
        if(results.operatingSystemEnum == null){
            throw new HubIntegrationException('Could not determing the Operating System of this Docker tar.')
        }
        setupPackageManagers(results.extractionResults)

        performExtractFromRunningImage(dockerTar.getName(), results)
    }


    private void performExtractFromRunningImage(String fileNamePrefix, TarExtractionResults tarResults) {
        File workingDirectory = new File(workingDirectoryPath)
        // run the package managers
        // extract the bdio from output
        // deploy bdio to the Hub
        tarResults.extractionResults.each { extractionResult ->
            def outputFile = new File(workingDirectory, "${fileNamePrefix}_${extractionResult.packageManager}_bdio.jsonld")

            new FileOutputStream(outputFile).withStream { outputStream ->
                BdioWriter writer = new BdioWriter(new Gson(), outputStream)
                Extractor extractor = getExtractorByPackageManager(extractionResult.packageManager)
                extractor.extract(writer, tarResults.operatingSystemEnum)
            }
        }
    }

    DockerClient getDockerClient(){
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://my-docker-host.tld:2376")
                .withDockerTlsVerify(true)
                .withDockerCertPath("/home/user/.docker/certs")
                .withDockerConfig("/home/user/.docker")
                .withApiVersion("1.23")
                .withRegistryUrl("https://index.docker.io/v1/")
                .withRegistryUsername("dockeruser")
                .withRegistryPassword("ilovedocker")
                .withRegistryEmail("dockeruser@github.com")
                .build();
        DockerClientBuilder.getInstance(config).build();
    }

    private void setupPackageManagers(List<TarExtractionResult> results){
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