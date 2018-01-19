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
package com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker;

import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import com.blackducksoftware.integration.hub.docker.imageinspector.lib.OperatingSystemEnum;

public class ImageInfoParsed {
    private final String fileSystemRootDirName;
    private final OperatingSystemEnum operatingSystemEnum;
    private final ImagePkgMgr pkgMgr;

    public ImageInfoParsed(final String fileSystemRootDirName, final OperatingSystemEnum operatingSystemEnum, final ImagePkgMgr pkgMgr) {
        this.fileSystemRootDirName = fileSystemRootDirName;
        this.operatingSystemEnum = operatingSystemEnum;
        this.pkgMgr = pkgMgr;
    }

    public String getFileSystemRootDirName() {
        return fileSystemRootDirName;
    }

    public OperatingSystemEnum getOperatingSystemEnum() {
        return operatingSystemEnum;
    }

    public ImagePkgMgr getPkgMgr() {
        return pkgMgr;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, RecursiveToStringStyle.JSON_STYLE);
    }
}
