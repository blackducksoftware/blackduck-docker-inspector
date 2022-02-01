/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.programversion;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProgramVersion {
    private static final String PROGRAM_NAME_PRETTY = "Black Duck Docker Inspector";
    private static final String PROGRAM_ID = "blackduck-docker-inspector";
    private final Logger logger = LoggerFactory.getLogger(ProgramVersion.class);
    private String programVersion;
    private String inspectorImageFamily;
    private String inspectorImageVersion;

    @PostConstruct
    public void init() throws IOException {
        final ClassPathPropertiesFile versionProperties = new ClassPathPropertiesFile("version.properties");
        programVersion = versionProperties.getProperty("program.version");
        inspectorImageFamily = versionProperties.getProperty("inspector.image.family");
        inspectorImageVersion = versionProperties.getProperty("inspector.image.version");
        logger.debug(String.format("programVersion: %s", programVersion));
    }

    public String getProgramNamePretty() {
        return PROGRAM_NAME_PRETTY;
    }

    public String getProgramId() {
        return PROGRAM_ID;
    }
    public String getProgramVersion() {
        return programVersion;
    }

    public String getInspectorImageFamily() {
        return inspectorImageFamily;
    }

    public String getInspectorImageVersion() {
        return inspectorImageVersion;
    }
}
