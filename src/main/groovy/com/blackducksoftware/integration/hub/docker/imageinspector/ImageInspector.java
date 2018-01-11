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

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.bdio.BdioWriter;
import com.blackducksoftware.integration.hub.docker.hubclient.HubClient;
import com.blackducksoftware.integration.hub.docker.imageinspector.config.Config;
import com.blackducksoftware.integration.hub.docker.imageinspector.config.ProgramPaths;
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
    private HubClient hubClient;

    @Autowired
    private ProgramPaths programPaths;

    @Autowired
    private Config config;

    @Autowired
    private List<Extractor> extractors;

    @Autowired
    private DockerTarParser tarParser;

    public void setConfig(final Config config) {
        this.config = config;
    }

    public void init() {
        tarParser.setWorkingDirectory(new File(programPaths.getHubDockerWorkingDirPath()));
    }

    public List<File> extractLayerTars(final File dockerTar) throws IOException {
        return tarParser.extractLayerTars(dockerTar);
    }

    public File extractDockerLayers(final List<File> layerTars, final List<ManifestLayerMapping> layerMappings) throws IOException {
        return tarParser.extractDockerLayers(layerTars, layerMappings);
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

    public void verifyHubConnection() throws HubIntegrationException {
        hubClient.testHubConnection();
        return;
    }

    public void initImageName() throws HubIntegrationException {
        logger.debug(String.format("initImageName(): dockerImage: %s, dockerTar: %s", config.getDockerImage(), config.getDockerTar()));
        if (StringUtils.isNotBlank(config.getDockerImage())) {
            final String[] imageNameAndTag = config.getDockerImage().split(":");
            if ((imageNameAndTag.length > 0) && (StringUtils.isNotBlank(imageNameAndTag[0]))) {
                config.setDockerImageRepo(imageNameAndTag[0]);
            }
            if ((imageNameAndTag.length > 1) && (StringUtils.isNotBlank(imageNameAndTag[1]))) {
                config.setDockerImageTag(imageNameAndTag[1]);
            } else {
                config.setDockerImageTag("latest");
            }
        }
        logger.debug(String.format("initImageName(): final: dockerImage: %s; dockerImageRepo: %s; dockerImageTag: %s", config.getDockerImage(), config.getDockerImageRepo(), config.getDockerImageTag()));
    }

    public void adjustImageNameTagFromLayerMappings(final List<ManifestLayerMapping> layerMappings) {
        if ((layerMappings != null) && (layerMappings.size() == 1)) {
            if (StringUtils.isBlank(config.getDockerImageRepo())) {
                config.setDockerImageRepo(layerMappings.get(0).getImageName());
            }
            if (StringUtils.isBlank(config.getDockerImageTag())) {
                config.setDockerImageTag(layerMappings.get(0).getTagName());
            }
        }
        logger.debug(String.format("adjustImageNameTagFromLayerMappings(): final: dockerImage: %s; dockerImageRepo: %s; dockerImageTag: %s", config.getDockerImage(), config.getDockerImageRepo(), config.getDockerImageTag()));
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

    public void uploadBdioFiles(final List<File> bdioFiles) throws IntegrationException {
        if (hubClient.isValid()) {
            if (bdioFiles != null) {
                for (final File file : bdioFiles) {
                    hubClient.uploadBdioToHub(file);
                }
            }
            logger.info(" ");
            logger.info("Successfully uploaded all of the bdio files!");
            logger.info(" ");
        }
    }

    private File generateBdioFromPackageMgrDirs(final String dockerImageRepo, final String dockerImageTag, final List<ManifestLayerMapping> layerMappings, final String givenProjectName, final String givenVersionName,
            final String tarFileName, final ImageInfo imageInfo, final String architecture) throws FileNotFoundException, IOException, HubIntegrationException, InterruptedException {
        logger.trace("generateBdioFromPackageMgrDirs(): Purging/recreating output dir");
        final File outputDirectory = new File(programPaths.getHubDockerOutputPathContainer());
        try {
            FileUtils.deleteDirectory(outputDirectory);
            outputDirectory.mkdirs();
        } catch (final IOException e) {
            logger.warn(String.format("Error purging output dir: %s", outputDirectory.getAbsolutePath()));
        }
        logger.trace(String.format("outputDirectory: exists: %b; isDirectory: %b; $ files: %d", outputDirectory.exists(), outputDirectory.isDirectory(), outputDirectory.listFiles().length));

        ManifestLayerMapping manifestMapping = null;
        for (final ManifestLayerMapping mapping : layerMappings) {
            if (StringUtils.compare(mapping.getTargetImageFileSystemRootDirName(), imageInfo.getFileSystemRootDirName()) == 0) {
                manifestMapping = mapping;
            }
        }
        if (manifestMapping == null) {
            throw new HubIntegrationException(String.format("Mapping for %s not found in target image manifest file", imageInfo.getFileSystemRootDirName()));
        }

        final String imageDirectoryName = manifestMapping.getTargetImageFileSystemRootDirName();
        String pkgMgrFilePath = imageInfo.getPkgMgr().getExtractedPackageManagerDirectory().getAbsolutePath();
        pkgMgrFilePath = pkgMgrFilePath.substring(pkgMgrFilePath.indexOf(imageDirectoryName) + imageDirectoryName.length() + 1);

        final String codeLocationName = programPaths.getCodeLocationName(manifestMapping.getImageName(), manifestMapping.getTagName(), pkgMgrFilePath, imageInfo.getPkgMgr().getPackageManager().toString());
        final String finalProjectName = deriveHubProject(manifestMapping.getImageName(), givenProjectName);
        final String finalProjectVersionName = deriveHubProjectVersion(manifestMapping, givenVersionName);
        logger.info(String.format("Hub project: %s, version: %s; Code location : %s", finalProjectName, finalProjectVersionName, codeLocationName));
        final String bdioFilename = programPaths.getBdioFilename(manifestMapping.getImageName(), pkgMgrFilePath, finalProjectName, finalProjectVersionName);
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
            hubProjectName = programPaths.cleanImageName(imageName);
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
