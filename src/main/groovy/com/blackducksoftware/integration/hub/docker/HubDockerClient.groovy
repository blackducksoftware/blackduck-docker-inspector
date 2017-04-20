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
import com.github.dockerjava.core.DockerClientConfig

@Component
class HubDockerClient {

    private final Logger logger = LoggerFactory.getLogger(HubDockerClient.class)

    @Value('${docker.host}')
    String dockerHost

    // Have not tested these, they may also be ignored //
    @Value('${docker.tls.verify}')
    Boolean dockerTlsVerify

    @Value('${docker.cert.path}')
    String dockerCertPath
    /////////////////////////////////////////////////////

    ////// These seem to be ignored by the DockerClient /////
    //    @Value('${docker.config}')
    //    String dockerConfig
    //
    //    @Value('${docker.api.version}')
    //    String dockerApiVersion
    //
    //    @Value('${docker.registry.url}')
    //    String dockerRegistryUrl
    //
    //    @Value('${docker.registry.username}')
    //    String dockerRegistryUsername
    //
    //    @Value('${docker.registry.password}')
    //    String dockerRegistryPassword
    //
    //    @Value('${docker.registry.email}')
    //    String dockerRegistryEmail
    //////////////////////////////////////////////////////////

    DockerClient getDockerClient(){
        // Docker client uses the system properties for proxies
        // http.proxyHost , http.proxyPort, http.proxyUser, http.proxyPassword
        Properties properties = (Properties) System.getProperties().clone()
        properties = DefaultDockerClientConfig.loadIncludedDockerProperties(properties)

        DefaultDockerClientConfig.Builder builder = new DefaultDockerClientConfig.Builder().withProperties(properties)
        if(StringUtils.isNotBlank(dockerHost)){
            builder.withDockerHost(dockerHost)
        }
        if(dockerTlsVerify != null){
            builder.withDockerTlsVerify(dockerTlsVerify)
        }
        if(StringUtils.isNotBlank(dockerCertPath)){
            builder.withDockerCertPath(dockerCertPath)
        }
        //        if(StringUtils.isNotBlank(dockerConfig)){
        //            builder.withDockerConfig(dockerConfig)
        //        }
        //        if(StringUtils.isNotBlank(dockerApiVersion)){
        //            builder.withApiVersion(dockerApiVersion)
        //        }
        //        if(StringUtils.isNotBlank(dockerRegistryUrl)){
        //            builder.withRegistryUrl(dockerRegistryUrl)
        //        }
        //        if(StringUtils.isNotBlank(dockerRegistryUsername)){
        //            builder .withRegistryUsername(dockerRegistryUsername)
        //        }
        //        if(StringUtils.isNotBlank(dockerRegistryPassword)){
        //            builder.withRegistryPassword(dockerRegistryPassword)
        //        }
        //        if(StringUtils.isNotBlank(dockerRegistryEmail)){
        //            builder.withRegistryEmail(dockerRegistryEmail)
        //        }

        DockerClientConfig config = builder.build()
        DockerClientBuilder.getInstance(config).build();
    }
}