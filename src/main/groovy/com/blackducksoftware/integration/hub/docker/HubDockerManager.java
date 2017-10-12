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
package com.blackducksoftware.integration.hub.docker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.bdio.BdioWriter;
import com.blackducksoftware.integration.hub.docker.client.DockerClientManager;
import com.blackducksoftware.integration.hub.docker.extractor.ExtractionDetails;
import com.blackducksoftware.integration.hub.docker.extractor.Extractor;
import com.blackducksoftware.integration.hub.docker.hub.HubClient;
import com.blackducksoftware.integration.hub.docker.tar.DockerTarParser;
import com.blackducksoftware.integration.hub.docker.tar.ImageInfo;
import com.blackducksoftware.integration.hub.docker.tar.manifest.ManifestLayerMapping;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.google.gson.Gson;

@Component
public class HubDockerManager {
    private final Logger logger = LoggerFactory.getLogger(HubDockerManager.class);

    @Value("${linux.distro}")
    private String linuxDistro;

    @Autowired
    private HubClient hubClient;

    @Autowired
    private ProgramPaths programPaths;

    @Autowired
    private DockerClientManager dockerClientManager;

    @Autowired
    private List<Extractor> extractors;

    @Autowired
    private DockerTarParser tarParser;

    public void init() {
        tarParser.setWorkingDirectory(new File(programPaths.getHubDockerWorkingDirPath()));
    }

    public File getTarFileFromDockerImage(final String imageName, final String tagName) throws IOException, HubIntegrationException {
        return dockerClientManager.getTarFileFromDockerImage(imageName, tagName);
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

    public List<File> generateBdioFromImageFilesDir(final String dockerImageRepo, final String dockerImageTag, final List<ManifestLayerMapping> mappings, final String projectName, final String versionName, final File dockerTar,
            final File targetImageFileSystemRootDir, final OperatingSystemEnum osEnum) throws IOException, HubIntegrationException, InterruptedException {
        final ImageInfo imagePkgMgrInfo = tarParser.collectPkgMgrInfo(targetImageFileSystemRootDir, osEnum);
        if (imagePkgMgrInfo.getOperatingSystemEnum() == null) {
            throw new HubIntegrationException("Could not determine the Operating System of this Docker tar.");
        }

        final String architecture = getExtractorByPackageManager(imagePkgMgrInfo.getPkgMgr().getPackageManager()).deriveArchitecture(targetImageFileSystemRootDir);
        logger.debug(String.format("generateBdioFromImageFilesDir(): architecture: %s", architecture));
        return generateBdioFromPackageMgrDirs(dockerImageRepo, dockerImageTag, mappings, projectName, versionName, dockerTar.getName(), imagePkgMgrInfo, architecture);
    }

    public void phoneHome() {
        hubClient.phoneHome(dockerClientManager.getDockerEngineVersion());
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

    public void cleanWorkingDirectory() throws IOException {
        final File workingDirectory = new File(programPaths.getHubDockerWorkingDirPath());
        if (workingDirectory.exists()) {
            FileUtils.deleteDirectory(workingDirectory);
        }
    }

    private List<File> generateBdioFromPackageMgrDirs(final String dockerImageRepo, final String dockerImageTag, final List<ManifestLayerMapping> layerMappings, final String projectName, final String versionName, final String tarFileName,
            final ImageInfo imageInfo, final String architecture) throws FileNotFoundException, IOException, HubIntegrationException, InterruptedException {
        logger.trace("generateBdioFromPackageMgrDirs(): Purging/recreating output dir");
        final File outputDirectory = new File(programPaths.getHubDockerOutputPathContainer());
        try {
            FileUtils.deleteDirectory(outputDirectory);
            outputDirectory.mkdirs();
        } catch (final IOException e) {
            logger.warn(String.format("Error purging output dir: %s", outputDirectory.getAbsolutePath()));
        }
        logger.trace(String.format("outputDirectory: exists: %b; isDirectory: %b; $ files: %d", outputDirectory.exists(), outputDirectory.isDirectory(), outputDirectory.listFiles().length));

        final List<File> bdioFiles = new ArrayList<>();
        ManifestLayerMapping manifestMapping = null;
        for (final ManifestLayerMapping mapping : layerMappings) {
            if (StringUtils.compare(mapping.getTargetImageFileSystemRootDirName(), imageInfo.getFileSystemRootDirName()) == 0) {
                manifestMapping = mapping;
            }
        }
        if (manifestMapping == null) {
            throw new HubIntegrationException(String.format("Mapping for %s not found in target image manifest file", imageInfo.getFileSystemRootDirName()));
        }

        String codeLocationName, hubProjectName, hubVersionName = "";
        final String imageDirectoryName = manifestMapping.getTargetImageFileSystemRootDirName();
        String pkgMgrFilePath = imageInfo.getPkgMgr().getExtractedPackageManagerDirectory().getAbsolutePath();
        pkgMgrFilePath = pkgMgrFilePath.substring(pkgMgrFilePath.indexOf(imageDirectoryName) + imageDirectoryName.length() + 1);

        codeLocationName = programPaths.getCodeLocationName(manifestMapping.getImageName(), manifestMapping.getTagName(), pkgMgrFilePath, imageInfo.getPkgMgr().getPackageManager().toString());
        hubProjectName = deriveHubProject(manifestMapping.getImageName(), projectName);
        hubVersionName = deriveHubProjectVersion(manifestMapping, versionName);
        logger.info(String.format("Hub project, version: %s, %s; Code location : %s", hubProjectName, hubVersionName, codeLocationName));
        final String bdioFilename = programPaths.getBdioFilename(manifestMapping.getImageName(), pkgMgrFilePath, hubProjectName, hubVersionName);
        final File bdioOutputFile = new File(outputDirectory, bdioFilename);
        bdioFiles.add(bdioOutputFile);
        try (FileOutputStream bdioOutputStream = new FileOutputStream(bdioOutputFile)) {
            try (BdioWriter bdioWriter = new BdioWriter(new Gson(), bdioOutputStream)) {
                final Extractor extractor = getExtractorByPackageManager(imageInfo.getPkgMgr().getPackageManager());
                final ExtractionDetails extractionDetails = new ExtractionDetails(imageInfo.getOperatingSystemEnum(), architecture);
                extractor.extract(dockerImageRepo, dockerImageTag, imageInfo.getPkgMgr(), bdioWriter, extractionDetails, codeLocationName, hubProjectName, hubVersionName);
            }
        }

        return bdioFiles;
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
