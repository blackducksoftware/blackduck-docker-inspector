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

import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum
import com.blackducksoftware.integration.hub.docker.tar.manifest.ImageInfo
import com.blackducksoftware.integration.hub.exception.HubIntegrationException
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser

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
				File layerTar = layerTars.find{
					StringUtils.compare(layer, it.getParentFile().getName()) == 0
				}
				if(layerTar != null){
					def imageOutputDir = new File(imageFilesDir, mapping.getImageDirectory())
					logger.trace("Processing layer: ${layerTar.getAbsolutePath()}")
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

	private String extractManifestFileContent(String dockerTarName){
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
		try {
			List<ImageInfo> images = getManifestContents(tarFileName)
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

	private List<ImageInfo> getManifestContents(String tarFileName){
		logger.debug("getManifestContents()")
		List<ImageInfo> images = new ArrayList<>()
		logger.debug("getManifestContents(): extracting manifest file content")
		def manifestContentString = extractManifestFileContent(tarFileName)
		logger.debug("getManifestContents(): parsing: ${manifestContentString}")
		JsonParser parser = new JsonParser()
		JsonArray manifestContent = parser.parse(manifestContentString).getAsJsonArray()
		Gson gson = new Gson()
		for(JsonElement element : manifestContent) {
			logger.debug("getManifestContents(): element: ${element.toString()}")
			images.add(gson.fromJson(element, ImageInfo.class))
		}
		images
	}

	// TODO this method needs to be split up
	private void parseLayerTarAndExtract(File layerTar, File layerOutputDir){
		logger.debug("layerTar: ${layerTar.getAbsolutePath()}")
		def layerInputStream = new TarArchiveInputStream(new FileInputStream(layerTar), "UTF-8")
		try {
			layerOutputDir.mkdirs()
			logger.debug("layerOutputDir: ${layerOutputDir.getAbsolutePath()}")
			Path layerOutputDirPath = layerOutputDir.toPath()
			TarArchiveEntry layerEntry
			while (null != (layerEntry = layerInputStream.getNextTarEntry())) {
				try {
					String fileSystemEntryName = layerEntry.getName()
					logger.trace("Processing layerEntry: ${fileSystemEntryName}")
					if ((fileSystemEntryName.startsWith('.wh.')) || (fileSystemEntryName.contains('/.wh.')))   {
						logger.trace("Found white-out file ${fileSystemEntryName}")
						int whiteOutMarkIndex = fileSystemEntryName.indexOf('.wh.')
						String beforeWhiteOutMark = fileSystemEntryName.substring(0, whiteOutMarkIndex)
						String afterWhiteOutMark = fileSystemEntryName.substring(whiteOutMarkIndex + ".wh.".length())
						String filePathToRemove = "${beforeWhiteOutMark}${afterWhiteOutMark}"
						final File fileToRemove = new File(layerOutputDir, filePathToRemove)
						logger.trace("Removing ${filePathToRemove} from image (this layer whites it out)")
						if (fileToRemove.isDirectory()) {
							try {
								fileToRemove.deleteDir()
								logger.trace("Directory ${filePathToRemove} successfully removed")
							} catch (Exception e) {
								logger.warn("Error removing whited-out directory ${filePathToRemove}")
							}
						} else {
							try {
								Files.delete(fileToRemove.toPath())
								logger.trace("File ${filePathToRemove} successfully removed")
							} catch (Exception e) {
								logger.warn("Error removing whited-out file ${filePathToRemove}")
							}
						}
						continue
					}
					if(layerEntry.isSymbolicLink() || layerEntry.isLink()) {
						logger.trace("Processing link: ${fileSystemEntryName}")
						Path startLink = null
						try {
							startLink = Paths.get(layerOutputDir.getAbsolutePath(), fileSystemEntryName)
						} catch (InvalidPathException e) {
							logger.warn("Error extracting symbolic link ${fileSystemEntryName}: Error creating Path object: ${e.getMessage()}")
							continue
						}
						Path endLink = null
						logger.trace("Getting link name from layer entry")
						String linkPath = layerEntry.getLinkName()
						logger.trace("layerEntry.getLinkName(): ${linkPath}")
						logger.trace("Checking link type")
						if(layerEntry.isSymbolicLink()){
							logger.trace("${layerEntry.name} is a symbolic link")
							logger.trace("Calculating endLink: startLink: ${startLink.toString()}; layerEntry.getLinkName(): ${layerEntry.getLinkName()}")
							if (linkPath.startsWith('/')) {
								String relLinkPath = "." + linkPath
								logger.trace("endLink made relative: ${relLinkPath}")
								endLink =  layerOutputDirPath.resolve(relLinkPath)
							} else {
								endLink = startLink.resolveSibling(layerEntry.getLinkName())
							}
							logger.trace("normalizing ${endLink.toString()}")
							endLink = endLink.normalize()
							logger.trace("endLink: ${endLink.toString()}")
							try {
								try {
									Files.delete(startLink) // remove lower layer's version if exists
								} catch (IOException e) {
									// expected (most of the time)
								}
								Files.createSymbolicLink(startLink, endLink)
							} catch (FileAlreadyExistsException e) {
								String msg = "FileAlreadyExistsException creating symbolic link from ${startLink.toString()} to ${endLink.toString()}; " +
										"this will not affect the results unless it affects a file needed by the package manager; " +
										"Error: ${e.getMessage()}"
								throw new HubIntegrationException(msg)
							}
						} else if (layerEntry.isLink()) {
							logger.trace("${layerEntry.name} is a hard link")
							logger.trace("Calculating endLink: startLink: ${startLink.toString()}; layerEntry.getLinkName(): ${layerEntry.getLinkName()}")
							endLink =  layerOutputDirPath.resolve(layerEntry.getLinkName())
							logger.trace("normalizing ${endLink.toString()}")
							endLink = endLink.normalize()
							logger.trace("endLink: ${endLink.toString()}")

							logger.trace("${layerEntry.name} is a hard link: ${startLink.toString()} -> ${endLink.toString()}")
							File targetFile = endLink.toFile()
							if (!targetFile.exists()) {
								logger.warn("Attempting to create a link to ${targetFile}, but it does not exist")
							}
							try {
								Files.createLink(startLink, endLink)
							} catch (NoSuchFileException|FileAlreadyExistsException e) {
								logger.warn("Error creating hard link from ${startLink.toString()} to ${endLink.toString()}; " +
										"this will not affect the results unless it affects a file needed by the package manager; " +
										"Error: ${e.getMessage()}")
							}
						}
					} else {

						logger.trace("Processing file/dir: ${fileSystemEntryName}")

						final File outputFile = new File(layerOutputDir, fileSystemEntryName)
						if (layerEntry.isFile()) {
							logger.trace("Processing file: ${fileSystemEntryName}")
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
					logger.error("Error extracting files from layer tar: ${e.toString()}")
				}
			}
		} finally {
			IOUtils.closeQuietly(layerInputStream)
		}
	}
}