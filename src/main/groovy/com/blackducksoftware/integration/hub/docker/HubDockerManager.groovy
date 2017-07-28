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
package com.blackducksoftware.integration.hub.docker

import java.nio.file.Files
import java.nio.file.Path

import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.bdio.simple.BdioWriter
import com.blackducksoftware.integration.hub.docker.client.DockerClientManager
import com.blackducksoftware.integration.hub.docker.client.ProgramPaths
import com.blackducksoftware.integration.hub.docker.extractor.ExtractionDetails
import com.blackducksoftware.integration.hub.docker.extractor.Extractor
import com.blackducksoftware.integration.hub.docker.tar.DockerTarParser
import com.blackducksoftware.integration.hub.docker.tar.LayerMapping
import com.blackducksoftware.integration.hub.docker.tar.TarExtractionResults
import com.blackducksoftware.integration.hub.exception.HubIntegrationException
import com.google.gson.Gson


@Component
class HubDockerManager {
    private final Logger logger = LoggerFactory.getLogger(HubDockerManager.class)

    @Value('${linux.distro}')
    String linuxDistro

    @Autowired
    HubClient hubClient

    @Autowired
    ProgramPaths programPaths

    @Autowired
    DockerClientManager dockerClientManager

    @Autowired
    List<Extractor> extractors

    @Autowired
    DockerTarParser tarParser

    @Autowired
    PackageManagerFiles packageManagerFiles

    void init() {
        tarParser.workingDirectory = new File(programPaths.getHubDockerWorkingDirPath())
    }

    File getTarFileFromDockerImage(String imageName, String tagName) {
        dockerClientManager.getTarFileFromDockerImage(imageName, tagName)
    }

    List<File> extractLayerTars(File dockerTar){
        tarParser.extractLayerTars(dockerTar)
    }

    File extractDockerLayers(List<File> layerTars, List<LayerMapping> layerMappings) {
        tarParser.extractDockerLayers(layerTars, layerMappings)
    }

    String extractManifestFileContent(String dockerTarName){
        tarParser.extractManifestFileContent(dockerTarName)
    }

    OperatingSystemEnum detectOperatingSystem(String operatingSystem, File extractedFilesDir) {
        tarParser.detectOperatingSystem(operatingSystem, extractedFilesDir)
    }

    OperatingSystemEnum detectCurrentOperatingSystem() {
        tarParser.detectOperatingSystemFromEtcDir(new File("/etc"))
    }

    List<LayerMapping> getLayerMappings(String tarFileName, String dockerImageName, String dockerTagName) {
        return tarParser.getLayerMappings(tarFileName, dockerImageName, dockerTagName)
    }

    List<File> generateBdioFromImageFilesDir(List<LayerMapping> mappings, String projectName, String versionName, File dockerTar, File imageFilesDir, OperatingSystemEnum osEnum) {
        TarExtractionResults tarExtractionResults = tarParser.extractPackageManagerDirs(imageFilesDir, osEnum)
        if(tarExtractionResults.operatingSystemEnum == null){
            throw new HubIntegrationException('Could not determine the Operating System of this Docker tar.')
        }
        String architecture = null
        if(osEnum == OperatingSystemEnum.ALPINE){
            List<File> etcDirectories = tarParser.findFileWithName(imageFilesDir, "etc")
            for(File etc : etcDirectories){
                File architectureFile = new File(etc, 'apk')
                architectureFile = new File(architectureFile, 'arch')
                if(architectureFile.exists()){
                    architecture = architectureFile.readLines().get(0)
                    break
                }
            }
        }
        generateBdioFromPackageMgrDirs(mappings, projectName, versionName, dockerTar.getName(), tarExtractionResults, architecture)
    }

    void uploadBdioFiles(List<File> bdioFiles){
        if(hubClient.isValid()){
            if(bdioFiles != null){
                bdioFiles.each { file ->
                    hubClient.uploadBdioToHub(file)
                }
            }
            logger.info(' ')
            logger.info('Successfully uploaded all of the bdio files!')
            logger.info(' ')
        }
    }

