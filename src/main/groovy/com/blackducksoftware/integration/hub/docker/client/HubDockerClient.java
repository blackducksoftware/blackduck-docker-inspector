/**
 * Hub Docker Inspector
 *
 * Copyright (C) 2017 Black Duck Software, Inc.
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
package com.blackducksoftware.integration.hub.docker.client;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.docker.executor.Executor;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig.Builder;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

@Component
class HubDockerClient {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    Executor executor;

    @Value("${docker.host}")
    String dockerHost;

    @Value("${docker.registry}")
    String dockerRegistry;

    @Value("${docker.registry.username}")
    String dockerRegistryUsername;

    @Value("${docker.registry.password}")
    String dockerRegistryPassword;

    @Value("${command.timeout}")
    long commandTimeout;

    // Have not tested these, they may also be ignored //
    @Value("${docker.tls.verify}")
    Boolean dockerTlsVerify;

    @Value("${docker.cert.path}")
    String dockerCertPath;
    /////////////////////////////////////////////////////

    ////// These seem to be ignored by the DockerClient /////
    // @Value("${docker.config}")
    // String dockerConfig
    //
    // @Value("${docker.api.version}")
    // String dockerApiVersion
    //
    //
    // @Value("${docker.registry.email}")
    // String dockerRegistryEmail
    //////////////////////////////////////////////////////////

    private DockerClient dockerClient;

    DockerClient getDockerClient() throws HubIntegrationException {
        if (dockerClient == null) {
            // loginAuthenticatedRegistry();
            // Docker client uses the system properties for proxies
            // http.proxyHost , http.proxyPort, http.proxyUser, http.proxyPassword
            final Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder();
            if (StringUtils.isNotBlank(dockerHost)) {
                builder.withDockerHost(dockerHost);
            }
            if (dockerTlsVerify != null) {
                builder.withDockerTlsVerify(dockerTlsVerify);
            }
            if (StringUtils.isNotBlank(dockerCertPath)) {
                builder.withDockerCertPath(dockerCertPath);
            }
            // if(StringUtils.isNotBlank(dockerConfig)){
            // builder.withDockerConfig(dockerConfig)
            // }
            // if(StringUtils.isNotBlank(dockerApiVersion)){
            // builder.withApiVersion(dockerApiVersion)
            // }
            // if(StringUtils.isNotBlank(dockerRegistryUrl)){
            // builder.withRegistryUrl(dockerRegistryUrl)
            // }
            // if(StringUtils.isNotBlank(dockerRegistryUsername)){
            // builder .withRegistryUsername(dockerRegistryUsername)
            // }
            // if(StringUtils.isNotBlank(dockerRegistryPassword)){
            // builder.withRegistryPassword(dockerRegistryPassword)
            // }
            // if(StringUtils.isNotBlank(dockerRegistryEmail)){
            // builder.withRegistryEmail(dockerRegistryEmail)
            // }

            final DockerClientConfig config = builder.build();
            logger.debug(String.format("docker host: %s", config.getDockerHost()));
            logger.debug(String.format("docker username: %s", config.getRegistryUsername()));
            dockerClient = DockerClientBuilder.getInstance(config).build();
        }
        return dockerClient;
    }

    private void loginAuthenticatedRegistry() throws HubIntegrationException {
        if (StringUtils.isNotBlank(dockerRegistryUsername) && StringUtils.isNotBlank(dockerRegistryPassword)) {
            logger.debug(String.format("Logging into docker as %s", dockerRegistryUsername));
            String command = "docker login -u=${dockerRegistryUsername} -p=${dockerRegistryPassword}";
            if (StringUtils.isNotBlank(dockerRegistry)) {
                command += " ${dockerRegistry}";
            }
            try {
                executor.executeCommand(command);
            } catch (HubIntegrationException | IOException | InterruptedException e) {
                final String msg = String.format("Error executing command: %s; error: %s", command, e.getMessage());
                throw new HubIntegrationException(msg, e);
            }
        }
    }
}
