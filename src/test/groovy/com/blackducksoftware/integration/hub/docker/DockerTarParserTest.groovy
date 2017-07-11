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
package com.blackducksoftware.integration.hub.docker

import static java.nio.file.StandardCopyOption.*;
import static org.junit.Assert.*

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import org.junit.Ignore
import org.junit.Test

import com.blackducksoftware.integration.hub.docker.tar.DockerTarParser
import com.blackducksoftware.integration.hub.docker.tar.LayerMapping
import com.blackducksoftware.integration.hub.docker.tar.TarExtractionResults

import groovy.io.FileType

class DockerTarParserTest {
	private final static int DPKG_STATUS_FILE_SIZE = 98016

	private static final String IMAGE_NAME = "centos_minus_vim_plus_bacula"

	private static final String IMAGE_TAG = "1"

	private static final String LAYER_ID = "layerId1"

	// This test requires a full docker image tarfile, which is pretty big for source control
	@Ignore
	@Test
	void testExtractFullImage() {
		File dockerTar = new File("src/test/resources/centos_minus_vim_plus_bacula.tar")
		File workingDirectory = TestUtils.createTempDirectory()
		println "workingDirectory: ${workingDirectory.getAbsolutePath()}"

		DockerTarParser tarParser = new DockerTarParser()
		tarParser.workingDirectory = workingDirectory

		List<File> layerTars = tarParser.extractLayerTars(dockerTar)
		List<LayerMapping> layerMappings = tarParser.getLayerMappings(dockerTar.getName(), IMAGE_NAME, IMAGE_TAG)
		assertEquals(1, layerMappings.size())
		assertEquals(3, layerMappings.get(0).layers.size())
		assertEquals("2a0fa5e88024009238839366f837dd956d6cd5ec47c69a27ee2d29f7043b311e", layerMappings.get(0).layers.get(0))
		assertEquals("be1e7f55c85cbbee044eb3390a68a217796cbed6ca20f2d928e99127405873c5", layerMappings.get(0).layers.get(1))
		assertEquals("43ee44a345a69374e3207bd2777c574a1b74532ee4e928108bfe0883908fc7e0", layerMappings.get(0).layers.get(2))
		File imageFilesDir = tarParser.extractDockerLayers(layerTars, layerMappings)
		OperatingSystemEnum targetOsEnum = tarParser.detectOperatingSystem(null, imageFilesDir)
		TarExtractionResults tarExtractionResults = tarParser.extractPackageManagerDirs(imageFilesDir, targetOsEnum)

		boolean varLibRpmNameFound = false
		int numFilesFound = 0
		workingDirectory.eachFileRecurse(FileType.FILES) { file ->
			numFilesFound++
			if (file.getAbsolutePath().endsWith("var/lib/rpm/Name")) {
				println file.getAbsolutePath()
				varLibRpmNameFound = true
				String stringsOutput = "strings ${file.getAbsolutePath()}".execute().getText()
				assertTrue(stringsOutput.contains("bacula-console"))
				assertTrue(stringsOutput.contains("bacula-client"))
				assertTrue(stringsOutput.contains("bacula-director"))
			}
		}
		assertTrue(varLibRpmNameFound)
		assertEquals(162836, numFilesFound)
	}

	@Test
	void testExtractDockerLayerTarSimple() {
		doLayerTest("simple")
	}

	@Test
	void testExtractDockerLayerTarWithSymbolicLink() {
		doLayerTest("withSymbolicLink")
	}

	void doLayerTest(String testFileDir) {
		File workingDirectory = TestUtils.createTempDirectory()
		File tarExtractionDirectory = new File(workingDirectory, DockerTarParser.TAR_EXTRACTION_DIRECTORY)
		File layerDir = new File(tarExtractionDirectory, "ubuntu_latest.tar/${LAYER_ID}")
		layerDir.mkdirs()
		Path layerDirPath = Paths.get(layerDir.getAbsolutePath());

		File dockerTar = new File(layerDir, "layer.tar")
		Files.copy((new File("src/test/resources/${testFileDir}/layer.tar")).toPath(), dockerTar.toPath(), REPLACE_EXISTING)
		List<File> layerTars = new ArrayList<>()
		layerTars.add(dockerTar)

		DockerTarParser tarParser = new DockerTarParser()
		tarParser.workingDirectory = workingDirectory

		List<LayerMapping> layerMappings = new ArrayList<>()
		LayerMapping layerMapping = new LayerMapping()
		layerMapping.imageName = IMAGE_NAME
		layerMapping.tagName = IMAGE_TAG
		List<String> layerIds = new ArrayList<>()
		layerIds.add(LAYER_ID)
		layerMapping.layers = layerIds
		layerMappings.add(layerMapping)

		File results = tarParser.extractDockerLayers(layerTars, layerMappings)
		assertEquals(tarExtractionDirectory.getAbsolutePath() + "/imageFiles", results.getAbsolutePath())

		File dpkgStatusFile = new File(workingDirectory.getAbsolutePath() + "/tarExtraction/imageFiles/image_${IMAGE_NAME}_v_${IMAGE_TAG}/var/lib/dpkg/status")
		assertTrue(dpkgStatusFile.exists())

		assertEquals(DPKG_STATUS_FILE_SIZE, dpkgStatusFile.size())
	}
}