/*
 * Copyright (C) 2017 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Black Duck Software.
 */
package com.blackducksoftware.integration.hub.docker

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.DockerClientConfig

@Component
class HubDockerClient {

    private final Logger logger = LoggerFactory.getLogger(HubDockerClient.class)

    @Value('${docker.host}')
    String dockerHost

    @Value('${docker.tls.verify}')
    Boolean dockerTlsVerify

    @Value('${docker.cert.path}')
    String dockerCertPath

    @Value('${docker.config}')
    String dockerConfig

    @Value('${docker.api.version}')
    String dockerApiVersion

    @Value('${docker.registry.url}')
    String dockerRegistryUrl

    @Value('${docker.registry.username}')
    String dockerRegistryUsername

    @Value('${docker.registry.password}')
    String dockerRegistryPassword

    @Value('${docker.registry.email}')
    String dockerRegistryEmail



    DockerClient getDockerClient(){
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .withDockerTlsVerify(dockerTlsVerify)
                .withDockerCertPath(dockerCertPath)
                .withDockerConfig(dockerConfig)
                .withApiVersion(dockerApiVersion)
                .withRegistryUrl(dockerRegistryUrl)
                .withRegistryUsername(dockerRegistryUsername)
                .withRegistryPassword(dockerRegistryPassword)
                .withRegistryEmail(dockerRegistryEmail)
                .build();
        DockerClientBuilder.getInstance(config).build();
    }
}
