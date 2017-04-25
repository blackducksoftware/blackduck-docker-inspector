/*
 * Copyright (C) 2017 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Black Duck Software.
 */
package com.blackducksoftware.integration.hub.docker.tar

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum
import com.blackducksoftware.integration.hub.exception.HubIntegrationException

class DockerTarParser {
    private final Logger logger = LoggerFactory.getLogger(DockerTarParser.class)

    private static final String OS_EXTRACTION_PATTERN = "etc/(lsb-release|os-release)"

    private static final String EXTRACTION_PATTERN = "(lib/apk.*|var/lib/(dpkg|rpm){1}.*|${OS_EXTRACTION_PATTERN})"

    File workingDirectory

    File extractDockerLayers(File dockerTar){
        File tarExtractionDirectory = new File(workingDirectory, 'tarExtraction')
        List<File> layerTars = extractLayerTars(tarExtractionDirectory,dockerTar)
        File layerFilesDir = new File(tarExtractionDirectory, 'layerFiles')
        layerTars.each { layerTar ->
            def layerName = layerTar.getName()
            if(StringUtils.compare(layerName,'layer.tar') == 0){
                layerName = layerTar.getParentFile().getName()
            }
            def layerOutputDir = new File(layerFilesDir, layerName)
            parseLayerTarAndExtract(EXTRACTION_PATTERN, layerTar, layerOutputDir)
        }
        layerFilesDir
    }

    OperatingSystemEnum detectOperatingSystem(String operatingSystem, File layerFilesDir) {
        OperatingSystemEnum osEnum
        if(StringUtils.isNotBlank(operatingSystem)){
            osEnum = OperatingSystemEnum.determineOperatingSystem(operatingSystem)
        } else{
            logger.trace("Layer directory ${layerFilesDir.getName()}, looking for etc")
            def etcFile = findFileWithName(layerFilesDir, 'etc')
            if (etcFile == null) {
                String msg = "Unable to identify the Linux flavor of this image. You'll need to run with the --linux.distro option"
                throw new HubIntegrationException(msg)
            }
            osEnum = detectOperatingSystemFromEtcDir(etcFile)
        }
        if (osEnum == null) {
            String msg = "Unable to identify the Linux flavor of this image. You'll need to run with the --linux.distro option"
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
        OperatingSystemEnum osEnum = extractOperatingSystemFromFiles(etcFile.listFiles())
        osEnum
    }

    TarExtractionResults extractPackageManagerDirs(File layerFilesDir, OperatingSystemEnum osEnum) {
        TarExtractionResults results = new TarExtractionResults()
        results.operatingSystemEnum = osEnum
        layerFilesDir.listFiles().each { layerDirectory ->
            logger.trace("Layer directory .getName()}, looking for lib")
            def libDir = findFileWithName(layerDirectory, 'lib')
            if(libDir == null){
                throw new HubIntegrationException("Could not find the lib directroy in ${layerDirectory.getAbsolutePath()}")
            } else{
                logger.trace('lib directory : '+libDir.getAbsolutePath())
                libDir.listFiles().each { packageManagerDirectory ->
                    logger.trace(packageManagerDirectory.getAbsolutePath())
                    TarExtractionResult result = new TarExtractionResult()
                    result.layer = layerDirectory.getName()
                    result.packageManager =PackageManagerEnum.getPackageManagerEnumByName(packageManagerDirectory.getName())
                    result.extractedPackageManagerDirectory = packageManagerDirectory
                    results.extractionResults.add(result)
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

    private File findFileWithName(File fileToSearch, String name){
        logger.debug(sprintf("Looking in %s for %s", fileToSearch.getAbsolutePath(), name))
        if(StringUtils.compare(fileToSearch.getName(), name) == 0){
            logger.trace("File Name ${name} found ${fileToSearch.getAbsolutePath()}")
            return fileToSearch
        } else if (fileToSearch.isDirectory()){
            File foundFile = null
            for(File subFile : fileToSearch.listFiles()){
                foundFile = findFileWithName(subFile, name)
                if(foundFile != null){
                    break
                }
            }
            return foundFile
        }
    }

    private List<File> extractLayerTars(File tarExtractionDirectory, File dockerTar){
        List<File> untaredFiles = new ArrayList<>()
        final File outputDir = new File(tarExtractionDirectory, dockerTar.getName())
        def tarArchiveInputStream = new TarArchiveInputStream(new FileInputStream(dockerTar))
        try {
            def tarArchiveEntry
            while (null != (tarArchiveEntry = tarArchiveInputStream.getNextTarEntry())) {
                final File outputFile = new File(outputDir, tarArchiveEntry.getName())
                if (tarArchiveEntry.isDirectory()) {
                    outputFile.mkdirs()
                } else if(tarArchiveEntry.name.contains('layer.tar')){
                    final OutputStream outputFileStream = new FileOutputStream(outputFile)
                    try{
                        IOUtils.copy(tarArchiveInputStream, outputFileStream)
                        untaredFiles.add(outputFile)
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

    private void parseLayerTarAndExtract(String extractionPattern, File layerTar, File layerOutputDir){
        def layerInputStream = new TarArchiveInputStream(new FileInputStream(layerTar))
        try {
            def layerEntry
            while (null != (layerEntry = layerInputStream.getNextTarEntry())) {
                try{
                    if(shouldExtractEntry(extractionPattern, layerEntry.name)){
                        final File outputFile = new File(layerOutputDir, layerEntry.getName())
                        if (layerEntry.isFile()) {
                            if(!outputFile.getParentFile().exists()){
                                outputFile.getParentFile().mkdirs()
                            }
                            final OutputStream outputFileStream = new FileOutputStream(outputFile)
                            try{
                                IOUtils.copy(layerInputStream, outputFileStream)
                            } finally{
                                outputFileStream.close()
                            }
                        }
                    }
                }catch(Exception e){
                    logger.error(e.toString())
                }
            }
        } finally {
            IOUtils.closeQuietly(layerInputStream)
        }
    }


    boolean shouldExtractEntry(String extractionPattern, String entryName){
        entryName.matches(extractionPattern)
    }
}
