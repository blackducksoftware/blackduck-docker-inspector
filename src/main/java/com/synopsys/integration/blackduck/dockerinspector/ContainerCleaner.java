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
package com.synopsys.integration.blackduck.dockerinspector;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.dockerinspector.dockerclient.DockerClientManager;

public class ContainerCleaner implements Callable<String> {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final boolean stopContainer;
    private final boolean removeImage;
    private final DockerClientManager dockerClientManager;
    private final String imageId;
    private final String containerId;

    public ContainerCleaner(final DockerClientManager dockerClientManager, final String imageId, final String containerId, final boolean stopContainer, final boolean removeImage) {
        this.dockerClientManager = dockerClientManager;
        this.imageId = imageId;
        this.containerId = containerId;
        this.stopContainer = stopContainer;
        this.removeImage = removeImage;
    }

    @Override
    public String call() {
        String statusMessage = "Cleanup of container/image: Success";
        try {

            if (stopContainer) {
                logger.info(String.format("Stopping/removing container %s", containerId));
                dockerClientManager.stopRemoveContainer(containerId);
                if (removeImage) {
                    logger.info(String.format("Removing image %s", imageId));
                    dockerClientManager.removeImage(imageId);
                }
            }
            logger.debug(statusMessage);
        } catch (final Throwable e) {
            statusMessage = String.format("Error during cleanup container %s / image %s: %s", containerId, imageId, e.getMessage());
            logger.debug(statusMessage);
        }
        return statusMessage;
    }
}
