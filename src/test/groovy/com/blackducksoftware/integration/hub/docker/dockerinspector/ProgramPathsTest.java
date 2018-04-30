package com.blackducksoftware.integration.hub.docker.dockerinspector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

import com.blackducksoftware.integration.hub.docker.dockerinspector.config.Config;
import com.blackducksoftware.integration.hub.docker.dockerinspector.config.DockerInspectorOption;
import com.blackducksoftware.integration.hub.docker.dockerinspector.config.ProgramPaths;
import com.blackducksoftware.integration.hub.docker.imageinspector.TestUtils;
import com.blackducksoftware.integration.hub.imageinspector.name.Names;

@RunWith(SpringRunner.class)
public class ProgramPathsTest {

    @InjectMocks
    private ProgramPaths programPaths;

    @Mock
    private Config config;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testReleasedVersion() throws IllegalArgumentException, IllegalAccessException, IOException {
        doTest("hub-docker-1.0.0.jar", true);
    }

    @Test
    public void testSnapshotVersion() throws IllegalArgumentException, IllegalAccessException, IOException {
        doTest("hub-docker-0.0.1-SNAPSHOT.jar", false);
    }

    private void doTest(final String jarFileName, final boolean prefixCodeLocationName) throws IllegalArgumentException, IllegalAccessException, IOException {
        String prefix = "";
        if (prefixCodeLocationName) {
            prefix = "xyz";
        }

        final List<DockerInspectorOption> configOptions = new ArrayList<>();
        configOptions.add(new DockerInspectorOption("hub.url", "hubUrl", "testHubUrl", "Hub URL", String.class, "", "public", false));
        BDDMockito.given(config.getPublicConfigOptions()).willReturn(configOptions);
        BDDMockito.given(config.isOnHost()).willReturn(true);
        BDDMockito.given(config.isUploadBdio()).willReturn(true);
        BDDMockito.given(config.getLinuxDistro()).willReturn("");
        BDDMockito.given(config.getDockerTar()).willReturn("");
        BDDMockito.given(config.getDockerImage()).willReturn("");
        BDDMockito.given(config.getDockerImageId()).willReturn("");
        BDDMockito.given(config.getDockerImageRepo()).willReturn("");
        BDDMockito.given(config.getDockerImageTag()).willReturn("");
        BDDMockito.given(config.getHubUrl()).willReturn("");
        BDDMockito.given(config.getHubCodelocationPrefix()).willReturn(prefix);

        final File installDir = TestUtils.createTempDirectory();
        String installDirPath = installDir.getAbsolutePath();
        if (!installDirPath.endsWith("/")) {
            installDirPath = String.format("%s/", installDirPath);
        }
        BDDMockito.given(config.getWorkingDirPath()).willReturn(installDirPath);

        final File jarFile = new File(installDir, "hub-docker-1.0.0.jar");
        jarFile.createNewFile();

        programPaths.init(); // TODO why do I need this?
        assertTrue(programPaths.getHubDockerConfigDirPathHost().startsWith(String.format("%sconfig_", installDirPath)));
        assertTrue(programPaths.getHubDockerConfigFilePathHost().startsWith(String.format("%sconfig_", installDirPath)));
        assertTrue(programPaths.getHubDockerConfigFilePathHost().endsWith(String.format("/application.properties")));
        assertTrue(programPaths.getHubDockerTargetDirPathHost().startsWith(String.format("%starget_", installDirPath)));
        assertEquals(String.format("%s", installDirPath), programPaths.getHubDockerPgmDirPathHost());

        if (prefixCodeLocationName) {
            assertEquals(String.format("%s_imageName_imageTag_pkgMgrFilePath_pkgMgrName", prefix), Names.getCodeLocationName(prefix, "imageName", "imageTag", "pkgMgrFilePath", "pkgMgrName"));
        } else {
            assertEquals("imageName_imageTag_pkgMgrFilePath_pkgMgrName", Names.getCodeLocationName(null, "imageName", "imageTag", "pkgMgrFilePath", "pkgMgrName"));
        }
    }
}
