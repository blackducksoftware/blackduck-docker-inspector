/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Optional;

import javax.annotation.PostConstruct;

import com.synopsys.integration.blackduck.imageinspector.image.common.RepoTag;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.dockerinspector.blackduckclient.BlackDuckClient;
import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.DockerInspectorSystemProperties;
import com.synopsys.integration.blackduck.dockerinspector.config.ProgramPaths;
import com.synopsys.integration.blackduck.dockerinspector.dockerclient.DockerClientManager;
import com.synopsys.integration.blackduck.dockerinspector.exception.HelpGenerationException;
import com.synopsys.integration.blackduck.dockerinspector.help.HelpWriter;
import com.synopsys.integration.blackduck.dockerinspector.httpclient.HttpClientInspector;
import com.synopsys.integration.blackduck.dockerinspector.output.Output;
import com.synopsys.integration.blackduck.dockerinspector.output.Result;
import com.synopsys.integration.blackduck.dockerinspector.output.ResultFile;
import com.synopsys.integration.blackduck.dockerinspector.programarguments.ArgumentParser;
import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;
import com.synopsys.integration.blackduck.imageinspector.api.name.ImageNameResolver;
import com.synopsys.integration.exception.IntegrationException;

@SpringBootApplication
@ComponentScan(basePackages = { "com.synopsys.integration.blackduck.imageinspector", "com.synopsys.integration.blackduck.dockerinspector" })
public class DockerInspector {
    private static final Logger logger = LoggerFactory.getLogger(DockerInspector.class);

    private static final String DETECT_CALLER_NAME = "Detect";

    @Autowired
    private BlackDuckClient blackDuckClient;

    @Autowired
    private DockerClientManager dockerClientManager;

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
    private HttpClientInspector inspector;

    @Autowired
    private HelpWriter helpWriter;

    @Autowired
    private Output output;

    @Autowired
    private DockerInspectorSystemProperties dockerInspectorSystemProperties;

    public static void main(String[] args) {
        SpringApplicationBuilder appBuilder = new SpringApplicationBuilder(DockerInspector.class);
        appBuilder.logStartupInfo(false);
        appBuilder.bannerMode(Banner.Mode.OFF);
        appBuilder.run(args);
        logger.warn("The program is not expected to get here.");
    }

    @PostConstruct
    public void inspectImage() {
        Result result = null;
        try {
            if (!initAndValidate(config)) {
                System.exit(0);
            }
            result = inspector.getBdio();
        } catch (HelpGenerationException helpGenerationException) {
            String msg = String.format("Error generating help: %s", helpGenerationException.getMessage());
            logger.error(msg);
            logStackTraceIfDebug(helpGenerationException);
            result = Result.createResultFailure(msg);
        } catch (Exception e) {
            String msg = String.format("Error inspecting image: %s", e.getMessage());
            logger.error(msg);
            logStackTraceIfDebug(e);
            result = Result.createResultFailure(msg);
        }
        File resultsFile = new File(output.getFinalOutputDir(), programPaths.getDockerInspectorResultsFilename());
        resultFile.write(new Gson(), resultsFile, result);
        int returnCode = result.getReturnCode();
        logger.info(String.format("Returning %d", returnCode));
        System.exit(returnCode);
    }

    private void logStackTraceIfDebug(Exception e) {
        String trace = ExceptionUtils.getStackTrace(e);
        logger.debug(String.format("Stack trace: %s", trace));
    }

    private boolean helpInvoked() {
        logger.debug("Checking to see if help argument passed");
        if (applicationArguments == null) {
            logger.debug("applicationArguments is null");
            return false;
        }
        String[] args = applicationArguments.getSourceArgs();
        if (contains(args, "-h") || contains(args, "--help") || contains(args, "--help=true")) {
            logger.debug("Help argument passed");
            return true;
        }
        return false;
    }

    private String getHelpTopics() {
        ArgumentParser argumentParser = new ArgumentParser(applicationArguments.getSourceArgs());
        String argFollowingHelpFlag = argumentParser.findValueForCommand("-h", "--help");
        if (StringUtils.isBlank(argFollowingHelpFlag) || argFollowingHelpFlag.startsWith("-")) {
            return null;
        }
        return argFollowingHelpFlag;
    }

