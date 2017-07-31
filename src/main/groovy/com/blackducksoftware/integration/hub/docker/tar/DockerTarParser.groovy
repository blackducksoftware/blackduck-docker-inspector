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
package com.blackducksoftware.integration.hub.docker.tar


import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum
import com.blackducksoftware.integration.hub.docker.tar.manifest.ImageInfo
import com.blackducksoftware.integration.hub.docker.tar.manifest.Manifest
import com.blackducksoftware.integration.hub.exception.HubIntegrationException

@Component
class DockerTarParser {
    private final Logger logger = LoggerFactory.getLogger(DockerTarParser.class)

    public static final String TAR_EXTRACTION_DIRECTORY = 'tarExtraction'

    // TODO make this private; add getter/setter (it gets set in HubDockerManager, plus tests)
    File workingDirectory

    File extractDockerLayers(List<File> layerTars, List<LayerMapping> layerMappings){
        File tarExtractionDirectory = getTarExtractionDirectory()
        File imageFilesDir = new File(tarExtractionDirectory, 'imageFiles')

        layerMappings.each { mapping ->
            mapping.layers.each { layer ->
                logger.trace("layer: ${layer}")
                // TODO: move this to LayerTar class?
                File layerTar = layerTars.find{
                    StringUtils.compare(layer, it.getParentFile().getName()) == 0
                }
                if(layerTar != null){
                    def imageOutputDir = new File(imageFilesDir, mapping.getImageDirectory())
                    logger.trace("Processing layer: ${layerTar.getAbsolutePath()}")
                    DockerLayerTar dockerLayerTar = new DockerLayerTar(layerTar)
                    dockerLayerTar.extractToDir(imageOutputDir)
                } else {
                    logger.warn("Could not find the tar for layer ${layer}")
                }
            }
        }
        imageFilesDir
    }

    private File getTarExtractionDirectory() {
        return new File(workingDirectory, TAR_EXTRACTION_DIRECTORY)
    }

    OperatingSystemEnum detectOperatingSystem(String operatingSystem, File extractedFilesDir) {
        OperatingSystemEnum osEnum
        if(StringUtils.isNotBlank(operatingSystem)){
            osEnum = OperatingSystemEnum.determineOperatingSystem(operatingSystem)
        } else{
            logger.trace("Image directory ${extractedFilesDir.getName()}, looking for etc")
            List<File> etcFiles = findFileWithName(extractedFilesDir, 'etc')
            if (etcFiles == null) {
                String msg = "Unable to find the files that specify the Linux distro of this image."
                throw new HubIntegrationException(msg)
            }
            for(File etcFile : etcFiles){
                try{
                    osEnum = detectOperatingSystemFromEtcDir(etcFile)
                    if(osEnum != null){
                        break
                    }
                } catch (HubIntegrationException e){
                    logger.debug("Error detecing OS from etc dir: ${e.toString()}")
                }
            }
        }

        Set<PackageManagerEnum> packageManagers = new HashSet<>()
        extractedFilesDir.listFiles().each { layerDirectory ->
            logger.trace("Looking in layerDirectory ${layerDirectory.getAbsolutePath()} for lib dir")
            List<File> libDirs = findFileWithName(layerDirectory, 'lib')
            if(libDirs != null){
                libDirs.each{ libDir ->
                    libDir.listFiles().each { packageManagerDirectory ->
                        try{
                            packageManagers.add(PackageManagerEnum.getPackageManagerEnumByName(packageManagerDirectory.getName()))
                        } catch (IllegalArgumentException e){
                            logger.trace(e.toString())
                        }
                    }
                }
            }
        }
        if(packageManagers.size() == 1){
            PackageManagerEnum packageManager = packageManagers.iterator().next()
            osEnum = packageManager.operatingSystem
            logger.debug("Package manager ${packageManager.name()} returns Operating System ${osEnum.name()}")
        }
        if (osEnum == null) {
            String msg = "Unable to identify the Linux distro of this image. You'll need to run with the --linux.distro option"
            throw new HubIntegrationException(msg)
        }
        osEnum
    }

