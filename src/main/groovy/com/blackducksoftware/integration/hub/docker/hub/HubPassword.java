package com.blackducksoftware.integration.hub.docker.hub;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HubPassword {

    @Value("${hub.password}")
    private String hubPasswordProperty;

    @Value("${BD_HUB_PASSWORD:}")
    private String hubPasswordEnvVar;

    public String get() {
        String hubPassword = hubPasswordEnvVar;
        if (!StringUtils.isBlank(hubPasswordProperty)) {
            hubPassword = hubPasswordProperty;
        }
        return hubPassword;
    }
}