    void cleanWorkingDirectory(){
        File workingDirectory = new File(programPaths.getHubDockerWorkingDirPath())
        if(workingDirectory.exists()){
            FileUtils.deleteDirectory(workingDirectory)
        }
    }

    void copyFile(File fileToCopy, File destination) {
        String filename = fileToCopy.getName()
        logger.debug("Copying ${fileToCopy.getAbsolutePath()} to ${destination.getAbsolutePath()}")
        Path destPath = destination.toPath().resolve(filename)
        Files.copy(fileToCopy.toPath(), destPath)
    }

    private List<File> generateBdioFromPackageMgrDirs(List<LayerMapping> layerMappings, String projectName, String versionName, String tarFileName, TarExtractionResults tarResults, String architecture) {
        File workingDirectory = new File(programPaths.getHubDockerWorkingDirPath())
        def bdioFiles = []
        tarResults.extractionResults.each { extractionResult ->
            def mapping = layerMappings.find { mapping ->
                StringUtils.compare(mapping.getImageDirectory(), extractionResult.imageDirectoryName) == 0
            }
            String imageDirectoryName = mapping.getImageDirectory()
            String filePath = extractionResult.extractedPackageManagerDirectory.getAbsolutePath()
            filePath = filePath.substring(filePath.indexOf(imageDirectoryName) + 1)
            filePath = filePath.substring(filePath.indexOf('/') + 1)
            filePath = filePath.replaceAll('/', '_')
            String cleanedImageName = mapping.imageName.replaceAll('/', '_')
            packageManagerFiles.stubPackageManagerFiles(extractionResult)
            String codeLocationName, hubProjectName, hubVersionName = ''
            codeLocationName = "${cleanedImageName}_${mapping.tagName}_${filePath}_${extractionResult.packageManager}"
            hubProjectName = deriveHubProject(cleanedImageName, projectName)
            hubVersionName = deriveHubProjectVersion(mapping, versionName)

            logger.info("Hub project, version: ${hubProjectName}, ${hubVersionName}; Code location : ${codeLocationName}")

            String cleanedHubProjectName = hubProjectName.replaceAll('/', '_')
            String bdioFilename = "${cleanedImageName}_${filePath}_${cleanedHubProjectName}_${hubVersionName}_bdio.jsonld"
            logger.debug("bdioFilename: ${bdioFilename}")
            def outputFile = new File(workingDirectory, bdioFilename)
            bdioFiles.add(outputFile)
            new FileOutputStream(outputFile).withStream { outputStream ->
                BdioWriter writer = new BdioWriter(new Gson(), outputStream)
                try{
                    Extractor extractor = getExtractorByPackageManager(extractionResult.packageManager)
                    ExtractionDetails extractionDetails = new ExtractionDetails()
                    extractionDetails.operatingSystem = tarResults.operatingSystemEnum
                    extractionDetails.architecture = architecture
                    extractor.extract(writer, extractionDetails, codeLocationName, hubProjectName, hubVersionName)
                }finally{
                    writer.close()
                }
            }
        }
        bdioFiles
    }

    private String deriveHubProject(String cleanedImageName, String projectName) {
        String hubProjectName
        if (StringUtils.isBlank(projectName)) {
            hubProjectName = cleanedImageName
        } else {
            logger.debug("Using project from config property")
            hubProjectName = projectName
        }
        return hubProjectName
    }

    private String deriveHubProjectVersion(LayerMapping mapping, String versionName) {
        String hubVersionName
        if (StringUtils.isBlank(versionName)) {
            hubVersionName = mapping.tagName
        } else {
            logger.debug("Using project version from config property")
            hubVersionName = versionName
        }
        return hubVersionName
    }
    private Extractor getExtractorByPackageManager(PackageManagerEnum packageManagerEnum){
        extractors.find { currentExtractor ->
            currentExtractor.packageManagerEnum == packageManagerEnum
        }
    }
}