    OperatingSystemEnum detectOperatingSystemFromEtcDir(File etcFile) {
        if(etcFile == null) {
            throw new HubIntegrationException(
            sprintf("Could not determine the Operating System because none of the expected etc files were found."))
        }
        if(etcFile == null || etcFile.listFiles() == null || etcFile.listFiles().size() == 0){
            throw new HubIntegrationException(
            sprintf("Could not determine the Operating System because we could not find the OS files in %s.", etcFile.getAbsolutePath()))
        }
        logger.debug("etc directory ${etcFile.getAbsolutePath()}")
        OperatingSystemEnum osEnum = extractOperatingSystemFromFiles(etcFile.listFiles())
        osEnum
    }

    TarExtractionResults extractPackageManagerDirs(File extractedImageFilesDir, OperatingSystemEnum osEnum) {
        TarExtractionResults results = new TarExtractionResults()
        results.operatingSystemEnum = osEnum
        extractedImageFilesDir.listFiles().each { imageDirectory ->
            logger.info("Extracting data from Image directory ${imageDirectory.getName()}")
            PackageManagerEnum.values().each { packageManagerEnum ->
                File packageManagerDirectory = new File(imageDirectory, packageManagerEnum.directory)
                if (packageManagerDirectory.exists()){
                    logger.trace("Package Manager Dir: ${packageManagerDirectory.getAbsolutePath()}")
                    TarExtractionResult result = new TarExtractionResult()
                    result.imageDirectoryName = imageDirectory.getName()
                    result.packageManager = packageManagerEnum
                    result.extractedPackageManagerDirectory = packageManagerDirectory
                    results.extractionResults.add(result)
                } else {
                    logger.info("Package manager dir ${packageManagerDirectory.getAbsolutePath()} does not exist")
                }
            }
        }
        results
    }

    private OperatingSystemEnum extractOperatingSystemFromFiles(File[] osFiles){
        OperatingSystemEnum osEnum = null
        for(File osFile : osFiles){
            String linePrefix = null
            if(StringUtils.compare(osFile.getName(),'lsb-release') == 0){
                linePrefix = 'DISTRIB_ID='
            } else if(StringUtils.compare(osFile.getName(),'os-release') == 0){
                linePrefix = 'ID='
            }
            if(linePrefix != null){
                osFile.eachLine { line ->
                    line = line.trim()
                    if(line.startsWith(linePrefix)){
                        def (description, value) = line.split('=')
                        value = value.replaceAll('"', '')
                        osEnum = OperatingSystemEnum.determineOperatingSystem(value)
                    }
                }
            }
            if(osEnum != null){
                break
            }
        }
        osEnum
    }

    List<File> findFileWithName(File fileToSearch, String name){
        logger.trace(sprintf("Looking in %s for %s", fileToSearch.getAbsolutePath(), name))
        if(StringUtils.compare(fileToSearch.getName(), name) == 0){
            logger.trace("File Name ${name} found ${fileToSearch.getAbsolutePath()}")
            List<File> files = new ArrayList<>()
            files.add(fileToSearch)
            return files
        } else if (fileToSearch.isDirectory()){
            List<File> files = new ArrayList<>()
            for(File subFile : fileToSearch.listFiles()){
                def foundFile = findFileWithName(subFile, name)
                if(foundFile != null){
                    files.addAll(foundFile)
                }
            }
            return files
        }
    }

    List<File> extractLayerTars(File dockerTar){
        File tarExtractionDirectory = getTarExtractionDirectory()
        List<File> untaredFiles = new ArrayList<>()
        final File outputDir = new File(tarExtractionDirectory, dockerTar.getName())
        def tarArchiveInputStream = new TarArchiveInputStream(new FileInputStream(dockerTar))
        try {
            def tarArchiveEntry
            while (null != (tarArchiveEntry = tarArchiveInputStream.getNextTarEntry())) {
                final File outputFile = new File(outputDir, tarArchiveEntry.getName())
                if (tarArchiveEntry.isFile()) {
                    if(!outputFile.getParentFile().exists()){
                        outputFile.getParentFile().mkdirs()
                    }
                    final OutputStream outputFileStream = new FileOutputStream(outputFile)
                    try{
                        IOUtils.copy(tarArchiveInputStream, outputFileStream)
                        if(tarArchiveEntry.name.contains('layer.tar')){
                            untaredFiles.add(outputFile)
                        }
                    } finally{
                        outputFileStream.close()
                    }
                }
            }
        } finally {
            IOUtils.closeQuietly(tarArchiveInputStream)
        }
        untaredFiles
    }

