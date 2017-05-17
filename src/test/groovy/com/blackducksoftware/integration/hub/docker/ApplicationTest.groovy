package com.blackducksoftware.integration.hub.docker;

import java.util.List
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
	public void test() {
		Application app = new Application()
		app.dockerImage = "ubuntu"
		app.dockerImages = new DockerImages()
		
		app.hubClient = [
				isValid: { true },
				assertValid: {},
				testHubConnection: {},
				uploadBdioToHub: { File bdioFile -> }
			] as HubClient
			
			app.hubDockerManager = [
				init: { },
				getTarFileFromDockerImage: { new File("src/test/resources/simple/layer.tar") },
				extractLayerTars: { File dockerTar -> null },
				cleanWorkingDirectory: {},
				generateBdioFromPackageMgrDirs: {null},
				deriveDockerTarFile: {null},
				getTarFileFromDockerImage: {String imageName, String tagName -> new File("src/test/resources/image.tar")},
				extractDockerLayers: {List<File> layerTars, List<LayerMapping> layerMappings -> null},
				detectOperatingSystem: {String operatingSystem, File extractedFilesDir -> OperatingSystemEnum.UBUNTU},
				detectCurrentOperatingSystem: {OperatingSystemEnum.UBUNTU},
				generateBdioFromImageFilesDir: {List<LayerMapping> mappings, String projectName, String versionName, File dockerTar, File imageFilesDir, OperatingSystemEnum osEnum -> new ArrayList<File>()}
			] as HubDockerManager
			
		
		app.init()
	}

}
