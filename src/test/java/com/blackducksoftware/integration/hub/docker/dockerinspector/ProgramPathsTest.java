package com.blackducksoftware.integration.hub.docker.dockerinspector;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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
        final String installDirPath = installDir.getAbsolutePath();
        Mockito.when(config.getWorkingDirPath()).thenReturn(installDirPath);

        programPaths.init();

        assertEquals(String.format("%s", installDirPath), programPaths.getHubDockerPgmDirPathHost());
        final String runDirPath = programPaths.getHubDockerRunDirPathHost();
        assertEquals(String.format("%sconfig/", runDirPath), programPaths.getHubDockerConfigDirPathHost());
        assertEquals(String.format("%sconfig/application.properties", runDirPath), programPaths.getHubDockerConfigFilePathHost());
        assertEquals(String.format("%starget/", runDirPath), programPaths.getHubDockerTargetDirPathHost());

    }
}
