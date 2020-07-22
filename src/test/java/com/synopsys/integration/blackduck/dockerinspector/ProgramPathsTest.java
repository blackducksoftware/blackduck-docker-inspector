package com.synopsys.integration.blackduck.dockerinspector;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.ProgramPaths;

@RunWith(SpringRunner.class)
public class ProgramPathsTest {
    private static Config config;
    private static ProcessId processId;
    private static ProgramPaths programPaths;
    private static String installDirPath;

    @BeforeClass
    public static void setup() throws IOException {
        config = Mockito.mock(Config.class);
        processId = Mockito.mock(ProcessId.class);
        Mockito.when(processId.addProcessIdToName(Mockito.anyString())).thenReturn("run_1");
        final File installDir = TestUtils.createTempDirectory();
        installDirPath = installDir.getAbsolutePath();
        Mockito.when(config.getWorkingDirPath()).thenReturn(installDirPath);
        Mockito.when(processId.addProcessIdToName(Mockito.anyString())).thenReturn("test");
        programPaths = new ProgramPaths(config, processId);
    }

    @Test
    public void testReleasedVersion() throws IllegalArgumentException, IllegalAccessException, IOException {
        doTest("blackduck-docker-inspector-1.0.0.jar", true);
    }

    @Test
    public void testSnapshotVersion() throws IllegalArgumentException, IllegalAccessException, IOException {
        doTest("blackduck-docker-inspector-0.0.1-SNAPSHOT.jar", false);
    }

    private void doTest(final String jarFileName, final boolean prefixCodeLocationName) throws IllegalArgumentException, IllegalAccessException, IOException {


        assertEquals(installDirPath, programPaths.getDockerInspectorPgmDirPath());
        final String runDirPath = programPaths.getDockerInspectorRunDirPath();
        assertEquals(String.format("%sconfig/", runDirPath), programPaths.getDockerInspectorConfigDirPath());
        assertEquals(String.format("%sconfig/application.properties", runDirPath), programPaths.getDockerInspectorConfigFilePath());
        assertEquals(String.format("%starget/", runDirPath), programPaths.getDockerInspectorTargetDirPath());

    }
}
