package com.blackducksoftware.integration.hub.docker.dockerinspector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

import com.blackducksoftware.integration.hub.docker.dockerinspector.config.Config;
import com.blackducksoftware.integration.hub.docker.dockerinspector.config.ProgramPaths;

@RunWith(SpringRunner.class)
public class ProgramPathsTest {

    @InjectMocks
    private ProgramPaths programPaths;

    @Mock
    private Config config;

    @Test
    public void testReleasedVersion() throws IllegalArgumentException, IllegalAccessException, IOException {
        doTest("hub-docker-1.0.0.jar", true);
    }

    @Test
    public void testSnapshotVersion() throws IllegalArgumentException, IllegalAccessException, IOException {
        doTest("hub-docker-0.0.1-SNAPSHOT.jar", false);
    }

    private void doTest(final String jarFileName, final boolean prefixCodeLocationName) throws IllegalArgumentException, IllegalAccessException, IOException {
        final File installDir = TestUtils.createTempDirectory();
        String installDirPath = installDir.getAbsolutePath();
        if (!installDirPath.endsWith("/")) {
            installDirPath = String.format("%s/", installDirPath);
        }
        BDDMockito.given(config.getWorkingDirPath()).willReturn(installDirPath);

        programPaths.init();

        assertTrue(programPaths.getHubDockerConfigDirPathHost().startsWith(String.format("%sconfig_", installDirPath)));
        assertTrue(programPaths.getHubDockerConfigFilePathHost().startsWith(String.format("%sconfig_", installDirPath)));
        assertTrue(programPaths.getHubDockerConfigFilePathHost().endsWith(String.format("/application.properties")));
        assertTrue(programPaths.getHubDockerTargetDirPathHost().startsWith(String.format("%starget_", installDirPath)));
        assertEquals(String.format("%s", installDirPath), programPaths.getHubDockerPgmDirPathHost());
    }
}
