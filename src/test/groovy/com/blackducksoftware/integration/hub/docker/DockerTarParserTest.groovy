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

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import java.util.List

import org.junit.Ignore
import org.junit.Test
import static org.junit.Assert.*
import com.blackducksoftware.integration.hub.docker.tar.DockerTarParser
import com.blackducksoftware.integration.hub.docker.tar.LayerMapping
import static java.nio.file.StandardCopyOption.*;

class DockerTarParserTest {
	
	@Test
	void testPerformExtractOfDockerTarSimple() {
		doTest("simple")
	}
	
	@Test
	void testPerformExtractOfDockerTarWithSymbolicLink() {
		doTest("withSymbolicLink")
	}

    void doTest(String type) {
		File workingDirectory = createTempDirectory()
		File tarExtractionDirectory = new File(workingDirectory, DockerTarParser.TAR_EXTRACTION_DIRECTORY)
		File layerDir = new File(tarExtractionDirectory, "ubuntu_latest.tar/layerId1")
		layerDir.mkdirs()
		Path layerDirPath = Paths.get(layerDir.getAbsolutePath());

        File dockerTar = new File(layerDir, "layer.tar")
		Files.copy((new File("src/test/resources/${type}/layer.tar")).toPath(), dockerTar.toPath(), REPLACE_EXISTING)
		List<File> layerTars = new ArrayList<>()
		layerTars.add(dockerTar)
		
        DockerTarParser tarParser = new DockerTarParser()
        tarParser.workingDirectory = workingDirectory
		
		List<LayerMapping> layerMappings = new ArrayList<>()
		LayerMapping layerMapping = new LayerMapping()
		layerMapping.imageName = "image1"
		layerMapping.tagName = "image1tag1"
		Set<String> layerIds = new HashSet<>()
		layerIds.add("layerId1")
		layerMapping.layers = layerIds
		layerMappings.add(layerMapping)

        File results = tarParser.extractDockerLayers(layerTars, layerMappings)
		assertEquals(tarExtractionDirectory.getAbsolutePath() + "/imageFiles", results.getAbsolutePath())
		
		File dpkgStatusFile = new File(workingDirectory.getAbsolutePath() + "/tarExtraction/imageFiles/image_image1_v_image1tag1/var/lib/dpkg/status")
		assertTrue(dpkgStatusFile.exists())
		
		assertEquals(98016, dpkgStatusFile.size())
    }


public static File createTempDirectory() throws IOException {
		final File temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
		if(!(temp.delete())) {
			throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
		}
		if(!(temp.mkdir())) {
			throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
		}
		return (temp);
	}

}