    private boolean contains(String[] stringsToSearch, String targetString) {
        for (String stringToTest : stringsToSearch) {
            if (targetString.equals(stringToTest)) {
                return true;
            }
        }
        return false;
    }

    private boolean initAndValidate(Config config) throws IntegrationException, FileNotFoundException {
        if (!DETECT_CALLER_NAME.equals(config.getCallerName())) {
            logger.warn("*** Support for running Docker Inspector as a standalone utility is being dropped.  In the future, users should run Docker Inspector via Detect. ***");
        }

        logger.info(String.format("Black Duck Docker Inspector %s", programVersion.getProgramVersion()));
        logger.debug(String.format("Java version: %s", System.getProperty("java.version")));
        if (helpInvoked()) {
            provideHelp(config);
            return false;
        }
        dockerInspectorSystemProperties.augmentSystemProperties(config.getSystemPropertiesPath());
        logger.debug(String.format("running from dir: %s", System.getProperty("user.dir")));
        logger.trace(String.format("dockerImageTag: %s", config.getDockerImageTag()));
        logger.trace(String.format("Black Duck project: %s, version: %s;", config.getBlackDuckProjectName(), config.getBlackDuckProjectVersion()));
        if (config.isPhoneHome() && !config.isOfflineMode()) {
            logger.debug("PhoneHome enabled");
            try {
                blackDuckClient.phoneHome(deriveDockerEngineVersion(config));
            } catch (Exception e) {
                logger.warn(String.format("Unable to phone home: %s", e.getMessage()));
            }
        }
        initImageName();
        logger.info(String.format("Inspecting image:tag %s:%s (platform: %s)",
            config.getDockerImageRepo(), config.getDockerImageTag(),
            Optional.ofNullable(config.getDockerImagePlatform()).orElse("<unspecified>")));
        blackDuckClient.testBlackDuckConnection();
        return true;
    }

    private void provideHelp(Config config) throws FileNotFoundException, HelpGenerationException {
        String givenHelpOutputFilePath = config.getHelpOutputFilePath();
        if (StringUtils.isBlank(givenHelpOutputFilePath)) {
            helpWriter.concatinateContentToPrintStream(System.out, getHelpTopics());
        } else {
            File helpOutputFile = new File(givenHelpOutputFilePath);
            if ((!helpOutputFile.isDirectory())) {
                try (PrintStream helpPrintStream = new PrintStream(new FileOutputStream(helpOutputFile))) {
                    helpWriter.concatinateContentToPrintStream(helpPrintStream, getHelpTopics());
                }
            } else {
                helpWriter.writeIndividualFilesToDir(new File(givenHelpOutputFilePath), getHelpTopics());
            }
        }
    }

    private String deriveDockerEngineVersion(Config config) {
        String dockerEngineVersion = "None";
        if (StringUtils.isBlank(config.getImageInspectorUrl())) {
            dockerEngineVersion = dockerClientManager.getDockerEngineVersion();
        }
        return dockerEngineVersion;
    }

    private void initImageName() {
        logger.debug(String.format("initImageName(): dockerImage: %s, dockerTar: %s", config.getDockerImage(), config.getDockerTar()));
        ImageNameResolver resolver = new ImageNameResolver();
        RepoTag resolvedRepoTag = resolver.resolve(config.getDockerImage(), null, null);
        if (resolvedRepoTag.getRepo().isPresent()) {
            config.setDockerImageRepo(resolvedRepoTag.getRepo().get());
        }
        if (resolvedRepoTag.getTag().isPresent()) {
            config.setDockerImageTag(resolvedRepoTag.getTag().get());
        }
        logger.debug(String.format("initImageName(): final: dockerImage: %s; dockerImageRepo: %s; dockerImageTag: %s", config.getDockerImage(), config.getDockerImageRepo(), config.getDockerImageTag()));
    }
}
