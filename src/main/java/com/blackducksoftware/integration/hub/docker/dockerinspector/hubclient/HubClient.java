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
package com.blackducksoftware.integration.hub.docker.dockerinspector.hubclient;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.exception.EncryptionException;
import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.configuration.HubServerConfigBuilder;
import com.blackducksoftware.integration.hub.docker.dockerinspector.DockerEnvImageInspector;
import com.blackducksoftware.integration.hub.docker.dockerinspector.ProgramVersion;
import com.blackducksoftware.integration.hub.docker.dockerinspector.config.Config;
import com.blackducksoftware.integration.hub.docker.dockerinspector.config.ProgramPaths;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.service.CodeLocationService;
import com.blackducksoftware.integration.hub.service.HubServicesFactory;
import com.blackducksoftware.integration.hub.service.PhoneHomeService;
import com.blackducksoftware.integration.log.Slf4jIntLogger;
import com.blackducksoftware.integration.phonehome.PhoneHomeRequestBody;
import com.blackducksoftware.integration.rest.connection.RestConnection;

@Component
public class HubClient {
    private static final String PHONE_HOME_METADATA_NAME_CALLER_VERSION = "callerVersion";

    private static final String PHONE_HOME_METADATA_NAME_CALLER_NAME = "callerName";

    private final Logger logger = LoggerFactory.getLogger(HubClient.class);

    @Autowired
    private Config config;

    @Autowired
    private HubSecrets hubSecrets;

    @Autowired
    private ProgramVersion programVersion;

    @Autowired
    private ProgramPaths programPaths;

    public boolean isValid() {
        return createHubServerConfigBuilder().isValid();
    }

    public void testHubConnection() throws HubIntegrationException {
        logger.trace(String.format("Hub username: %s", getHubUsername())); // ArgsWithSpacesTest tests this in output
        if (!config.isUploadBdio()) {
            logger.debug("Upload of BDIO not enabled; skipping verification of Hub connection");
            return;
        }
        RestConnection restConnection;
        try {
            restConnection = createRestConnection();
            restConnection.connect();
        } catch (final IntegrationException e) {
            final String msg = String.format("Error connecting to Hub: %s", e.getMessage());
            throw new HubIntegrationException(msg);
        }
        logger.info("Successful connection to the Hub.");
    }

    public void uploadBdioToHub(final File bdioFile) throws IntegrationException {
        final RestConnection restConnection = createRestConnection();
        final HubServicesFactory hubServicesFactory = new HubServicesFactory(restConnection);
        final CodeLocationService bomImportRequestService = hubServicesFactory.createCodeLocationService();
        bomImportRequestService.importBomFile(bdioFile);
        logger.info(String.format("Uploaded bdio file %s to %s", bdioFile.getName(), config.getHubUrl()));
    }

    private String getHubUsername() {
        return programPaths.unEscape(config.getHubUsername());
    }

    public void phoneHome(final String dockerEngineVersion) {
        if (!config.isPhoneHome()) {
            logger.debug("PhoneHome disabled");
            return;
        }
        logger.debug("Attempting to phone home");
        try {
            phoneHomeHubConnection(dockerEngineVersion);
        } catch (final Throwable e) {
            logger.debug(String.format("Attempt to phone home failed. This may simply be because Hub credentials were not supplied. Error message: %s", e.getMessage()));
        }
    }

    private void phoneHomeHubConnection(final String dockerEngineVersion) throws IOException, EncryptionException {
        final RestConnection restConnection = createRestConnection();
        final HubServicesFactory hubServicesFactory = new HubServicesFactory(restConnection);
        final PhoneHomeService phoneHomeService = hubServicesFactory.createPhoneHomeService();
        final PhoneHomeRequestBody.Builder phoneHomeRequestBodyBuilder = phoneHomeService.createInitialPhoneHomeRequestBodyBuilder(DockerEnvImageInspector.PROGRAM_ID, programVersion.getProgramVersion());
        if (!StringUtils.isBlank(config.getCallerName())) {
            phoneHomeRequestBodyBuilder.addToMetaData(PHONE_HOME_METADATA_NAME_CALLER_NAME, config.getCallerName());
        }
        if (!StringUtils.isBlank(config.getCallerVersion())) {
            phoneHomeRequestBodyBuilder.addToMetaData(PHONE_HOME_METADATA_NAME_CALLER_VERSION, config.getCallerVersion());
        }
        phoneHomeService.phoneHome(phoneHomeRequestBodyBuilder);
        logger.trace("Attempt to phone home completed");
    }

    private RestConnection createRestConnection() throws EncryptionException, IllegalStateException {
        final HubServerConfigBuilder hubServerConfigBuilder = createHubServerConfigBuilder();
        return hubServerConfigBuilder.build().createRestConnection(new Slf4jIntLogger(logger));
    }

    private HubServerConfigBuilder createHubServerConfigBuilder() {
        String hubProxyHost = config.getHubProxyHost();
        String hubProxyPort = config.getHubProxyPort();
        String hubProxyUsername = config.getHubProxyUsername();
        String hubProxyPassword = config.getHubProxyPassword();
        if (StringUtils.isBlank(config.getHubProxyHost()) && !StringUtils.isBlank(config.getScanCliOptsEnvVar())) {
            final List<String> scanCliOpts = Arrays.asList(config.getScanCliOptsEnvVar().split("\\s"));
            for (String opt : scanCliOpts) {
                opt = opt.trim();
                if (opt.startsWith("-Dhttp.proxy.host=") || opt.startsWith("-Dhttps.proxy.host=") || opt.startsWith("-Dhttp.proxyHost=") || opt.startsWith("-Dhttps.proxyHost=")) {
                    hubProxyHost = getValue(opt);
                } else if (opt.startsWith("-Dhttp.proxy.port=") || opt.startsWith("-Dhttps.proxy.port=") || opt.startsWith("-Dhttp.proxyPort=") || opt.startsWith("-Dhttps.proxyPort=")) {
                    hubProxyPort = getValue(opt);
                } else if (opt.startsWith("-Dhttp.proxy.username=") || opt.startsWith("-Dhttps.proxy.username=") || opt.startsWith("-Dhttp.proxyUser=") || opt.startsWith("-Dhttps.proxyUser=")) {
                    hubProxyUsername = getValue(opt);
                } else if (opt.startsWith("-Dhttp.proxy.password=") || opt.startsWith("-Dhttps.proxy.password=") || opt.startsWith("-Dhttp.proxyPassword=") || opt.startsWith("-Dhttps.proxyPassword=")) {
                    hubProxyPassword = getValue(opt);
                }
            }
        }
        final HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder();
        hubServerConfigBuilder.setHubUrl(config.getHubUrl());
        hubServerConfigBuilder.setApiToken(hubSecrets.getApiToken());
        hubServerConfigBuilder.setUsername(getHubUsername());
        hubServerConfigBuilder.setPassword(hubSecrets.getPassword());
        hubServerConfigBuilder.setTimeout(config.getHubTimeout());
        hubServerConfigBuilder.setProxyHost(hubProxyHost);
        hubServerConfigBuilder.setProxyPort(hubProxyPort);
        hubServerConfigBuilder.setProxyUsername(hubProxyUsername);
        hubServerConfigBuilder.setProxyPassword(hubProxyPassword);
        hubServerConfigBuilder.setAlwaysTrustServerCertificate(config.isHubAlwaysTrustCert());
        return hubServerConfigBuilder;
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
