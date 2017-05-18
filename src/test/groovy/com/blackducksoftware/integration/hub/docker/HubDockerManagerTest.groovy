package com.blackducksoftware.integration.hub.docker;

import java.io.File
import java.util.List

import com.blackducksoftware.integration.hub.docker.tar.LayerMapping
import com.blackducksoftware.integration.hub.docker.tar.TarExtractionResult
import com.blackducksoftware.integration.hub.docker.tar.TarExtractionResults

import static org.junit.Assert.*
import org.apache.commons.io.FileUtils
import com.blackducksoftware.integration.hub.docker.client.DockerClientManager
import com.blackducksoftware.integration.hub.docker.executor.DpkgExecutor
import com.blackducksoftware.integration.hub.docker.extractor.DpkgExtractor
import com.blackducksoftware.integration.hub.docker.extractor.Extractor
import com.blackducksoftware.integration.hub.docker.tar.DockerTarParser
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

class HubDockerManagerTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
		String imageName = "ubuntu"
		String tagName = "1.0"
		OperatingSystemEnum os = OperatingSystemEnum.UBUNTU
		
		def packages = new File('src/test/resources/ubuntu_dpkg_output_1.txt') as String[]

		File imageTarFile = new File("test/image.tar")
		
		List<Extractor> extractors = new ArrayList<>()
		Extractor dpkgExtractor = new DpkgExtractor()
		dpkgExtractor.executor = [
			listPackages: {-> packages}
			] as DpkgExecutor
		dpkgExtractor.executor.init()
		dpkgExtractor.init()
		extractors.add(dpkgExtractor)
		
		HubDockerManager mgr = new HubDockerManager()
		mgr.packageManagerFiles = [
			stubPackageManagerFiles: {TarExtractionResult result -> println "stubPackageManagerFiles() mocked"}
			] as PackageManagerFiles
		mgr.workingDirectoryPath = TestUtils.createTempDirectory().getAbsolutePath()
		mgr.hubClient = [
			] as HubClient
		mgr.dockerClientManager = [
			getTarFileFromDockerImage: {String name, String tag -> imageTarFile}
			] as DockerClientManager
		mgr.extractors = extractors
		
		TarExtractionResults tarExtractionResults = new TarExtractionResults()
		tarExtractionResults.operatingSystemEnum = os
		List<TarExtractionResult> extractionResults = new ArrayList<>()
		TarExtractionResult result = new TarExtractionResult()
		result.imageDirectoryName = "image_${imageName}_v_${tagName}"
		result.extractedPackageManagerDirectory = new File("test/resources/imageDir")
		result.packageManager = PackageManagerEnum.DPKG
		extractionResults.add(result)
		tarExtractionResults.extractionResults = extractionResults
		
		mgr.tarParser = [
			extractPackageManagerDirs: {File imageFilesDir, OperatingSystemEnum osEnum -> tarExtractionResults}
			] as DockerTarParser
		
		assertEquals("image.tar", mgr.getTarFileFromDockerImage(imageName, tagName).getName())
		
		List<LayerMapping> mappings = new ArrayList<LayerMapping>()
		LayerMapping mapping = new LayerMapping()
		mapping.imageName = imageName
		mapping.tagName = tagName
		mapping.layers = new ArrayList<>()
		mapping.layers.add("testLayerId")
		mappings.add(mapping)
		File imageFilesDir
		List<File> bdioFiles = mgr.generateBdioFromImageFilesDir(mappings, "testProjectName", "testProjectVersion", imageTarFile, imageFilesDir, os)
		for (File bdioFile : bdioFiles) {
				println "${bdioFile.getAbsolutePath()}"
		}

		File file1 = new File("src/test/resources/imageDir_testProjectName_testProjectVersion_bdio.jsonld")
		File file2 = bdioFiles.get(0)
		println "Comparing ${file2.getAbsolutePath()} to ${file1.getAbsolutePath()}"
		boolean filesAreEqual = TestUtils.contentEquals(file1, file2, ["\"@id\":", "\"externalSystemTypeId\":"])
		assertTrue(filesAreEqual)
	}

}
