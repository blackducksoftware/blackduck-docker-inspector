/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.blackduckclient;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.codelocation.CodeLocationCreationData;
import com.synopsys.integration.blackduck.codelocation.bdiolegacy.BdioUploadCodeLocationCreationRequest;
import com.synopsys.integration.blackduck.codelocation.bdiolegacy.BdioUploadService;
import com.synopsys.integration.blackduck.codelocation.upload.UploadBatch;
import com.synopsys.integration.blackduck.codelocation.upload.UploadBatchOutput;
import com.synopsys.integration.blackduck.codelocation.upload.UploadTarget;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder;
import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;
import com.synopsys.integration.blackduck.exception.BlackDuckIntegrationException;
import com.synopsys.integration.blackduck.http.client.BlackDuckHttpClient;
import com.synopsys.integration.blackduck.phonehome.BlackDuckPhoneHomeHelper;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.util.IntEnvironmentVariables;
import com.synopsys.integration.util.NameVersion;
import com.synopsys.integration.util.NoThreadExecutorService;

@Component
public class BlackDuckClient {

    private static final String PHONE_HOME_METADATA_NAME_CALLER_VERSION = "callerVersion";
    private static final String PHONE_HOME_METADATA_NAME_CALLER_NAME = "callerName";
    private static final String PHONE_HOME_METADATA_NAME_DOCKER_ENGINE_VERSION = "dockerEngineVersion";
    private static final String PHONE_HOME_METADATA_NAME_BDIO_BY_LAYER = "bdioOrganizeComponentsByLayer";
    private static final String PHONE_HOME_METADATA_NAME_BDIO_INCLUDE_REMOVED = "bdioIncludeRemovedComponents";
    private static final String PHONE_HOME_METADATA_NAME_PLATFORM_TOP_LAYER_ID_SPECIFIED = "platformTopLayerIdSpecified";

    private final Logger logger = LoggerFactory.getLogger(BlackDuckClient.class);
    private final IntLogger intLogger = new Slf4jIntLogger(logger);

    @Autowired
    private Config config;

    @Autowired
    private BlackDuckSecrets blackDuckSecrets;

    @Autowired
    private ProgramVersion programVersion;

    public void testBlackDuckConnection() throws BlackDuckIntegrationException {
        logger.trace(String.format(
            "Black Duck username: %s",
            getBlackDuckUsername()
        )); // ArgsWithSpacesTest tests this in output
        if (!config.isUploadBdio() || config.isOfflineMode()) {
            logger.debug(
                "Upload of BDIO disabled or offline mode is enabled; skipping verification of Black Duck connection");
            return;
        }
        BlackDuckHttpClient httpConnection;
        try {
            httpConnection = createHttpConnection(intLogger);
            httpConnection.attemptAuthentication();
        } catch (IntegrationException e) {
            String msg = String.format("Error connecting to Black Duck: %s", e.getMessage());
            throw new BlackDuckIntegrationException(msg);
        }
        logger.info("Successful connection to Black Duck.");
    }

    public void uploadBdio(File bdioFile, String codeLocationName, NameVersion projectAndVersion)
        throws IntegrationException {
        if (config.isOfflineMode()) {
            logger.info("Upload of BDIO has been disabled by offline mode");
            return;
        }
        logger.info("Uploading BDIO files.");
        BlackDuckHttpClient httpConnection = createHttpConnection(intLogger);
        BlackDuckServicesFactory blackDuckServicesFactory = createBlackDuckServicesFactory(
            intLogger,
            httpConnection
        );
        BdioUploadService bdioUploadService = blackDuckServicesFactory.createBdioUploadService();

        UploadBatch uploadBatch = new UploadBatch();
        UploadTarget uploadTarget = UploadTarget.createDefault(projectAndVersion, codeLocationName, bdioFile);
        logger.info(String.format("uploading %s", uploadTarget.getUploadFile().getName()));
        uploadBatch.addUploadTarget(uploadTarget);
        BdioUploadCodeLocationCreationRequest uploadRequest = bdioUploadService
            .createUploadRequest(uploadBatch);
        CodeLocationCreationData<UploadBatchOutput> bdioUploadResults = bdioUploadService
            .uploadBdio(uploadRequest);
        bdioUploadResults.getOutput().getOutputs().stream().forEach(o -> logger.debug(String
            .format("\tUpload %s: output: %s%n", o.getCodeLocationName(),
                o.getResponse().orElse("unknown")
            )));
        logger.info(
            String.format("Uploaded bdio file %s to %s", bdioFile.getName(), config.getBlackDuckUrl()));
    }

    private BlackDuckServicesFactory createBlackDuckServicesFactory(
        IntLogger intLogger,
        BlackDuckHttpClient httpConnection
    ) {
        IntEnvironmentVariables intEnvironmentVariables = IntEnvironmentVariables.empty();
        ExecutorService executorService = new NoThreadExecutorService();
        return new BlackDuckServicesFactory(intEnvironmentVariables, executorService, intLogger, httpConnection, new Gson(),
            BlackDuckServicesFactory.createDefaultObjectMapper()
        );
    }

    private String getBlackDuckUsername() {
        return config.getBlackDuckUsername();
    }

    public void phoneHome(String dockerEngineVersion) {
        logger.debug("Attempting to phone home");
        try {
            phoneHomeBlackDuckConnection(dockerEngineVersion);
        } catch (Exception e) {
            logger.debug(String.format(
                "Attempt to phone home failed. This may simply be because Black Duck credentials were not supplied. Error message: %s",
                e.getMessage()
            ));
        }
    }

