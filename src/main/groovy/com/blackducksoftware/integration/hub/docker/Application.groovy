package com.blackducksoftware.integration.hub.docker

import javax.annotation.PostConstruct

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder

@SpringBootApplication
class Application {
    private final Logger logger = LoggerFactory.getLogger(Application.class)

    @Value('${docker.tar}')
    String dockerTar

    @Value('${docker.image.name}')
    String dockerImageName

    @Value('${docker.tag.name}')
    String dockerTagName

    @Autowired
    HubClient hubClient

    @Autowired
    HubDockerManager hubDockerManager

    static void main(final String[] args) {
        new SpringApplicationBuilder(Application.class).logStartupInfo(false).run(args)
    }

    @PostConstruct
    void init() {
        try {
            hubClient.testHubConnection()
            logger.info 'Your Hub configuration is valid and a successful connection to the Hub was established.'
        } catch (Exception e) {
            logger.error("Your Hub configuration is not valid: ${e.message}")
        }

        def bdioFiles = null
        if (StringUtils.isNotBlank(dockerImageName)) {
            if (StringUtils.isBlank(dockerTagName)) {
                dockerTagName = 'latest'
            }
            bdioFiles = hubDockerManager.performExtractOfDockerImage(dockerImageName, dockerTagName)
        } else if(StringUtils.isNotBlank(dockerTar)) {
            bdioFiles =  hubDockerManager.performExtractOfDockerTar(new File(dockerTar))
        }
        hubDockerManager.uploadBdioFiles(bdioFiles)
    }
}
