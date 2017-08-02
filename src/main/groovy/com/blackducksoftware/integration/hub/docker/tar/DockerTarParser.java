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
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum;
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum;
import com.blackducksoftware.integration.hub.docker.linux.Dirs;
import com.blackducksoftware.integration.hub.docker.linux.EtcDir;
import com.blackducksoftware.integration.hub.docker.linux.FileSys;
import com.blackducksoftware.integration.hub.docker.tar.manifest.Manifest;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;

@Component
public class DockerTarParser {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String TAR_EXTRACTION_DIRECTORY = "tarExtraction";
    private File workingDirectory;

    File extractDockerLayers(final List<File> layerTars, final List<LayerMapping> layerMappings) throws IOException {
        final File tarExtractionDirectory = getTarExtractionDirectory();
        final File imageFilesDir = new File(tarExtractionDirectory, "imageFiles");
        for (final LayerMapping mapping : layerMappings) {
            for (final String layer : mapping.getLayers()) {

                logger.trace("Looking for tar for layer: ${layer}");
                // TODO: move this to LayerTar class? extract method?
                File layerTar = null;
                for (final File candidateLayerTar : layerTars) {
                    if (layer.equals(candidateLayerTar.getParentFile().getName())) {
                        logger.info(String.format("Found layer tar for layer %s", layer));
                        layerTar = candidateLayerTar;
                        break;
                    }
                }

                if (layerTar != null) {
                    final File imageOutputDir = new File(imageFilesDir, mapping.getImageDirectory());
                    logger.trace("Processing layer: ${layerTar.getAbsolutePath()}");
                    final DockerLayerTar dockerLayerTar = new DockerLayerTar(layerTar);
                    dockerLayerTar.extractToDir(imageOutputDir);
                } else {
                    logger.error("Could not find the tar for layer ${layer}");
                }
            }
        }
        return imageFilesDir;
    }

    private File getTarExtractionDirectory() {
        return new File(workingDirectory, TAR_EXTRACTION_DIRECTORY);
    }

    public OperatingSystemEnum detectOperatingSystem(final String operatingSystem, final File extractedFilesDir) throws HubIntegrationException, IOException {
        OperatingSystemEnum osEnum = deriveOsFromPkgMgr(extractedFilesDir);
        if (osEnum != null) {
            return osEnum;
        }
        if (StringUtils.isNotBlank(operatingSystem)) {
            osEnum = OperatingSystemEnum.determineOperatingSystem(operatingSystem);
            return osEnum;
        }
        osEnum = deriveOsFromEtcDir(extractedFilesDir);
        if (osEnum == null) {
            final String msg = "Unable to identify the Linux distro of this image. You'll need to run with the --linux.distro option";
            throw new HubIntegrationException(msg);
        }
        return osEnum;
    }

    private OperatingSystemEnum deriveOsFromEtcDir(final File extractedFilesDir) throws HubIntegrationException, IOException {
        logger.trace("Image directory ${extractedFilesDir.getName()}, looking for etc");
        OperatingSystemEnum osEnum = null;
        final List<File> etcFiles = Dirs.findFileWithName(extractedFilesDir, "etc");
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
                logger.debug("Error detecing OS from etc dir: ${e.toString()}");
            }
        }
        return osEnum;
    }

    private OperatingSystemEnum deriveOsFromPkgMgr(final File extractedFilesDir) {
        OperatingSystemEnum osEnum = null;

        final FileSys extractedFileSys = new FileSys(extractedFilesDir);
        final Set<PackageManagerEnum> packageManagers = extractedFileSys.getPackageManagers();
        if (packageManagers.size() == 1) {
            final PackageManagerEnum packageManager = packageManagers.iterator().next();
            osEnum = packageManager.getOperatingSystem();
            logger.debug("Package manager ${packageManager.name()} returns Operating System ${osEnum.name()}");
            return osEnum;
        }
        return null;

    }

    public ImageInfo collectPkgMgrInfo(final File extractedImageFilesDir, final OperatingSystemEnum osEnum) {
        final ImageInfo imagePkgMgrInfo = new ImageInfo();
        imagePkgMgrInfo.setOperatingSystemEnum(osEnum);
        // There will only be one imageDirectory; the .each is a lazy way to get it
        // It has the entire target image file system
        for (final File imageDirectory : extractedImageFilesDir.listFiles()) {
            logger.debug("Checking image file system at ${imageDirectory.getName()} for package managers");
            for (final PackageManagerEnum packageManagerEnum : PackageManagerEnum.values()) {
                final File packageManagerDirectory = new File(imageDirectory, packageManagerEnum.getDirectory());
                if (packageManagerDirectory.exists()) {
                    logger.trace("Package Manager Dir: ${packageManagerDirectory.getAbsolutePath()}");
                    final ImagePkgMgr result = new ImagePkgMgr();
                    result.setImageDirectoryName(imageDirectory.getName());
                    result.setPackageManager(packageManagerEnum);
                    result.setExtractedPackageManagerDirectory(packageManagerDirectory);
                    imagePkgMgrInfo.getPkgMgrs().add(result);
                } else {
                    logger.info("Package manager dir ${packageManagerDirectory.getAbsolutePath()} does not exist");
                }
            }
        }
        return imagePkgMgrInfo;
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
                        if (tarArchiveEntry.getName().contains("layer.tar")) {
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

    public List<LayerMapping> getLayerMappings(final String tarFileName, final String dockerImageName, final String dockerTagName) throws Exception {
        logger.debug("getLayerMappings()");
        final Manifest manifest = new Manifest(dockerImageName, dockerTagName, getTarExtractionDirectory(), tarFileName);
        List<LayerMapping> mappings;
        try {
            mappings = manifest.getLayerMappings();
        } catch (final Exception e) {
            logger.error("Could not parse the image manifest file : ${e.toString()}");
            throw e;
        }
        return mappings;
    }
}