    private void phoneHomeBlackDuckConnection(String dockerEngineVersion) {

        BlackDuckHttpClient httpConnection = createHttpConnection(intLogger);
        BlackDuckServicesFactory blackDuckServicesFactory = createBlackDuckServicesFactory(
            intLogger,
            httpConnection
        );

        Map<String, String> metaDataMap = new HashMap<>();
        if (StringUtils.isNotBlank(dockerEngineVersion)) {
            metaDataMap
                .put(PHONE_HOME_METADATA_NAME_DOCKER_ENGINE_VERSION, dockerEngineVersion);
        }
        if (StringUtils.isNotBlank(config.getCallerName())) {
            metaDataMap
                .put(PHONE_HOME_METADATA_NAME_CALLER_NAME, config.getCallerName());
        }
        if (StringUtils.isNotBlank(config.getCallerVersion())) {
            metaDataMap
                .put(PHONE_HOME_METADATA_NAME_CALLER_VERSION, config.getCallerVersion());
        }
        metaDataMap.put(
            PHONE_HOME_METADATA_NAME_BDIO_BY_LAYER,
            String.valueOf(config.isOrganizeComponentsByLayer())
        );
        metaDataMap.put(
            PHONE_HOME_METADATA_NAME_BDIO_INCLUDE_REMOVED,
            String.valueOf(config.isIncludeRemovedComponents())
        );
        if (StringUtils.isNotBlank(config.getDockerPlatformTopLayerId())) {
            metaDataMap
                .put(PHONE_HOME_METADATA_NAME_PLATFORM_TOP_LAYER_ID_SPECIFIED, "true");
        }

        BlackDuckPhoneHomeHelper.createPhoneHomeHelper(blackDuckServicesFactory).handlePhoneHome(
            programVersion.getProgramId(), programVersion.getProgramVersion(), metaDataMap);
        logger.trace("Attempt to phone home completed");
    }

    private BlackDuckHttpClient createHttpConnection(IntLogger intLogger) {
        BlackDuckServerConfigBuilder blackDuckServerConfigBuilder = createBlackDuckServerConfigBuilder();
        return blackDuckServerConfigBuilder.build().createBlackDuckHttpClient(intLogger);
    }

    private BlackDuckServerConfigBuilder createBlackDuckServerConfigBuilder() {
        String blackDuckProxyHost = config.getBlackDuckProxyHost();
        String blackDuckProxyPort = config.getBlackDuckProxyPort();
        String blackDuckProxyUsername = config.getBlackDuckProxyUsername();
        String blackDuckProxyPassword = config.getBlackDuckProxyPassword();
        if (StringUtils.isBlank(config.getBlackDuckProxyHost()) && !StringUtils
            .isBlank(config.getScanCliOptsEnvVar())) {
            List<String> scanCliOpts = Arrays.asList(config.getScanCliOptsEnvVar().split("\\s"));
            for (String opt : scanCliOpts) {
                opt = opt.trim();
                if (opt.startsWith("-Dhttp.proxy.host=") || opt.startsWith("-Dhttps.proxy.host=") || opt
                    .startsWith("-Dhttp.proxyHost=") || opt.startsWith("-Dhttps.proxyHost=")) {
                    blackDuckProxyHost = getValue(opt);
                } else if (opt.startsWith("-Dhttp.proxy.port=") || opt.startsWith("-Dhttps.proxy.port=")
                    || opt.startsWith("-Dhttp.proxyPort=") || opt.startsWith("-Dhttps.proxyPort=")) {
                    blackDuckProxyPort = getValue(opt);
                } else if (opt.startsWith("-Dhttp.proxy.username=") || opt
                    .startsWith("-Dhttps.proxy.username=") || opt.startsWith("-Dhttp.proxyUser=") || opt
                    .startsWith("-Dhttps.proxyUser=")) {
                    blackDuckProxyUsername = getValue(opt);
                } else if (opt.startsWith("-Dhttp.proxy.password=") || opt
                    .startsWith("-Dhttps.proxy.password=") || opt.startsWith("-Dhttp.proxyPassword=") || opt
                    .startsWith("-Dhttps.proxyPassword=")) {
                    blackDuckProxyPassword = getValue(opt);
                }
            }
        }
        BlackDuckServerConfigBuilder blackDuckServerConfigBuilder = new BlackDuckServerConfigBuilder();
        blackDuckServerConfigBuilder.setUrl(config.getBlackDuckUrl());
        blackDuckServerConfigBuilder.setApiToken(blackDuckSecrets.getApiToken());
        blackDuckServerConfigBuilder.setUsername(getBlackDuckUsername());
        blackDuckServerConfigBuilder.setPassword(blackDuckSecrets.getPassword());
        blackDuckServerConfigBuilder.setTimeoutInSeconds(config.getBlackDuckTimeout());
        blackDuckServerConfigBuilder.setProxyHost(blackDuckProxyHost);
        blackDuckServerConfigBuilder.setProxyPort(blackDuckProxyPort);
        blackDuckServerConfigBuilder.setProxyUsername(blackDuckProxyUsername);
        blackDuckServerConfigBuilder.setProxyPassword(blackDuckProxyPassword);
        blackDuckServerConfigBuilder.setTrustCert(config.isBlackDuckAlwaysTrustCert());
        return blackDuckServerConfigBuilder;
    }

    private String getValue(String nameEqualsValue) {
        List<String> nameValue = Arrays.asList(nameEqualsValue.split("="));
        String value = null;
        if (nameValue.size() == 2) {
            value = nameValue.get(1);
        }
        return value;
    }
}
