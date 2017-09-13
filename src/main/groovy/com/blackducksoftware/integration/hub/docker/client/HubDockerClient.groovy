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
package com.blackducksoftware.integration.hub.docker.client


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

    @Value('${docker.registry}')
    String dockerRegistry

    @Value('${docker.registry.username}')
    String dockerRegistryUsername

    @Value('${docker.registry.password}')
    String dockerRegistryPassword

    @Value('${command.timeout}')
    long commandTimeout

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
    //
    //    @Value('${docker.registry.email}')
    //    String dockerRegistryEmail
    //////////////////////////////////////////////////////////

    private DockerClient dockerClient

    DockerClient getDockerClient(){
        if(dockerClient == null){
            loginAuthenticatedRegistry()
            // Docker client uses the system properties for proxies
            // http.proxyHost , http.proxyPort, http.proxyUser, http.proxyPassword
            Properties properties = (Properties) System.getProperties().clone()
            properties = DefaultDockerClientConfig.loadIncludedDockerProperties(properties)
            logger.debug(String.format("Docker properties: %s", properties.toString()));

            DefaultDockerClientConfig.Builder builder = new DefaultDockerClientConfig.Builder().withProperties(properties)
            if (StringUtils.isNotBlank(dockerHost)) {
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
            dockerClient = DockerClientBuilder.getInstance(config).build();
        }
        dockerClient
    }

    private void loginAuthenticatedRegistry(){
        if(StringUtils.isNotBlank(dockerRegistryUsername) && StringUtils.isNotBlank(dockerRegistryPassword)){
            logger.debug(String.format("Logging into docker as %s", dockerRegistryUsername));
            String command = "docker login -u=${dockerRegistryUsername} -p=${dockerRegistryPassword}"
            if(StringUtils.isNotBlank(dockerRegistry)){
                command += " ${dockerRegistry}"
            }
            try {
                def standardOut = new StringBuilder()
                def standardError = new StringBuilder()
                def process = command.execute()
                process.consumeProcessOutput(standardOut, standardError)
                process.waitForOrKill(commandTimeout)
                logger.debug(standardOut.toString())
                logger.debug(standardError.toString())
            } catch(Exception e) {
                logger.error("Error executing command {}",command,e)
            }

        }
    }
}