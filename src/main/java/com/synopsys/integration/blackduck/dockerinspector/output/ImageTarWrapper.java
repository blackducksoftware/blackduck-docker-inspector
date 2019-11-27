/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2019 Synopsys, Inc.
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
package com.synopsys.integration.blackduck.dockerinspector.output;

import java.io.File;

public class ImageTarWrapper {

    private final File file;
    private final String imageRepo;
    private final String imageTag;

    public ImageTarWrapper(final File file, final String imageRepo, final String imageTag) {
        this.file = file;
        this.imageRepo = imageRepo;
        this.imageTag = imageTag;
    }

    public ImageTarWrapper(final File file) {
        this.file = file;
        this.imageRepo = null;
        this.imageTag = null;
    }

    public File getFile() {
        return file;
    }

    public String getImageRepo() {
        return imageRepo;
    }

    public String getImageTag() {
        return imageTag;
    }
}
