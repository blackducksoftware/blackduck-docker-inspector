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
package com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker


import static java.nio.file.StandardCopyOption.*;
import static org.junit.Assert.*

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import org.junit.Ignore
import org.junit.Test

import com.blackducksoftware.integration.hub.docker.imageinspector.TestUtils
import com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.manifest.HardwiredManifestFactory
import com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.manifest.ManifestLayerMapping
import com.blackducksoftware.integration.hub.docker.imageinspector.lib.OperatingSystemEnum

import groovy.io.FileType

class DockerTarParserTest {
    private final static int DPKG_STATUS_FILE_SIZE = 98016

    private static final String IMAGE_NAME = "blackducksoftware/centos_minus_vim_plus_bacula"

    private static final String IMAGE_TAG = "1.0"

    private static final String LAYER_ID = "layerId1"


    @Test
    void testExtractFullImage() {
        File dockerTar = new File("build/images/test/centos_minus_vim_plus_bacula.tar")
        File workingDirectory = TestUtils.createTempDirectory()
        println "workingDirectory: ${workingDirectory.getAbsolutePath()}"

        DockerTarParser tarParser = new DockerTarParser()
        tarParser.workingDirectory = workingDirectory
        tarParser.manifestFactory = new HardwiredManifestFactory()

        List<File> layerTars = tarParser.extractLayerTars(dockerTar)
        List<ManifestLayerMapping> layerMappings = tarParser.getLayerMappings(dockerTar.getName(), IMAGE_NAME, IMAGE_TAG)
        assertEquals(1, layerMappings.size())
        assertEquals(2, layerMappings.get(0).layers.size())
        File imageFilesDir = tarParser.extractDockerLayers("imageName", "imageTag", layerTars, layerMappings)
        OperatingSystemEnum targetOsEnum = tarParser.detectOperatingSystem(imageFilesDir)
        ImageInfoParsed tarExtractionResults = tarParser.collectPkgMgrInfo(imageFilesDir, targetOsEnum)

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

        // MacOS file system does not preserve case which throws off the count
        println "Extracted ${numFilesFound} files"
        assertTrue(numFilesFound > 18000)
        assertTrue(numFilesFound < 19000)
    }

    @Test
    void testExtractDockerLayerTarSimple() {
        doLayerTest("simple")
    }

    // Strangely, the target file of links in docker tars all seem to
    // be relative to the file system root, which is bizarre
    // As a result, this test fails now that the code works
    // on links within real docker images
    @Ignore
    @Test
    void testExtractDockerLayerTarWithSymbolicLink() {
        doLayerTest("withSymbolicLink")
    }

    void doLayerTest(String testFileDir) {
        String underscoredImageName = IMAGE_NAME.replace('/', '_')
        String targetImageFileSystemRootDirName = "image_${underscoredImageName}_v_${IMAGE_TAG}"
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
        tarParser.manifestFactory = new HardwiredManifestFactory()

        List<ManifestLayerMapping> layerMappings = new ArrayList<>()
        List<String> layerIds = new ArrayList<>()
        layerIds.add(LAYER_ID)
        ManifestLayerMapping layerMapping = new ManifestLayerMapping(IMAGE_NAME, IMAGE_TAG, layerIds)
        layerMappings.add(layerMapping)

        File targetImageFileSystemRootDir = tarParser.extractDockerLayers("blackducksoftware_centos_minus_vim_plus_bacula", "1.0", layerTars, layerMappings)
        assertEquals(tarExtractionDirectory.getAbsolutePath() + "/imageFiles/${targetImageFileSystemRootDirName}", targetImageFileSystemRootDir.getAbsolutePath())

        File dpkgStatusFile = new File(workingDirectory.getAbsolutePath() + "/tarExtraction/imageFiles/${targetImageFileSystemRootDirName}/var/lib/dpkg/status")
        assertTrue(dpkgStatusFile.exists())

        assertEquals(DPKG_STATUS_FILE_SIZE, dpkgStatusFile.size())
    }
}