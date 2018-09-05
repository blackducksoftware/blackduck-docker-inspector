/**
 * blackduck-docker-inspector
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
package com.synopsys.integration.blackduck.dockerinspector.blackduckclient;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.synopsys.integration.blackduck.configuration.HubServerConfigBuilder;
import com.synopsys.integration.blackduck.dockerinspector.DockerEnvImageInspector;
import com.synopsys.integration.blackduck.dockerinspector.ProgramVersion;
import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.exception.HubIntegrationException;
import com.synopsys.integration.blackduck.rest.BlackduckRestConnection;
import com.synopsys.integration.blackduck.service.CodeLocationService;
import com.synopsys.integration.blackduck.service.HubServicesFactory;
import com.synopsys.integration.exception.EncryptionException;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.phonehome.PhoneHomeCallable;
import com.synopsys.integration.phonehome.PhoneHomeRequestBody;
import com.synopsys.integration.phonehome.PhoneHomeService;

@Component
public class BlackDuckClient {
    private static final String PHONE_HOME_METADATA_NAME_CALLER_VERSION = "callerVersion";

    private static final String PHONE_HOME_METADATA_NAME_CALLER_NAME = "callerName";

    private final Logger logger = LoggerFactory.getLogger(BlackDuckClient.class);

    @Autowired
    private Config config;

    @Autowired
    private BlackDuckSecrets blackDuckSecrets;

    @Autowired
    private ProgramVersion programVersion;

    public boolean isValid() {
        return createBlackDuckServerConfigBuilder().isValid();
    }

    public void testBlackDuckConnection() throws HubIntegrationException {
        logger.trace(String.format("Black Duck username: %s", getBlackDuckUsername())); // ArgsWithSpacesTest tests this in output
        if (!config.isUploadBdio()) {
            logger.debug("Upload of BDIO not enabled; skipping verification of Black Duck connection");
            return;
        }
        BlackduckRestConnection restConnection;
        try {
            restConnection = createRestConnection();
            restConnection.connect();
        } catch (final IntegrationException e) {
            final String msg = String.format("Error connecting to Black Duck: %s", e.getMessage());
            throw new HubIntegrationException(msg);
        }
        logger.info("Successful connection to Black Duck.");
    }

    public void uploadBdio(final File bdioFile) throws IntegrationException {
        final BlackduckRestConnection restConnection = createRestConnection();
        final HubServicesFactory blackDuckServicesFactory = new HubServicesFactory(new Gson(), new JsonParser(), restConnection, new Slf4jIntLogger(logger));
        final CodeLocationService bomImportRequestService = blackDuckServicesFactory.createCodeLocationService();
        bomImportRequestService.importBomFile(bdioFile);
        logger.info(String.format("Uploaded bdio file %s to %s", bdioFile.getName(), config.getBlackDuckUrl()));
    }

    private String getBlackDuckUsername() {
        return config.getBlackDuckUsername();
    }

    public void phoneHome(final String dockerEngineVersion) {
        if (!config.isPhoneHome()) {
            logger.debug("PhoneHome disabled");
            return;
        }
        logger.debug("Attempting to phone home");
        try {
            phoneHomeBlackDuckConnection(dockerEngineVersion);
        } catch (final Throwable e) {
            logger.debug(String.format("Attempt to phone home failed. This may simply be because Black Duck credentials were not supplied. Error message: %s", e.getMessage()));
        }
    }

    private void phoneHomeBlackDuckConnection(final String dockerEngineVersion) throws IOException, EncryptionException {
        final BlackduckRestConnection restConnection = createRestConnection();
        final HubServicesFactory blackDuckServicesFactory = new HubServicesFactory(HubServicesFactory.createDefaultGson(), HubServicesFactory.createDefaultJsonParser(), restConnection, new Slf4jIntLogger(logger));
        final PhoneHomeService phoneHomeService = blackDuckServicesFactory.createPhoneHomeService(Executors.newSingleThreadExecutor());
        final PhoneHomeRequestBody.Builder phoneHomeRequestBodyBuilder = new PhoneHomeRequestBody.Builder();
        if (!StringUtils.isBlank(config.getCallerName())) {
            phoneHomeRequestBodyBuilder.addToMetaData(PHONE_HOME_METADATA_NAME_CALLER_NAME, config.getCallerName());
        }
        if (!StringUtils.isBlank(config.getCallerVersion())) {
            phoneHomeRequestBodyBuilder.addToMetaData(PHONE_HOME_METADATA_NAME_CALLER_VERSION, config.getCallerVersion());
        }
        final PhoneHomeCallable phoneHomeCallable = blackDuckServicesFactory.createBlackDuckPhoneHomeCallable(new URL(config.getBlackDuckUrl()), DockerEnvImageInspector.PROGRAM_ID, programVersion.getProgramVersion(),
                phoneHomeRequestBodyBuilder);
        phoneHomeService.phoneHome(phoneHomeCallable);
        logger.trace("Attempt to phone home completed");
    }

    private BlackduckRestConnection createRestConnection() throws EncryptionException, IllegalStateException {
        final HubServerConfigBuilder blackDuckServerConfigBuilder = createBlackDuckServerConfigBuilder();
        return blackDuckServerConfigBuilder.build().createRestConnection(new Slf4jIntLogger(logger));
    }

    private HubServerConfigBuilder createBlackDuckServerConfigBuilder() {
        String blackDuckProxyHost = config.getBlackDuckProxyHost();
        String blackDuckProxyPort = config.getBlackDuckProxyPort();
        String blackDuckProxyUsername = config.getBlackDuckProxyUsername();
        String blackDuckProxyPassword = config.getBlackDuckProxyPassword();
        if (StringUtils.isBlank(config.getBlackDuckProxyHost()) && !StringUtils.isBlank(config.getScanCliOptsEnvVar())) {
            final List<String> scanCliOpts = Arrays.asList(config.getScanCliOptsEnvVar().split("\\s"));
            for (String opt : scanCliOpts) {
                opt = opt.trim();
                if (opt.startsWith("-Dhttp.proxy.host=") || opt.startsWith("-Dhttps.proxy.host=") || opt.startsWith("-Dhttp.proxyHost=") || opt.startsWith("-Dhttps.proxyHost=")) {
                    blackDuckProxyHost = getValue(opt);
                } else if (opt.startsWith("-Dhttp.proxy.port=") || opt.startsWith("-Dhttps.proxy.port=") || opt.startsWith("-Dhttp.proxyPort=") || opt.startsWith("-Dhttps.proxyPort=")) {
                    blackDuckProxyPort = getValue(opt);
                } else if (opt.startsWith("-Dhttp.proxy.username=") || opt.startsWith("-Dhttps.proxy.username=") || opt.startsWith("-Dhttp.proxyUser=") || opt.startsWith("-Dhttps.proxyUser=")) {
                    blackDuckProxyUsername = getValue(opt);
                } else if (opt.startsWith("-Dhttp.proxy.password=") || opt.startsWith("-Dhttps.proxy.password=") || opt.startsWith("-Dhttp.proxyPassword=") || opt.startsWith("-Dhttps.proxyPassword=")) {
                    blackDuckProxyPassword = getValue(opt);
                }
            }
        }
        final HubServerConfigBuilder blackDuckServerConfigBuilder = new HubServerConfigBuilder();
        blackDuckServerConfigBuilder.setUrl(config.getBlackDuckUrl());
        blackDuckServerConfigBuilder.setApiToken(blackDuckSecrets.getApiToken());
        blackDuckServerConfigBuilder.setUsername(getBlackDuckUsername());
        blackDuckServerConfigBuilder.setPassword(blackDuckSecrets.getPassword());
        blackDuckServerConfigBuilder.setTimeout(config.getBlackDuckTimeout());
        blackDuckServerConfigBuilder.setProxyHost(blackDuckProxyHost);
        blackDuckServerConfigBuilder.setProxyPort(blackDuckProxyPort);
        blackDuckServerConfigBuilder.setProxyUsername(blackDuckProxyUsername);
        blackDuckServerConfigBuilder.setProxyPassword(blackDuckProxyPassword);
        blackDuckServerConfigBuilder.setTrustCert(config.isBlackDuckAlwaysTrustCert());
        return blackDuckServerConfigBuilder;
    }

    private String getValue(final String nameEqualsValue) {
        final List<String> nameValue = Arrays.asList(nameEqualsValue.split("="));
        String value = null;
        if (nameValue.size() == 2) {
            value = nameValue.get(1);
        }
        return value;
    }
}
