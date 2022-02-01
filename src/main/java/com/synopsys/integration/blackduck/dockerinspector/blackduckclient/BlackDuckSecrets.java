/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.blackduckclient;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;

@Component
public class BlackDuckSecrets {

    @Autowired
    private Config config;

    public String getPassword() {
        String blackDuckPassword = config.getBlackDuckPasswordEnvVar();
        if (!StringUtils.isBlank(config.getBlackDuckPassword())) {
            blackDuckPassword = config.getBlackDuckPassword();
        }
        return blackDuckPassword;
    }

    public String getApiToken() {
        String blackDuckApiToken = config.getBlackDuckApiTokenEnvVar();
        if (!StringUtils.isBlank(config.getBlackDuckApiToken())) {
            blackDuckApiToken = config.getBlackDuckApiToken();
        }
        return blackDuckApiToken;
    }
}
