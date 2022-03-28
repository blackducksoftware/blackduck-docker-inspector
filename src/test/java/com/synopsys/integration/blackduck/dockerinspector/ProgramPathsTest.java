package com.synopsys.integration.blackduck.dockerinspector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.ProgramPaths;

@ExtendWith(SpringExtension.class)
public class ProgramPathsTest {
    private static Config config;
    private static ProcessId processId;
    private static ProgramPaths programPaths;
    private static String installDirPath;

    @BeforeAll
    public static void setup() throws IOException {
        config = Mockito.mock(Config.class);
        processId = Mockito.mock(ProcessId.class);
        Mockito.when(processId.addProcessIdToName(Mockito.anyString())).thenReturn("run_1");
        File installDir = TestUtils.createTempDirectory();
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

    private void doTest(String jarFileName, boolean prefixCodeLocationName) throws IllegalArgumentException, IllegalAccessException, IOException {

        assertEquals(installDirPath, programPaths.getDockerInspectorPgmDirPath());
        String runDirPath = programPaths.getDockerInspectorRunDirPath();
        assertEquals(String.format("%sconfig/", runDirPath), programPaths.getDockerInspectorConfigDirPath());
        assertEquals(String.format("%sconfig/application.properties", runDirPath), programPaths.getDockerInspectorConfigFilePath());
        assertEquals(String.format("%starget/", runDirPath), programPaths.getDockerInspectorTargetDirPath());

    }
}
