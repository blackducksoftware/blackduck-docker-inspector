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
package com.blackducksoftware.integration.hub.docker.tar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum;
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum;
import com.blackducksoftware.integration.hub.docker.linux.Dirs;
import com.blackducksoftware.integration.hub.docker.linux.EtcDir;
import com.blackducksoftware.integration.hub.docker.linux.FileSys;
import com.blackducksoftware.integration.hub.docker.tar.manifest.Manifest;
import com.blackducksoftware.integration.hub.docker.tar.manifest.ManifestFactory;
import com.blackducksoftware.integration.hub.docker.tar.manifest.ManifestLayerMapping;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;

@Component
public class DockerTarParser {
    private static final String TAR_EXTRACTION_DIRECTORY = "tarExtraction";
    private static final String TARGET_IMAGE_FILESYSTEM_PARENT_DIR = "imageFiles";
    private static final String DOCKER_LAYER_TAR_FILENAME = "layer.tar";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private File workingDirectory;
    private File tarExtractionDirectory;

    @Autowired
    private ManifestFactory manifestFactory;

    public void setWorkingDirectory(final File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public File extractDockerLayers(final List<File> layerTars, final List<ManifestLayerMapping> manifestLayerMappings) throws IOException {
        final File tarExtractionDirectory = getTarExtractionDirectory();
        final File targetImageFileSystemParentDir = new File(tarExtractionDirectory, TARGET_IMAGE_FILESYSTEM_PARENT_DIR);
        File targetImageFileSystemRootDir = null;
        for (final ManifestLayerMapping manifestLayerMapping : manifestLayerMappings) {
            for (final String layer : manifestLayerMapping.getLayers()) {
                logger.trace(String.format("Looking for tar for layer: %s", layer));
                final File layerTar = getLayerTar(layerTars, layer);
                if (layerTar != null) {
                    targetImageFileSystemRootDir = extractLayerTarToDir(targetImageFileSystemParentDir, layerTar, manifestLayerMapping);
                } else {
                    logger.error(String.format("Could not find the tar for layer %s", layer));
                }
            }
        }
        return targetImageFileSystemRootDir;
    }

    public OperatingSystemEnum detectOperatingSystem(final String operatingSystem, final File targetImageFileSystemRootDir) throws HubIntegrationException, IOException {
        OperatingSystemEnum osEnum = deriveOsFromPkgMgr(targetImageFileSystemRootDir);
        if (osEnum != null) {
            return osEnum;
        }
        if (StringUtils.isNotBlank(operatingSystem)) {
            osEnum = OperatingSystemEnum.determineOperatingSystem(operatingSystem);
            return osEnum;
        }
        osEnum = deriveOsFromEtcDir(targetImageFileSystemRootDir);
        if (osEnum == null) {
            final String msg = "Unable to identify the Linux distro of this image. You'll need to run with the --linux.distro option";
            throw new HubIntegrationException(msg);
        }
        return osEnum;
    }

    public ImageInfo collectPkgMgrInfo(final File targetImageFileSystemRootDir, final OperatingSystemEnum osEnum) {
        logger.debug(String.format("Checking image file system at %s for package managers", targetImageFileSystemRootDir.getName()));
        for (final PackageManagerEnum packageManagerEnum : PackageManagerEnum.values()) {
            final File packageManagerDirectory = new File(targetImageFileSystemRootDir, packageManagerEnum.getDirectory());
            if (packageManagerDirectory.exists()) {
                logger.info(String.format("Found package Manager Dir: %s", packageManagerDirectory.getAbsolutePath()));
                final ImagePkgMgr targetImagePkgMgr = new ImagePkgMgr(packageManagerDirectory, packageManagerEnum);
                final ImageInfo imagePkgMgrInfo = new ImageInfo(targetImageFileSystemRootDir.getName(), osEnum, targetImagePkgMgr);
                return imagePkgMgrInfo;
            } else {
                logger.debug(String.format("Package manager dir %s does not exist", packageManagerDirectory.getAbsolutePath()));
            }
        }

        logger.error("No package manager found");
        return new ImageInfo(targetImageFileSystemRootDir.getName(), osEnum, null);
    }

    public List<File> extractLayerTars(final File dockerTar) throws IOException {
        final File tarExtractionDirectory = getTarExtractionDirectory();
        final List<File> untaredFiles = new ArrayList<>();
        final File outputDir = new File(tarExtractionDirectory, dockerTar.getName());
        final TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new FileInputStream(dockerTar));
        try {
            TarArchiveEntry tarArchiveEntry = null;
            while (null != (tarArchiveEntry = tarArchiveInputStream.getNextTarEntry())) {
                final File outputFile = new File(outputDir, tarArchiveEntry.getName());
                if (tarArchiveEntry.isFile()) {
                    if (!outputFile.getParentFile().exists()) {
                        outputFile.getParentFile().mkdirs();
                    }
                    final OutputStream outputFileStream = new FileOutputStream(outputFile);
                    try {
                        IOUtils.copy(tarArchiveInputStream, outputFileStream);
                        if (tarArchiveEntry.getName().contains(DOCKER_LAYER_TAR_FILENAME)) {
                            untaredFiles.add(outputFile);
                        }
                    } finally {
                        outputFileStream.close();
                    }
                }
            }
        } finally {
            IOUtils.closeQuietly(tarArchiveInputStream);
        }
        return untaredFiles;
    }

