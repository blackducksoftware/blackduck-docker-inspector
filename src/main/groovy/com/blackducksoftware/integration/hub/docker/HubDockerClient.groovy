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

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.DefaultDockerClientConfig.Builder

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
        Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder()
        if(StringUtils.isNotBlank(dockerHost)){
            builder.withDockerHost(dockerHost)
        }
        if(StringUtils.isNotBlank(dockerTlsVerify)){
            builder.withDockerTlsVerify(dockerTlsVerify)
        }
        builder.withDockerCertPath(dockerCertPath)
        builder.withDockerConfig(dockerConfig)
        builder.withApiVersion(dockerApiVersion)
        builder.withRegistryUrl(dockerRegistryUrl)
        builder.withRegistryUsername(dockerRegistryUsername)
        builder.withRegistryPassword(dockerRegistryPassword)
        builder.withRegistryEmail(dockerRegistryEmail)

        DefaultDockerClientConfig config =  builder.build()
        logger.info('docker host : '+config.dockerHost)
        logger.info('config : '+config.dockerConfig)
        logger.info('api version : '+config.apiVersion)
        logger.info('auth config : '+config.authConfig)
        logger.info('registry url : '+config.registryUrl)
        logger.info('registry user : '+config.registryUsername)
        logger.info('registry pass : '+config.registryPassword)
        logger.info('registry email : '+config.registryEmail)


        DockerClientBuilder.getInstance(config).build();
    }
}
