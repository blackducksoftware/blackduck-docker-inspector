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

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum
import com.blackducksoftware.integration.hub.exception.HubIntegrationException

@Component
class DockerTarParser {
	private final Logger logger = LoggerFactory.getLogger(DockerTarParser.class)
	public static final String TAR_EXTRACTION_DIRECTORY = 'tarExtraction'

	File workingDirectory

	File extractDockerLayers(List<File> layerTars, List<LayerMapping> layerMappings){
		File tarExtractionDirectory = getTarExtractionDirectory()
		File imageFilesDir = new File(tarExtractionDirectory, 'imageFiles')

		layerMappings.each { mapping ->
			mapping.layers.each { layer ->
				File layerTar = layerTars.find{
					StringUtils.compare(layer, it.getParentFile().getName()) == 0
				}
				if(layerTar != null){
					def imageOutputDir = new File(imageFilesDir, mapping.getImageDirectory())
					parseLayerTarAndExtract( layerTar, imageOutputDir)
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

	String extractManifestFileContent(String dockerTarName){
		File tarExtractionDirectory = getTarExtractionDirectory()
		File dockerTarDirectory = new File(tarExtractionDirectory, dockerTarName)
		File manifest = new File(dockerTarDirectory, 'manifest.json')
		StringUtils.join(manifest.readLines(), '\n')
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
					logger.trace(packageManagerDirectory.getAbsolutePath())
					TarExtractionResult result = new TarExtractionResult()
					result.imageDirectoryName = imageDirectory.getName()
					result.packageManager = packageManagerEnum
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

	private void parseLayerTarAndExtract(File layerTar, File layerOutputDir){
		def layerInputStream = new TarArchiveInputStream(new FileInputStream(layerTar), "UTF-8")
		try {
			layerOutputDir.mkdirs()
			def layerEntry
			while (null != (layerEntry = layerInputStream.getNextTarEntry())) {
				try{
					if(layerEntry.isSymbolicLink() || layerEntry.isLink()) {
						logger.trace("Processing link: ${layerEntry.getName()}")
						logger.trace("Output dir path: ${layerOutputDir.getAbsolutePath()}")
						Path startLink
						try {
							startLink = Paths.get(layerOutputDir.getAbsolutePath(), layerEntry.getName())
						} catch (InvalidPathException e) {
							logger.error("Error extracting symbolic link ${layerEntry.getName()}: Error creating Path object: ${e.getMessage()}")
							continue
						}
						Path endLink = null
						logger.trace("Getting link name from layer entry")
						String linkPath = layerEntry.getLinkName()
						logger.trace("checking first char")
						if (linkPath.startsWith('.')) {
							logger.trace("resolving sibling")
							endLink =  startLink.resolveSibling(layerEntry.getLinkName())
							logger.trace("normalizing")
							endLink = endLink.normalize()
						} else {
							logger.trace("getting end link via Paths.get()")
							logger.trace("Processing link: ${layerEntry.getLinkName()}")
							logger.trace("Output dir path: ${layerOutputDir.getAbsolutePath()}")
							//                            endLink = Paths.get(layerOutputDir.getAbsolutePath(), layerEntry.getLinkName())
							Path targetDirPath = FileSystems.getDefault().getPath(layerOutputDir.getAbsolutePath());

							Path targetFilePath
							try {
								targetFilePath = FileSystems.getDefault().getPath(layerEntry.getLinkName())
							} catch (InvalidPathException e) {
								logger.error("Error extracting symbolic link to file ${layerEntry.getLinkName()}: Error creating Path object: ${e.getMessage()}")
								continue
							}
							endLink = targetDirPath.resolve(targetFilePath)
							logger.trace("normalizing ${endLink.toString()}")
							endLink = endLink.normalize()
						}
						logger.trace("Checking link type")
						if(layerEntry.isSymbolicLink()){
							logger.trace("${layerEntry.name} is a symbolic link")
							Files.createSymbolicLink(startLink, endLink)
						} else if(layerEntry.isLink()){
							logger.trace("${layerEntry.name} is a hard link")
							try {
								Files.createLink(startLink, endLink)
							} catch (NoSuchFileException e) {
								logger.debug("NoSuchFileException creating hard link from ${startLink.toString()} to ${endLink.toString()}; " +
										"this will not affect the results unless it affects a file needed by the package manager")
							}
						}
					} else {
						logger.trace("Processing file/dir: ${layerEntry.getName()}")
						final File outputFile = new File(layerOutputDir, layerEntry.getName())
						if (layerEntry.isFile()) {
							logger.trace("Processing file: ${layerEntry.getName()}")
							if(!outputFile.getParentFile().exists()){
								outputFile.getParentFile().mkdirs()
							}
							logger.trace("Creating output stream for ${outputFile.getName()}")
							final OutputStream outputFileStream = new FileOutputStream(outputFile)
							try{
								IOUtils.copy(layerInputStream, outputFileStream)
							} finally{
								outputFileStream.close()
							}
						} else {
							outputFile.mkdirs()
						}
					}
				} catch(Exception e) {
					logger.error("Error extracting files from layer tar: ${e.toString()}", e)
				}
			}
		} finally {
			IOUtils.closeQuietly(layerInputStream)
		}
	}
}