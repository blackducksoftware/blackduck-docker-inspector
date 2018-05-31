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
package com.blackducksoftware.integration.hub.docker.dockerinspector.restclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.imageinspector.lib.PackageManagerEnum;

public class BdioFilename {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String spdxName;
    private final String projectExternalIdMetaName;
    private final String projectExternalIdMetaVersion;
    private final String projectExternalIdMetaForgeName;

    public BdioFilename(final String spdxName, final String projectExternalIdMetaName, final String projectExternalIdMetaVersion, final String projectExternalIdMetaForgeName) {
        this.spdxName = spdxName;
        this.projectExternalIdMetaName = projectExternalIdMetaName;
        this.projectExternalIdMetaVersion = projectExternalIdMetaVersion;
        this.projectExternalIdMetaForgeName = projectExternalIdMetaForgeName;
    }

    public String getBdioFilename() throws IntegrationException {
        final String bdioFilename = String.format("%s_%s_%s_%s_bdio.jsonld", projectExternalIdMetaName, getPkgMgrLibDir(projectExternalIdMetaForgeName, spdxName),
                projectExternalIdMetaName, projectExternalIdMetaVersion);
        return bdioFilename;
    }

    private String getPkgMgrLibDir(final String projectExternalIdMetaForgeName, final String spdxName) throws IntegrationException {
        final PackageManagerEnum pkgMgr = getPkgMgrName(projectExternalIdMetaForgeName);
        return pkgMgr.getDirectory().substring(1).replaceAll("/", "_");
    }

    private PackageManagerEnum getPkgMgrName(final String projectExternalIdMetaForgeName) throws IntegrationException {
        // TODO this capability belongs in PackageManagerEnum
        if (PackageManagerEnum.APK.getOperatingSystem().name().equalsIgnoreCase(projectExternalIdMetaForgeName)) {
            return PackageManagerEnum.APK;
        }
        if (PackageManagerEnum.RPM.getOperatingSystem().name().equalsIgnoreCase(projectExternalIdMetaForgeName)) {
            return PackageManagerEnum.RPM;
        }
        if (PackageManagerEnum.DPKG.getOperatingSystem().name().equalsIgnoreCase(projectExternalIdMetaForgeName)) {
            return PackageManagerEnum.DPKG;
        }
        throw new IntegrationException(String.format("Unrecognized forge name in BDIO result: %s", projectExternalIdMetaForgeName));
    }

}
