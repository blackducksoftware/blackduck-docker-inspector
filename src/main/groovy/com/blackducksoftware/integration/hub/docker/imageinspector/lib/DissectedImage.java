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

import java.io.File;
import java.util.List;

import com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.manifest.ManifestLayerMapping;

public class DissectedImage {
    private File dockerTarFile = null;
    private List<File> layerTars = null;
    private List<ManifestLayerMapping> layerMappings = null;
    private File targetImageFileSystemRootDir = null;
    private OperatingSystemEnum targetOs = null;
    private String runOnImageName = null;
    private String runOnImageTag = null;
    private String bdioFilename = null;

    public DissectedImage() {
    }

    public File getDockerTarFile() {
        return dockerTarFile;
    }

    public void setDockerTarFile(final File dockerTarFile) {
        this.dockerTarFile = dockerTarFile;
    }

    public List<File> getLayerTars() {
        return layerTars;
    }

    public void setLayerTars(final List<File> layerTars) {
        this.layerTars = layerTars;
    }

    public List<ManifestLayerMapping> getLayerMappings() {
        return layerMappings;
    }

    public void setLayerMappings(final List<ManifestLayerMapping> layerMappings) {
        this.layerMappings = layerMappings;
    }

    public File getTargetImageFileSystemRootDir() {
        return targetImageFileSystemRootDir;
    }

    public void setTargetImageFileSystemRootDir(final File targetImageFileSystemRootDir) {
        this.targetImageFileSystemRootDir = targetImageFileSystemRootDir;
    }

    public OperatingSystemEnum getTargetOs() {
        return targetOs;
    }

    public void setTargetOs(final OperatingSystemEnum targetOs) {
        this.targetOs = targetOs;
    }

    public String getRunOnImageName() {
        return runOnImageName;
    }

    public void setRunOnImageName(final String runOnImageName) {
        this.runOnImageName = runOnImageName;
    }

    public String getRunOnImageTag() {
        return runOnImageTag;
    }

    public void setRunOnImageTag(final String runOnImageTag) {
        this.runOnImageTag = runOnImageTag;
    }

    public String getBdioFilename() {
        return bdioFilename;
    }

    public void setBdioFilename(final String bdioFilename) {
        this.bdioFilename = bdioFilename;
    }
}
