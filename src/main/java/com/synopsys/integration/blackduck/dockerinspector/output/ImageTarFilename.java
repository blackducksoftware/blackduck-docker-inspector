/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.output;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.ProgramPaths;
import com.synopsys.integration.blackduck.dockerinspector.dockerclient.DockerClientManager;
import com.synopsys.integration.blackduck.exception.BlackDuckIntegrationException;
import com.synopsys.integration.exception.IntegrationException;

@Component
public class ImageTarFilename {

    public String deriveImageTarFilenameFromImageTag(final String imageName, final String tagName) {
        return String.format("%s_%s.tar", cleanImageName(imageName), tagName);
    }

    private String cleanImageName(final String imageName) {
        return colonsToUnderscores(slashesToUnderscore(imageName));
    }

    private String colonsToUnderscores(final String imageName) {
        return imageName.replace(":", "_");
    }

    private String slashesToUnderscore(final String givenString) {
        return givenString.replace("/", "_");
    }
}
