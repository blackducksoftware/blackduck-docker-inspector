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
package com.blackducksoftware.integration.hub.docker.imageinspector.lib;

import com.blackducksoftware.integration.hub.bdio.model.SimpleBdioDocument;
import com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.ImageInfoParsed;
import com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.manifest.ManifestLayerMapping;

public class ImageInfoDerived {
    private final ImageInfoParsed imageInfoParsed;
    private String architecture = null;
    private String imageDirName = null;
    private ManifestLayerMapping manifestLayerMapping = null;
    private String pkgMgrFilePath = null;

    private String codeLocationName = null;
    private String finalProjectName = null;
    private String finalProjectVersionName = null;

    private SimpleBdioDocument bdioDocument = null;

    public ImageInfoDerived(final ImageInfoParsed imageInfoParsed) {
        this.imageInfoParsed = imageInfoParsed;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(final String architecture) {
        this.architecture = architecture;
    }

    public String getImageDirName() {
        return imageDirName;
    }

    public void setImageDirName(final String imageDirName) {
        this.imageDirName = imageDirName;
    }

    public ManifestLayerMapping getManifestLayerMapping() {
        return manifestLayerMapping;
    }

    public void setManifestLayerMapping(final ManifestLayerMapping manifestLayerMapping) {
        this.manifestLayerMapping = manifestLayerMapping;
    }

    public String getPkgMgrFilePath() {
        return pkgMgrFilePath;
    }

    public void setPkgMgrFilePath(final String pkgMgrFilePath) {
        this.pkgMgrFilePath = pkgMgrFilePath;
    }

    public ImageInfoParsed getImageInfoParsed() {
        return imageInfoParsed;
    }

    public String getCodeLocationName() {
        return codeLocationName;
    }

    public void setCodeLocationName(final String codeLocationName) {
        this.codeLocationName = codeLocationName;
    }

    public String getFinalProjectName() {
        return finalProjectName;
    }

    public void setFinalProjectName(final String finalProjectName) {
        this.finalProjectName = finalProjectName;
    }

    public String getFinalProjectVersionName() {
        return finalProjectVersionName;
    }

    public void setFinalProjectVersionName(final String finalProjectVersionName) {
        this.finalProjectVersionName = finalProjectVersionName;
    }

    public SimpleBdioDocument getBdioDocument() {
        return bdioDocument;
    }

    public void setBdioDocument(final SimpleBdioDocument bdioDocument) {
        this.bdioDocument = bdioDocument;
    }

}
