package com.blackducksoftware.integration.hub.docker


import static org.junit.Assert.*

import org.junit.Test

import com.blackducksoftware.integration.hub.docker.config.Config
import com.blackducksoftware.integration.hub.docker.config.DockerInspectorOption
import com.blackducksoftware.integration.hub.docker.imageinspector.config.ProgramPathUtils
import com.blackducksoftware.integration.hub.docker.imageinspector.config.ProgramPaths

class ProgramPathsTest {

    @Test
    public void testReleasedVersion() {
        doTest("hub-docker-1.0.0.jar", true)
    }

    @Test
    public void testSnapshotVersion() {
        doTest("hub-docker-0.0.1-SNAPSHOT.jar", false)
    }

    private void doTest(String jarFileName, boolean prefixCodeLocationName) {
        String prefix = "";
        if (prefixCodeLocationName) {
            prefix = "xyz";
        }
        List<DockerInspectorOption> configOptions = new ArrayList<>();
        configOptions.add(new DockerInspectorOption("hub.url", "hubUrl", "testHubUrl", "Hub URL", String.class, "", Config.GROUP_PUBLIC, false));
        Config config = [
            isOnHost: { true },
            isDryRun: { false },
            getLinuxDistro: { "" },
            getDockerTar: { "" },
            getDockerImage: { targetImageName },
            getDockerImageId: { "" },
            getTargetImageName: { "" },
            getDockerImageRepo: { targetImageName },
            getDockerImageTag : { "" },
            getHubUrl: { "test prop public string value" },
            setDockerImageRepo: {},
            setJarPath: {},
            getJarPath: { "/tmp/t.jar" },
            getHubCodelocationPrefix: { prefix },
            setHubCodelocationPrefix: { },
            setDockerImageTag: {
            },
            getHubUrl: { "testHubUrl" },
            getPublicConfigOptions: { configOptions },
            getRunId: { "" }
        ] as Config;

        File installDir = TestUtils.createTempDirectory()
        String installDirPath = installDir.getAbsolutePath()
        File jarFile = new File(installDir, "hub-docker-1.0.0.jar")
        jarFile.createNewFile()

        ProgramPaths paths = [
            getQualifiedJarPath: { -> return "file:${installDirPath}/${jarFileName}OTHERJUNK".toString() }
        ] as ProgramPaths

        paths.config = config;

        if (prefixCodeLocationName) {
            paths.codeLocationPrefix = prefixCodeLocationName
        }

        paths.setHubDockerPgmDirPathHost(installDir.getAbsolutePath() + "/")
        paths.init()

        assertTrue(paths.getHubDockerConfigDirPathHost().startsWith("${installDirPath}/config_".toString()))
        assertTrue(paths.getHubDockerConfigFilePathHost().startsWith("${installDirPath}/config_".toString()))
        assertTrue(paths.getHubDockerConfigFilePathHost().endsWith("/application.properties".toString()))
        assertTrue(paths.getHubDockerTargetDirPathHost().startsWith("${installDirPath}/target_".toString()))
        assertEquals("${installDirPath}/".toString(), paths.getHubDockerPgmDirPathHost())
        assertEquals("${installDirPath}/${jarFileName}".toString(), paths.getHubDockerJarPathActual())

        if (prefixCodeLocationName) {
            assertEquals(prefix + "_imageName_imageTag_pkgMgrFilePath_pkgMgrName", ProgramPathUtils.getCodeLocationName(prefix, "imageName", "imageTag",  "pkgMgrFilePath",  "pkgMgrName"))
        } else {
            assertEquals("imageName_imageTag_pkgMgrFilePath_pkgMgrName", ProgramPathUtils.getCodeLocationName(null, "imageName", "imageTag",  "pkgMgrFilePath",  "pkgMgrName"))
        }
    }
}