    public List<LayerMapping> getLayerMappings(String tarFileName, String dockerImageName, String dockerTagName){
        logger.debug("getLayerMappings()")
        List<LayerMapping> mappings = new ArrayList<>()
        Manifest manifest = new Manifest(this.getTarExtractionDirectory())
        try {
            List<ImageInfo> images = manifest.getManifestContents(tarFileName)
            for(ImageInfo image : images) {
                logger.debug("getLayerMappings(): image: ${image}")
                LayerMapping mapping = new LayerMapping()
                String specifiedRepoTag = ''
                if (StringUtils.isNotBlank(dockerImageName)) {
                    specifiedRepoTag = "${dockerImageName}:${dockerTagName}"
                }
                def (imageName, tagName) = ['', '']
                String foundRepoTag = image.repoTags.find { repoTag ->
                    StringUtils.compare(repoTag, specifiedRepoTag) == 0
                }
                if(StringUtils.isBlank(foundRepoTag)){
                    logger.debug("Attempting to parse repoTag from manifest")
                    if (image.repoTags == null) {
                        String msg = "The RepoTags field is missing from the tar file manifest. Please make sure this tar file was saved using the image name (vs. image ID)"
                        throw new HubIntegrationException(msg)
                    }
                    def repoTag = image.repoTags.get(0)
                    logger.debug("repoTag: ${repoTag}")
                    imageName = repoTag.substring(0, repoTag.lastIndexOf(':'))
                    tagName = repoTag.substring(repoTag.lastIndexOf(':') + 1)
                    logger.debug("Parsed imageName: ${imageName}; tagName: ${tagName}")
                } else {
                    logger.debug("foundRepoTag: ${foundRepoTag}")
                    imageName = foundRepoTag.substring(0, foundRepoTag.lastIndexOf(':'))
                    tagName = foundRepoTag.substring(foundRepoTag.lastIndexOf(':') + 1)
                    logger.debug("Found imageName: ${imageName}; tagName: ${tagName}")
                }
                logger.info("Image: ${imageName}, Tag: ${tagName}")
                mapping.imageName =  imageName.replaceAll(':', '_').replaceAll('/', '_')
                mapping.tagName = tagName
                for(String layer : image.layers){
                    mapping.layers.add(layer.substring(0, layer.indexOf('/')))
                }
                if (StringUtils.isNotBlank(dockerImageName)) {
                    if(StringUtils.compare(imageName, dockerImageName) == 0 && StringUtils.compare(tagName, dockerTagName) == 0){
                        logger.debug('Adding layer mapping')
                        logger.debug("Image: ${mapping.imageName}:${mapping.tagName}")
                        logger.debug("Layers: ${mapping.layers}")
                        mappings.add(mapping)
                    }
                } else {
                    logger.debug('Adding layer mapping')
                    logger.debug("Image ${mapping.imageName} , Tag ${mapping.tagName}")
                    logger.debug("Layers ${mapping.layers}")
                    mappings.add(mapping)
                }
            }
        } catch (Exception e) {
            logger.error("Could not parse the image manifest file : ${e.toString()}")
            throw e
        }
        // TODO TEMP; useful for debugging, but can probably remove once we're
        // confident in layer targeting
        logger.debug("getLayerMappings(): # mappings found: ${mappings.size()}")
        for (LayerMapping m : mappings) {
            logger.debug("getLayerMappings():\t${m.imageName}/${m.tagName}: ")
            for (String layerId : m.layers) {
                logger.debug("getLayerMappings():\t\t${layerId}")
            }
        }
        //////////////////
        mappings
    }




}