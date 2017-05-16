package com.blackducksoftware.integration.hub.docker.client

import com.blackducksoftware.integration.hub.docker.TestUtils

import static org.junit.Assert.*

import org.junit.Test

class ProgramPathsTest {

	@Test
	public void test() {
		File installDir = TestUtils.createTempDirectory()
		String installDirPath = installDir.getAbsolutePath()
		File jarFile = new File(installDir, "hub-docker-1.0.0.jar")
		jarFile.createNewFile()
		
		ProgramPaths paths = new ProgramPaths()
		paths.getMetaClass().getQualifiedJarPath = { -> return "SOMEJUNK${installDirPath}/hub-docker-1.0.0.jarOTHERJUNK" }
		
		paths.hubDockerPgmDirPath = installDir.getAbsolutePath()
		paths.init()
		
		assertEquals("${installDirPath}/config/".toString(), paths.getHubDockerConfigDirPath())
		assertEquals("${installDirPath}/config/application.properties".toString(), paths.getHubDockerConfigFilePath())
		assertEquals("${installDirPath}/target/".toString(), paths.getHubDockerTargetDirPath())
		assertEquals("${installDirPath}/".toString(), paths.getHubDockerPgmDirPath())
		assertEquals("${installDirPath}/hub-docker-1.0.0.jar".toString(), paths.getHubDockerJarPath())
	}

}
