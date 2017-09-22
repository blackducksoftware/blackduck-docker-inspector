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
package com.blackducksoftware.integration.hub.docker.image;

import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum;

class DockerImage {
    private final OperatingSystemEnum os;
    private final String imageName;
    private final String imageVersion;

    public DockerImage(final OperatingSystemEnum os, final String imageName, final String imageVersion) {
        this.os = os;
        this.imageName = imageName;
        this.imageVersion = imageVersion;
    }

    OperatingSystemEnum getOs() {
        return os;
    }

    String getImageName() {
        return imageName;
    }

    String getImageVersion() {
        return imageVersion;
    }

}
