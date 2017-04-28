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

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

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

    //    private static final String OS_EXTRACTION_PATTERN = "etc/(lsb-release|os-release)"
    //
    //    private static final String EXTRACTION_PATTERN = "(lib/apk.*|var/lib/(dpkg|rpm){1}.*|${OS_EXTRACTION_PATTERN})"

    File workingDirectory

    File extractDockerLayers(File dockerTar){
        File tarExtractionDirectory = new File(workingDirectory, 'tarExtraction')
        List<File> layerTars = extractLayerTars(tarExtractionDirectory, dockerTar)
        File layerFilesDir = new File(tarExtractionDirectory, 'layerFiles')
        layerTars.each { layerTar ->
            def layerName = layerTar.getName()
            if(StringUtils.compare(layerName,'layer.tar') == 0){
                layerName = layerTar.getParentFile().getName()
            }
            def layerOutputDir = new File(layerFilesDir, layerName)
            parseLayerTarAndExtract( layerTar, layerOutputDir)
            // parseLayerTarAndExtract(EXTRACTION_PATTERN, layerTar, layerOutputDir)
        }
        layerFilesDir
    }

    String extractManifestFileContent(String dockerTarName){
        File tarExtractionDirectory = new File(workingDirectory, 'tarExtraction')
        File dockerTarDirectory = new File(tarExtractionDirectory, dockerTarName)
        File manifest = new File(dockerTarDirectory, 'manifest.json')
        StringUtils.join(manifest.readLines(), '\n')
    }

    OperatingSystemEnum detectOperatingSystem(String operatingSystem, File layerFilesDir) {
        OperatingSystemEnum osEnum
        if(StringUtils.isNotBlank(operatingSystem)){
            osEnum = OperatingSystemEnum.determineOperatingSystem(operatingSystem)
        } else{
            logger.trace("Layer directory ${layerFilesDir.getName()}, looking for etc")
            List<File> etcFiles = findFileWithName(layerFilesDir, 'etc')
            if (etcFiles == null) {
                String msg = "Unable to find the files that specify the Linux distro of this image. You'll need to run with the --linux.distro option"
                throw new HubIntegrationException(msg)
            }
            for(File etcFile : etcFiles){
                try{
                    osEnum = detectOperatingSystemFromEtcDir(etcFile)
                    if(osEnum != null){
                        break
                    }
                } catch (HubIntegrationException e){
                    logger.debug(e.toString())
                }
            }
        }

        Set<PackageManagerEnum> packageManagers = new HashSet<>()
        layerFilesDir.listFiles().each { layerDirectory ->
            List<File> libDirs = findFileWithName(layerDirectory, 'lib')
            if(libDirs != null){
                libDirs.each{ libDir ->
                    libDir.listFiles().each { packageManagerDirectory ->
                        try{
                            packageManagers.add(PackageManagerEnum.getPackageManagerEnumByName(packageManagerDirectory.getName()))
                        } catch (IllegalArgumentException e){
                            logger.debug(e.toString())
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

    TarExtractionResults extractPackageManagerDirs(File layerFilesDir, OperatingSystemEnum osEnum) {
        TarExtractionResults results = new TarExtractionResults()
        results.operatingSystemEnum = osEnum
        layerFilesDir.listFiles().each { layerDirectory ->
            logger.info("Extracting data from layer ${layerDirectory.getName()}")
            List<File> varDirs = findFileWithName(layerDirectory, 'var')
            if(varDirs == null){
                logger.debug("Could not find the lib directroy in ${layerDirectory.getAbsolutePath()}")
            } else {
                varDirs.each{ varDir ->
                    def libDir = new File(varDir, 'lib')
                    logger.trace('lib directory : '+libDir.getAbsolutePath())
                    libDir.listFiles().each { packageManagerDirectory ->
                        try{
                            PackageManagerEnum packageManager = PackageManagerEnum.getPackageManagerEnumByName(packageManagerDirectory.getName())
                            logger.trace(packageManagerDirectory.getAbsolutePath())
                            TarExtractionResult result = new TarExtractionResult()
                            result.layer = layerDirectory.getName()
                            result.packageManager = packageManager
                            result.extractedPackageManagerDirectory = packageManagerDirectory
                            results.extractionResults.add(result)
                        } catch (IllegalArgumentException e){
                            logger.debug(e.toString())
                        }
                    }
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

    private List<File> findFileWithName(File fileToSearch, String name){
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

    private List<File> extractLayerTars(File tarExtractionDirectory, File dockerTar){
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

    private void parseLayerTarAndExtract(File layerTar, File layerOutputDir){
        // private void parseLayerTarAndExtract(String extractionPattern, File layerTar, File layerOutputDir){
        def layerInputStream = new TarArchiveInputStream(new FileInputStream(layerTar))
        try {
            layerOutputDir.mkdirs()
            def layerEntry
            while (null != (layerEntry = layerInputStream.getNextTarEntry())) {
                try{
                    // if(shouldExtractEntry(extractionPattern, layerEntry.name)){
                    if(layerEntry.isSymbolicLink()){
                        logger.debug("${layerEntry.name} is a symbolic link")
                        Path startLink = Paths.get(layerOutputDir.getAbsolutePath(), layerEntry.getName())
                        Path endLink = null
                        String linkPath = layerEntry.getLinkName()
                        if (linkPath.startsWith('.')) {
                            endLink =  startLink.resolveSibling(layerEntry.getLinkName())
                            endLink = endLink.normalize()
                        } else {
                            endLink = Paths.get(layerOutputDir.getAbsolutePath(), layerEntry.getLinkName())
                            endLink = endLink.normalize()
                        }
                        Files.createSymbolicLink(startLink, endLink)

                    } else {
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
                    //   }
                } catch(Exception e) {
                    logger.debug(e.toString())
                }
            }
        } finally {
            IOUtils.closeQuietly(layerInputStream)
        }
    }

    //    boolean shouldExtractEntry(String extractionPattern, String entryName){
    //        entryName.matches(extractionPattern)
    //    }
}
