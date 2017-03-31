package com.blackducksoftware.integration.hub.docker

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.docker.tar.DockerTarParser
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.DockerClientConfig

@Component
class HubDockerManager {
    private final Logger logger = LoggerFactory.getLogger(HubDockerManager.class)

    @Value('${working.directory}')
    String workingDirectoryPath

    @Value('${command.timeout}')
    long commandTimeout

    @Value('${docker.image.name}')
    String dockerImageName

    @Value('${docker.tar}')
    String dockerTar

    @Autowired
    OperatingSystemFinder operatingSystemFinder

    @Autowired
    HubClient hubClient


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

        tarParser.parseImageTar(dockerTar)

    }


    void performExtractFromRunningImage(PackageManagerEnum[] packageManagerEnums) {
        // run the package managers
        // extract the bdio from output
        // deploy bdio to the Hub
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
}