    public List<ManifestLayerMapping> getLayerMappings(final String tarFileName, final String dockerImageName, final String dockerTagName) throws Exception {
        logger.debug(String.format("getLayerMappings(): dockerImageName: %s; dockerTagName: %s", dockerImageName, dockerTagName));
        final Manifest manifest = manifestFactory.createManifest(getTarExtractionDirectory(), tarFileName);
        List<ManifestLayerMapping> mappings;
        try {
            mappings = manifest.getLayerMappings(dockerImageName, dockerTagName);
        } catch (final Exception e) {
            logger.error(String.format("Could not parse the image manifest file : %s", e.getMessage()));
            throw e;
        }
        if (mappings.size() == 0) {
            final String msg = String.format("Could not find image %s:%s in tar file %s", dockerImageName, dockerTagName, tarFileName);
            throw new HubIntegrationException(msg);
        }
        return mappings;
    }

    private File getTarExtractionDirectory() {
        if (tarExtractionDirectory == null) {
            tarExtractionDirectory = new File(workingDirectory, TAR_EXTRACTION_DIRECTORY);
        }
        return tarExtractionDirectory;
    }

    private File extractLayerTarToDir(final File imageFilesDir, final File layerTar, final ManifestLayerMapping mapping) throws IOException {
        logger.trace(String.format("Extracting layer: %s into %s", layerTar.getAbsolutePath(), mapping.getTargetImageFileSystemRootDirName()));
        final File targetImageFileSystemRoot = new File(imageFilesDir, mapping.getTargetImageFileSystemRootDirName());
        final DockerLayerTar dockerLayerTar = new DockerLayerTar(layerTar);
        dockerLayerTar.extractToDir(targetImageFileSystemRoot);
        return targetImageFileSystemRoot;
    }

    private File getLayerTar(final List<File> layerTars, final String layer) {
        File layerTar = null;
        for (final File candidateLayerTar : layerTars) {
            if (layer.equals(candidateLayerTar.getParentFile().getName())) {
                logger.debug(String.format("Found layer tar for layer %s", layer));
                layerTar = candidateLayerTar;
                break;
            }
        }
        return layerTar;
    }

    private OperatingSystemEnum deriveOsFromEtcDir(final File targetImageFileSystemRootDir) throws HubIntegrationException, IOException {
        logger.trace(String.format("Target file system root dir %s, looking for etc", targetImageFileSystemRootDir.getName()));
        OperatingSystemEnum osEnum = null;
        final List<File> etcFiles = Dirs.findFileWithName(targetImageFileSystemRootDir, "etc");
        if (etcFiles == null) {
            final String msg = "Unable to find the files that specify the Linux distro of this image.";
            throw new HubIntegrationException(msg);
        }
        for (final File etcFile : etcFiles) {
            try {
                final EtcDir etcDir = new EtcDir(etcFile);
                osEnum = etcDir.getOperatingSystem();
                if (osEnum != null) {
                    break;
                }
            } catch (final HubIntegrationException e) {
                logger.debug(String.format("Error detecing OS from etc dir: %s", e.toString()));
            }
        }
        return osEnum;
    }

    private OperatingSystemEnum deriveOsFromPkgMgr(final File targetImageFileSystemRootDir) {
        OperatingSystemEnum osEnum = null;

        final FileSys extractedFileSys = new FileSys(targetImageFileSystemRootDir);
        final Set<PackageManagerEnum> packageManagers = extractedFileSys.getPackageManagers();
        if (packageManagers.size() == 1) {
            final PackageManagerEnum packageManager = packageManagers.iterator().next();
            osEnum = packageManager.getOperatingSystem();
            logger.debug(String.format("Package manager %s returns Operating System %s", packageManager.name(), osEnum.name()));
            return osEnum;
        }
        return null;

    }

}
