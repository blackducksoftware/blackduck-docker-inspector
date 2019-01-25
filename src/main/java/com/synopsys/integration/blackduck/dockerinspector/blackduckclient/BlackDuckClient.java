/**
 * blackduck-docker-inspector
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
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


import com.synopsys.integration.blackduck.codelocation.CodeLocationCreationData;
import com.synopsys.integration.blackduck.codelocation.bdioupload.BdioUploadCodeLocationCreationRequest;
import com.synopsys.integration.blackduck.codelocation.bdioupload.BdioUploadService;
import com.synopsys.integration.blackduck.codelocation.bdioupload.UploadBatch;

import com.synopsys.integration.blackduck.codelocation.bdioupload.UploadBatchOutput;
import com.synopsys.integration.blackduck.codelocation.bdioupload.UploadTarget;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder;
import com.synopsys.integration.blackduck.exception.BlackDuckIntegrationException;
import com.synopsys.integration.blackduck.phonehome.BlackDuckPhoneHomeHelper;
import com.synopsys.integration.blackduck.rest.BlackDuckHttpClient;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.log.IntLogger;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import com.synopsys.integration.blackduck.dockerinspector.DockerInspector;
import com.synopsys.integration.blackduck.dockerinspector.ProgramVersion;
import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.Slf4jIntLogger;

@Component
public class BlackDuckClient {

  private static final String PHONE_HOME_METADATA_NAME_CALLER_VERSION = "callerVersion";
  private static final String PHONE_HOME_METADATA_NAME_CALLER_NAME = "callerName";
  private static final String PHONE_HOME_METADATA_NAME_DOCKER_ENGINE_VERSION = "dockerEngineVersion";
  private static final String PHONE_HOME_METADATA_NAME_BDIO_BY_LAYER = "bdioOrganizeComponentsByLayer";
  private static final String PHONE_HOME_METADATA_NAME_BDIO_INCLUDE_REMOVED = "bdioIncludeRemovedComponents";

  private final Logger logger = LoggerFactory.getLogger(BlackDuckClient.class);
  private final IntLogger intLogger = new Slf4jIntLogger(logger);

  @Autowired
  private Config config;

  @Autowired
  private BlackDuckSecrets blackDuckSecrets;

  @Autowired
  private ProgramVersion programVersion;

  public void testBlackDuckConnection() throws BlackDuckIntegrationException {
    logger.trace(String.format("Black Duck username: %s",
        getBlackDuckUsername())); // ArgsWithSpacesTest tests this in output
    if (!config.isUploadBdio() || config.isOfflineMode()) {
      logger.debug(
          "Upload of BDIO disabled or offline mode is enabled; skipping verification of Black Duck connection");
      return;
    }
    BlackDuckHttpClient restConnection;
    try {
      restConnection = createRestConnection(intLogger);
      restConnection.attemptAuthentication();
    } catch (final IntegrationException e) {
      final String msg = String.format("Error connecting to Black Duck: %s", e.getMessage());
      throw new BlackDuckIntegrationException(msg);
    }
    logger.info("Successful connection to Black Duck.");
  }

  public void uploadBdio(final File bdioFile, final String codeLocationName)
      throws IntegrationException {
    if (config.isOfflineMode()) {
      logger.info("Upload of BDIO has been disabled by offline mode");
      return;
    }
    logger.info("Uploading BDIO files.");
    final BlackDuckHttpClient restConnection = createRestConnection(intLogger);
    final BlackDuckServicesFactory blackDuckServicesFactory = createBlackDuckServicesFactory(
        intLogger,
        restConnection);
    final BdioUploadService bdioUploadService = blackDuckServicesFactory.createBdioUploadService();

    UploadBatch uploadBatch = new UploadBatch();
    UploadTarget uploadTarget = UploadTarget.createDefault(codeLocationName, bdioFile);
    logger.info(String.format("uploading %s", uploadTarget.getUploadFile().getName()));
    uploadBatch.addUploadTarget(uploadTarget);
    BdioUploadCodeLocationCreationRequest uploadRequest = bdioUploadService
        .createUploadRequest(uploadBatch);
    final CodeLocationCreationData<UploadBatchOutput> bdioUploadResults = bdioUploadService
        .uploadBdio(uploadRequest);
    bdioUploadResults.getOutput().getOutputs().stream().forEach(o -> logger.debug(String
        .format("\tUpload %s: result: %s\n", o.getCodeLocationName(),
            o.getResponse().orElse("unknown"))));
    logger.info(
        String.format("Uploaded bdio file %s to %s", bdioFile.getName(), config.getBlackDuckUrl()));
  }

  private BlackDuckServicesFactory createBlackDuckServicesFactory(final IntLogger intLogger,
      BlackDuckHttpClient restConnection) {
    return new BlackDuckServicesFactory(
        new Gson(), BlackDuckServicesFactory.createDefaultObjectMapper(), restConnection,
        intLogger);
  }

  private String getBlackDuckUsername() {
    return config.getBlackDuckUsername();
  }

  public void phoneHome(final String dockerEngineVersion) {
    logger.debug("Attempting to phone home");
    try {
      phoneHomeBlackDuckConnection(dockerEngineVersion);
    } catch (final Throwable e) {
      logger.debug(String.format(
          "Attempt to phone home failed. This may simply be because Black Duck credentials were not supplied. Error message: %s",
          e.getMessage()));
    }
  }

  private void phoneHomeBlackDuckConnection(final String dockerEngineVersion) throws IOException {

    final BlackDuckHttpClient restConnection = createRestConnection(intLogger);
    final BlackDuckServicesFactory blackDuckServicesFactory = createBlackDuckServicesFactory(
        intLogger,
        restConnection);

    Map<String, String> metaDataMap = new HashMap<>();
    if (!StringUtils.isBlank(dockerEngineVersion)) {
      metaDataMap
          .put(PHONE_HOME_METADATA_NAME_DOCKER_ENGINE_VERSION, dockerEngineVersion);
    }
    if (!StringUtils.isBlank(config.getCallerName())) {
      metaDataMap
          .put(PHONE_HOME_METADATA_NAME_CALLER_NAME, config.getCallerName());
    }
    if (!StringUtils.isBlank(config.getCallerVersion())) {
      metaDataMap
          .put(PHONE_HOME_METADATA_NAME_CALLER_VERSION, config.getCallerVersion());
    }
    metaDataMap.put(PHONE_HOME_METADATA_NAME_BDIO_BY_LAYER,
        String.valueOf(config.isOrganizeComponentsByLayer()));
    metaDataMap.put(PHONE_HOME_METADATA_NAME_BDIO_INCLUDE_REMOVED,
        String.valueOf(config.isIncludeRemovedComponents()));

    BlackDuckPhoneHomeHelper.createPhoneHomeHelper(blackDuckServicesFactory).handlePhoneHome(
        DockerInspector.PROGRAM_ID, programVersion.getProgramVersion(), metaDataMap);
    logger.trace("Attempt to phone home completed");
  }

  private BlackDuckHttpClient createRestConnection(final IntLogger intLogger) throws IllegalStateException {
    final BlackDuckServerConfigBuilder blackDuckServerConfigBuilder = createBlackDuckServerConfigBuilder();
    return blackDuckServerConfigBuilder.build().createBlackDuckHttpClient(intLogger);
  }

  private BlackDuckServerConfigBuilder createBlackDuckServerConfigBuilder() {
    String blackDuckProxyHost = config.getBlackDuckProxyHost();
    String blackDuckProxyPort = config.getBlackDuckProxyPort();
    String blackDuckProxyUsername = config.getBlackDuckProxyUsername();
    String blackDuckProxyPassword = config.getBlackDuckProxyPassword();
    if (StringUtils.isBlank(config.getBlackDuckProxyHost()) && !StringUtils
        .isBlank(config.getScanCliOptsEnvVar())) {
      final List<String> scanCliOpts = Arrays.asList(config.getScanCliOptsEnvVar().split("\\s"));
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
    final BlackDuckServerConfigBuilder blackDuckServerConfigBuilder = new BlackDuckServerConfigBuilder();
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
