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
package com.blackducksoftware.integration.hub.docker;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

public class ImageNameResolver {
    private Optional<String> newImageRepo = Optional.empty();
    private Optional<String> newImageTag = Optional.empty();

    public ImageNameResolver(final String givenImageName) {
        if (StringUtils.isNotBlank(givenImageName)) {
            newImageTag = Optional.of("latest");
            final int lastColonIndex = givenImageName.lastIndexOf(':');
            if (lastColonIndex < 0) {
                newImageRepo = Optional.of(givenImageName);
            } else {
                newImageRepo = Optional.of(givenImageName.substring(0, lastColonIndex));
                if ((lastColonIndex + 1) != givenImageName.length()) {
                    newImageTag = Optional.of(givenImageName.substring(lastColonIndex + 1));
                }
            }
        }
    }

    public Optional<String> getNewImageRepo() {
        return newImageRepo;
    }

    public Optional<String> getNewImageTag() {
        return newImageTag;
    }

}
