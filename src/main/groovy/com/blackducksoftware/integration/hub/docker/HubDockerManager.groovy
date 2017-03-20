package com.blackducksoftware.integration.hub.docker

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class HubDockerManager {
    private final Logger logger = LoggerFactory.getLogger(HubDockerManager.class)

    @Value('${working.directory}')
    String workingDirectoryPath

    @Value('${command.timeout}')
    long commandTimeout

    @Autowired
    OperatingSystemFinder operatingSystemFinder

    @Autowired
    HubClient hubClient

    
    void performExtractOfDockerImage() {
        // use docker to pull image if necessary
        // use docker to save image to tar
        // performExtractFromDockerTar()
    }


    void performExtractFromDockerTar() {
    }

    void performExtractFromRunningImage() {
    }
}