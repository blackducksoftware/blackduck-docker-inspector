/**
 * blackduck-docker-inspector
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
package com.synopsys.integration.blackduck.dockerinspector.restclient;

import com.google.common.base.Optional;
import com.synopsys.integration.blackduck.imageinspector.lib.PackageManagerEnum;
import com.synopsys.integration.exception.IntegrationException;

public class BdioFilename {
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
        final Optional<PackageManagerEnum> pkgMgr = getPkgMgrName(projectExternalIdMetaForgeName);
        if (pkgMgr.isPresent()) {
            return pkgMgr.get().getDirectory().substring(1).replaceAll("/", "_");
        } else {
            return "noPkgMgr";
        }
    }

    private Optional<PackageManagerEnum> getPkgMgrName(final String projectExternalIdMetaForgeName) throws IntegrationException {
        if (PackageManagerEnum.APK.getForge().getName().equalsIgnoreCase(projectExternalIdMetaForgeName)) {
            return Optional.of(PackageManagerEnum.APK);
        }
        if (PackageManagerEnum.RPM.getForge().getName().equalsIgnoreCase(projectExternalIdMetaForgeName)) {
            return Optional.of(PackageManagerEnum.RPM);
        }
        if (PackageManagerEnum.DPKG.getForge().getName().equalsIgnoreCase(projectExternalIdMetaForgeName)) {
            return Optional.of(PackageManagerEnum.DPKG);
        }
        return Optional.absent();
    }
}
