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
package com.blackducksoftware.integration.hub.docker.imageinspector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.bdio.BdioWriter;
import com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.DockerTarParser;
import com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.ImageInfo;
import com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.manifest.ManifestLayerMapping;
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.extractor.ExtractionDetails;
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.extractor.Extractor;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.google.gson.Gson;

@Component
public class ImageInspector {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private List<Extractor> extractors;

    @Autowired
    private DockerTarParser tarParser;

    private String outputDirPath = null;
    private String codeLocationPrefix = null;

    public void init(final String workingDirPath, final String outputDirPath, final String codeLocationPrefix) {
        logger.debug(String.format("working dir: %s", workingDirPath));
        tarParser.setWorkingDirectory(new File(workingDirPath));
        this.outputDirPath = outputDirPath;
        this.codeLocationPrefix = codeLocationPrefix;
    }

    public List<File> extractLayerTars(final File dockerTar) throws IOException {
        return tarParser.extractLayerTars(dockerTar);
    }

    public File extractDockerLayers(final String imageName, final String imageTag, final List<File> layerTars, final List<ManifestLayerMapping> layerMappings) throws IOException {
        return tarParser.extractDockerLayers(imageName, imageTag, layerTars, layerMappings);
    }

    public OperatingSystemEnum detectOperatingSystem(final String operatingSystem) {
        return tarParser.detectOperatingSystem(operatingSystem);
    }

    public OperatingSystemEnum detectOperatingSystem(final File targetImageFileSystemRootDir) throws HubIntegrationException, IOException {
        return tarParser.detectOperatingSystem(targetImageFileSystemRootDir);
    }

    public List<ManifestLayerMapping> getLayerMappings(final String tarFileName, final String dockerImageName, final String dockerTagName) throws Exception {
        return tarParser.getLayerMappings(tarFileName, dockerImageName, dockerTagName);
    }

    public File generateBdioFromImageFilesDir(final String dockerImageRepo, final String dockerImageTag, final List<ManifestLayerMapping> mappings, final String projectName, final String versionName, final File dockerTar,
            final File targetImageFileSystemRootDir, final OperatingSystemEnum osEnum) throws IOException, HubIntegrationException, InterruptedException {
        logger.debug(String.format("generateBdioFromImageFilesDir(): projectName: %s, versionName: %s", projectName, versionName));
        final ImageInfo imagePkgMgrInfo = tarParser.collectPkgMgrInfo(targetImageFileSystemRootDir, osEnum);
        if (imagePkgMgrInfo.getOperatingSystemEnum() == null) {
            throw new HubIntegrationException("Could not determine the Operating System of this Docker tar.");
        }

        final String architecture = getExtractorByPackageManager(imagePkgMgrInfo.getPkgMgr().getPackageManager()).deriveArchitecture(targetImageFileSystemRootDir);
        logger.debug(String.format("generateBdioFromImageFilesDir(): architecture: %s", architecture));
        return generateBdioFromPackageMgrDirs(dockerImageRepo, dockerImageTag, mappings, projectName, versionName, dockerTar.getName(), imagePkgMgrInfo, architecture);
    }

    private File generateBdioFromPackageMgrDirs(final String dockerImageRepo, final String dockerImageTag, final List<ManifestLayerMapping> layerMappings, final String givenProjectName, final String givenVersionName,
            final String tarFileName, final ImageInfo imageInfo, final String architecture) throws FileNotFoundException, IOException, HubIntegrationException, InterruptedException {
        logger.trace(String.format("generateBdioFromPackageMgrDirs(): Purging/recreating output dir: %s", outputDirPath));
        final File outputDirectory = new File(outputDirPath);
        try {
            FileUtils.deleteDirectory(outputDirectory);
            outputDirectory.mkdirs();
        } catch (final IOException e) {
            logger.warn(String.format("Error purging output dir: %s", outputDirectory.getAbsolutePath()));
        }
        logger.trace(String.format("outputDirectory: exists: %b; isDirectory: %b; $ files: %d", outputDirectory.exists(), outputDirectory.isDirectory(), outputDirectory.listFiles().length));
        final String imageDirectoryName = Names.getTargetImageFileSystemRootDirName(dockerImageRepo, dockerImageTag);
        ManifestLayerMapping manifestMapping = null;
        for (final ManifestLayerMapping mapping : layerMappings) {
            if (StringUtils.compare(imageDirectoryName, imageInfo.getFileSystemRootDirName()) == 0) {
                manifestMapping = mapping;
            }
        }
        if (manifestMapping == null) {
            throw new HubIntegrationException(String.format("Mapping for %s not found in target image manifest file", imageInfo.getFileSystemRootDirName()));
        }

        String pkgMgrFilePath = imageInfo.getPkgMgr().getExtractedPackageManagerDirectory().getAbsolutePath();
        pkgMgrFilePath = pkgMgrFilePath.substring(pkgMgrFilePath.indexOf(imageDirectoryName) + imageDirectoryName.length() + 1);

        final String codeLocationName = Names.getCodeLocationName(codeLocationPrefix, manifestMapping.getImageName(), manifestMapping.getTagName(), pkgMgrFilePath, imageInfo.getPkgMgr().getPackageManager().toString());
        final String finalProjectName = deriveHubProject(manifestMapping.getImageName(), givenProjectName);
        final String finalProjectVersionName = deriveHubProjectVersion(manifestMapping, givenVersionName);
        logger.info(String.format("Hub project: %s, version: %s; Code location : %s", finalProjectName, finalProjectVersionName, codeLocationName));
        final String bdioFilename = Names.getBdioFilename(manifestMapping.getImageName(), pkgMgrFilePath, finalProjectName, finalProjectVersionName);
        final File bdioOutputFile = new File(outputDirectory, bdioFilename);
        try (FileOutputStream bdioOutputStream = new FileOutputStream(bdioOutputFile)) {
            try (BdioWriter bdioWriter = new BdioWriter(new Gson(), bdioOutputStream)) {
                final Extractor extractor = getExtractorByPackageManager(imageInfo.getPkgMgr().getPackageManager());
                final ExtractionDetails extractionDetails = new ExtractionDetails(imageInfo.getOperatingSystemEnum(), architecture);
                extractor.extract(dockerImageRepo, dockerImageTag, imageInfo.getPkgMgr(), bdioWriter, extractionDetails, codeLocationName, finalProjectName, finalProjectVersionName);
            }
        }

        return bdioOutputFile;
    }

    private String deriveHubProject(final String imageName, final String projectName) {
        String hubProjectName;
        if (StringUtils.isBlank(projectName)) {
            hubProjectName = Names.cleanImageName(imageName);
        } else {
            logger.debug("Using project from config property");
            hubProjectName = projectName;
        }
        return hubProjectName;
    }

    private String deriveHubProjectVersion(final ManifestLayerMapping mapping, final String versionName) {
        String hubVersionName;
        if (StringUtils.isBlank(versionName)) {
            hubVersionName = mapping.getTagName();
        } else {
            logger.debug("Using project version from config property");
            hubVersionName = versionName;
        }
        return hubVersionName;
    }

    private Extractor getExtractorByPackageManager(final PackageManagerEnum packageManagerEnum) throws HubIntegrationException {
        for (final Extractor currentExtractor : extractors) {
            if (currentExtractor.getPackageManagerEnum() == packageManagerEnum) {
                return currentExtractor;
            }
        }
        throw new HubIntegrationException(String.format("Extractor not found for packageManager %s", packageManagerEnum.toString()));
    }
}
