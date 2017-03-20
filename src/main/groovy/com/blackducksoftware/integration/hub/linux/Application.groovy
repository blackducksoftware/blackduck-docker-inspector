package com.blackducksoftware.integration.hub.linux

import javax.annotation.PostConstruct

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder

@SpringBootApplication
class Application {
    private final Logger logger = LoggerFactory.getLogger(Application.class)

    @Value('${extract.mode}')
    String extractMode

    @Autowired
    HubClient hubClient

    @Autowired
    HubLinuxManager hubLinuxManager

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

        if ('local' == extractMode) {
            hubLinuxManager.performExtractFromLocalInspection()
        } else {
            hubLinuxManager.performExtractFromRemoteInspection()
        }
    }
}
