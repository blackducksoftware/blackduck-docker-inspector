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
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum

class DockerTarParser {
    private final Logger logger = LoggerFactory.getLogger(DockerTarParser.class)

    private static final String OS_EXTRACTION_PATTERN = "etc/(lsb-release|os-release)"

    private static final String EXTRACTION_PATTERN = "(var/lib/(dpkg|apt|yum|rpm|apk){1}.*|${OS_EXTRACTION_PATTERN})"

    File workingDirectory

    TarExtractionResults parseImageTar(String operatingSystem, File dockerTar){
        File tarExtractionDirectory = new File(workingDirectory, 'tarExtraction')

        if(tarExtractionDirectory.exists()){
            FileUtils.deleteDirectory(tarExtractionDirectory)
        }

        List<File> layerTars = extractLayerTars(tarExtractionDirectory,dockerTar)
        def layerOutputDir = new File(tarExtractionDirectory, 'layerFiles')
        layerTars.each { layerTar ->
            parseLayerTarAndExtract(EXTRACTION_PATTERN, layerTar, layerOutputDir)
        }
        TarExtractionResults results = new TarExtractionResults()
        if(StringUtils.isNotBlank(operatingSystem)){
            results.operatingSystemEnum = OperatingSystemEnum.determineOperatingSystem(operatingSystem)
        } else{
            results.operatingSystemEnum = extractOperatingSystemFromFile(new File(layerOutputDir, 'etc').listFiles()[0])
        }
        def packageManagerFiles =  new File(layerOutputDir, 'var/lib')
        packageManagerFiles.listFiles().each { packageManagerDirectory ->
            TarExtractionResult result = new TarExtractionResult()
            result.packageManager =PackageManagerEnum.getPackageManagerEnumByName(packageManagerDirectory.getName())
            result.extractedPackageManagerDirectory = packageManagerDirectory
            results.extractionResults.add(result)
        }
        results
    }

    OperatingSystemEnum parseImageTarForOperatingSystemOnly(File dockerTar){
        File tarExtractionDirectory = new File(workingDirectory, 'tarExtraction')

        if(tarExtractionDirectory.exists()){
            FileUtils.deleteDirectory(tarExtractionDirectory)
        }

        List<File> layerTars = extractLayerTars(tarExtractionDirectory,dockerTar)
        def layerOutputDir = new File(tarExtractionDirectory, 'layerFiles')
        layerTars.each { layerTar ->
            parseLayerTarAndExtract(OS_EXTRACTION_PATTERN, layerTar, layerOutputDir)
        }
        extractOperatingSystemFromFile(new File(layerOutputDir, 'etc').listFiles()[0])
    }

    private OperatingSystemEnum extractOperatingSystemFromFile(File osFile){
        OperatingSystemEnum osEnum = null
        String linePrefix = null
        if(osFile.getName().equals('lsb-release')){
            linePrefix = 'DISTRIB_ID='
        } else if(osFile.getName().equals('os-release')){
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
        osEnum
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
