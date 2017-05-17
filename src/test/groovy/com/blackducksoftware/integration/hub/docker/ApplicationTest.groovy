package com.blackducksoftware.integration.hub.docker;

import java.util.List
import com.blackducksoftware.integration.hub.docker.client.DockerClientManager
import com.blackducksoftware.integration.hub.docker.image.DockerImages
import com.blackducksoftware.integration.hub.docker.tar.LayerMapping



import static org.junit.Assert.*

import java.io.File

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

class ApplicationTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testCanUseCurrentContainer() {
		doTest("ubuntu", OperatingSystemEnum.UBUNTU, true)
	}
	
	@Test
	public void testNeedDifferentContainer() {
		doTest("alpine", OperatingSystemEnum.ALPINE, false)
	}
	
	private void doTest(String targetImageName, OperatingSystemEnum targetOsEnum, boolean expectBdioUpload) {
		Application app = new Application()
		app.dockerImage = targetImageName
		app.dockerImages = new DockerImages()
		
		app.hubClient = [
				isValid: { true },
				assertValid: {},
				testHubConnection: {},
				uploadBdioToHub: { File bdioFile -> }
			] as HubClient
			
			List<File> layerTarFiles = new ArrayList<>()
			File tarFile = new File("src/test/resources/simple/layer.tar")
			layerTarFiles.add(tarFile)
			
			List<File> bdioFiles = new ArrayList<>()
			File bdioFile = new File("src/test/resources/bdioFile.jsonld")
			bdioFiles.add(bdioFile)
			
			boolean uploadedBdioFiles = false
			boolean invokedSubContainer = false
			
			app.hubDockerManager = [
				init: { },
				getTarFileFromDockerImage: { new File("src/test/resources/simple/layer.tar") },
				extractLayerTars: { File dockerTar -> layerTarFiles },
				cleanWorkingDirectory: {},
				generateBdioFromPackageMgrDirs: {null},
				deriveDockerTarFile: {null},
				getTarFileFromDockerImage: {String imageName, String tagName -> new File("src/test/resources/image.tar")},
				extractDockerLayers: {List<File> layerTars, List<LayerMapping> layerMappings -> null},
				detectOperatingSystem: {String operatingSystem, File extractedFilesDir -> targetOsEnum},
				detectCurrentOperatingSystem: {OperatingSystemEnum.UBUNTU},
				generateBdioFromImageFilesDir: {List<LayerMapping> mappings, String projectName, String versionName, File dockerTar, File imageFilesDir, OperatingSystemEnum osEnum -> bdioFiles},
				extractManifestFileContent: {String tarFileName -> "[{\"Config\":\"ebcd9d4fca80e9e8afc525d8a38e7c56825dfb4a220ed77156f9fb13b14d4ab7.json\",\"RepoTags\":[\"${targetImageName}\"],\"Layers\":[\"68f9022b99e55f4856423cfcdc874e788299fc6147742a5551e10a62e8f2d521/layer.tar\",\"7cc064e7bb40e1237c51dbdf1a2a1d5c533ed67a27936b374adc87886da98a52/layer.tar\",\"09918b7293542c699c6b0c18c7c921472e574e279d42f43af2d70ec9d99dc608/layer.tar\",\"a9ee6ee9019fc117b5290d8f461d0312ff5c06e30b11cc96e43971573918f1b9/layer.tar\",\"9a9fb3763c4d41a028e3427fb412ce68d32a349b7b9fbd8c94eb967117f9b31d/layer.tar\"]}]"},
				uploadBdioFiles: {List<File> bdioFilesToUpload -> uploadedBdioFiles = true}
			] as HubDockerManager
			

		app.dockerClientManager = [
			pullImage: {String runOnImageName, String runOnImageVersion -> },
			run: {String runOnImageName, String runOnImageVersion, File dockerTarFile, boolean devMode -> invokedSubContainer = true}
			] as DockerClientManager
		
		app.init()
		if (expectBdioUpload) {
			assertTrue(uploadedBdioFiles)
			assertFalse(invokedSubContainer)
		} else {
			assertFalse(uploadedBdioFiles)
			assertTrue(invokedSubContainer)
		}
	}

}
