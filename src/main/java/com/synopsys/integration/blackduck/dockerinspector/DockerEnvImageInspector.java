/**
 * hub-docker-inspector
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.dockerinspector;

import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.dockerinspector.blackduckclient.BlackDuckClient;
import com.synopsys.integration.blackduck.dockerinspector.common.Inspector;
import com.synopsys.integration.blackduck.dockerinspector.common.Output;
import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.ProgramPaths;
import com.synopsys.integration.blackduck.dockerinspector.dockerclient.DockerClientManager;
import com.synopsys.integration.blackduck.dockerinspector.dockerexec.DissectedImage;
import com.synopsys.integration.blackduck.dockerinspector.help.formatter.UsageFormatter;
import com.synopsys.integration.blackduck.imageinspector.api.PkgMgrDataNotFoundException;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageInfoDerived;
import com.synopsys.integration.blackduck.imageinspector.lib.ImageInspector;
import com.synopsys.integration.blackduck.imageinspector.name.ImageNameResolver;
import com.synopsys.integration.blackduck.imageinspector.result.ResultFile;
import com.synopsys.integration.exception.IntegrationException;

@SpringBootApplication
@ComponentScan(basePackages = { "com.synopsys.integration.blackduck.imageinspector", "com.synopsys.integration.blackduck.dockerinspector" })
public class DockerEnvImageInspector {
    private static final Logger logger = LoggerFactory.getLogger(DockerEnvImageInspector.class);

    public static final String PROGRAM_NAME = "blackduck-docker-inspector.sh";
    public static final String PROGRAM_ID = "blackduck-docker-inspector";

    @Autowired
    private BlackDuckClient blackDuckClient;

    @Autowired
    private DockerClientManager dockerClientManager;

    @Autowired
    private Output output;

    @Autowired
    private ImageInspector imageInspector;

    @Autowired
    private ProgramVersion programVersion;

    @Autowired
    private ProgramPaths programPaths;

    @Autowired
    private ResultFile resultFile;

    @Autowired
    private ApplicationArguments applicationArguments;

    @Autowired
    private Config config;

    @Autowired
    private List<Inspector> inspectors;

    @Autowired
    private UsageFormatter usageFormatter;

    public static void main(final String[] args) {
        new SpringApplicationBuilder(DockerEnvImageInspector.class).logStartupInfo(false).run(args);
        logger.warn("The program is not expected to get here.");
    }

    @PostConstruct
    public void inspectImage() {
        int returnCode = -1;
        final DissectedImage dissectedImage = new DissectedImage();
        try {
            logMsgAboutRestClientMode();
            if (!initAndValidate(config)) {
                System.exit(0);
            }
            try {
                final Inspector inspector = chooseInspector();
                returnCode = inspector.getBdio(dissectedImage);
            } catch (final PkgMgrDataNotFoundException e) {
                logger.info("Pkg mgr not found; generating empty BDIO file");
                final ImageInfoDerived imageInfoDerived = imageInspector.generateEmptyBdio(config.getDockerImageRepo(), config.getDockerImageTag(), dissectedImage.getLayerMappings(), config.getBlackDuckProjectName(),
                        config.getBlackDuckProjectVersion(), dissectedImage.getDockerTarFile(), dissectedImage.getTargetImageFileSystemRootDir(), dissectedImage.getTargetOs(), config.getBlackDuckCodelocationPrefix());
                output.writeBdioFile(dissectedImage, imageInfoDerived);
                output.uploadBdio(dissectedImage);
                output.createContainerFileSystemTarIfRequested(dissectedImage.getTargetImageFileSystemRootDir());
                output.provideOutput();
                returnCode = output.reportResultsPkgMgrDataNotFound(dissectedImage);
                output.cleanUp(null);
            }
        } catch (final Throwable e) {
            final String msg = String.format("Error inspecting image: %s", e.getMessage());
            logger.error(msg);
            final String trace = ExceptionUtils.getStackTrace(e);
            logger.debug(String.format("Stack trace: %s", trace));
            resultFile.write(new Gson(), programPaths.getDockerInspectorHostResultPath(), false, msg, dissectedImage.getTargetOs(), dissectedImage.getRunOnImageName(), dissectedImage.getRunOnImageTag(),
                    dissectedImage.getDockerTarFile() == null ? "" : dissectedImage.getDockerTarFile().getName(), dissectedImage.getBdioFilename());
        }
        logger.info(String.format("Returning %d", returnCode));
        System.exit(returnCode);
    }

    private void logMsgAboutRestClientMode() {
        if (config.isOnHost() && !config.isImageInspectorServiceStart() && StringUtils.isBlank(config.getImageInspectorUrl())) {
            final StringBuilder sb = new StringBuilder();
            sb.append("\n========\n");
            sb.append("Please start using 'HTTP client mode' (see the Docker Inspector HTTP Client Mode page in the Black Duck Docker Inspector documentation)\n");
            sb.append("to help ensure a smooth transition to future versions. ");
            sb.append("HTTP client mode will eventually replace the current default Docker exec mode.\n");
            sb.append("To use HTTP client mode now:\n");
            sb.append("\tIf you are using Docker Inspector directly: set property imageinspector.service.start=true\n");
            sb.append("\tIf you are using Detect: set property detect.docker.passthrough.imageinspector.service.start=true\n");
            sb.append("See the documentation for information on how to avoid port conflicts.\n");
            sb.append("========");
            logger.warn(sb.toString());
        }
    }

    private Inspector chooseInspector() throws IntegrationException {
        for (final Inspector inspector : inspectors) {
            if (inspector.isApplicable()) {
                return inspector;
            }
        }
        throw new IntegrationException("Invalid configuration: Unable to identify which inspector mode to execute");
    }

    private boolean helpInvoked() {
        logger.debug("Checking to see if help argument passed");
        if (applicationArguments == null) {
            logger.debug("applicationArguments is null");
            return false;
        }
        final String[] args = applicationArguments.getSourceArgs();
        if (contains(args, "-h") || contains(args, "--help")) {
            logger.debug("Help argument passed");
            return true;
        }
        return false;
    }

    private boolean contains(final String[] stringsToSearch, final String targetString) {
        for (final String stringToTest : stringsToSearch) {
            if (targetString.equals(stringToTest)) {
                return true;
            }
        }
        return false;
    }

    private void showUsage() throws IllegalArgumentException, IllegalAccessException, IOException {
        final List<String> usage = usageFormatter.getStringList();
        System.out.println("----------");
        for (final String line : usage) {
            System.out.println(line);
        }
        System.out.println("----------");
    }

    private boolean initAndValidate(final Config config) throws IOException, IntegrationException, IllegalArgumentException, IllegalAccessException {
        logger.info(String.format("Black Duck Docker Inspector %s", programVersion.getProgramVersion()));
        if (helpInvoked()) {
            showUsage();
            return false;
        }
        logger.debug(String.format("running from dir: %s", System.getProperty("user.dir")));
        logger.trace(String.format("dockerImageTag: %s", config.getDockerImageTag()));
        if (config.isOnHost()) {
            try {
                String dockerEngineVersion = "None";
                if (StringUtils.isBlank(config.getImageInspectorUrl())) {
                    dockerEngineVersion = dockerClientManager.getDockerEngineVersion();
                }
                blackDuckClient.phoneHome(dockerEngineVersion);
            } catch (final Exception e) {
                logger.warn(String.format("Unable to phone home: %s", e.getMessage()));
            }
        }
        initImageName();
        logger.info(String.format("Inspecting image:tag %s:%s", config.getDockerImageRepo(), config.getDockerImageTag()));
        if (config.isOnHost()) {
            blackDuckClient.testBlackDuckConnection();
        }
        return true;
    }

    private void initImageName() throws IntegrationException {
        logger.debug(String.format("initImageName(): dockerImage: %s, dockerTar: %s", config.getDockerImage(), config.getDockerTar()));
        final ImageNameResolver resolver = new ImageNameResolver(config.getDockerImage());
        resolver.getNewImageRepo().ifPresent(repoName -> config.setDockerImageRepo(repoName));
        resolver.getNewImageTag().ifPresent(tagName -> config.setDockerImageTag(tagName));
        logger.debug(String.format("initImageName(): final: dockerImage: %s; dockerImageRepo: %s; dockerImageTag: %s", config.getDockerImage(), config.getDockerImageRepo(), config.getDockerImageTag()));
    }
}
