package com.synopsys.integration.blackduck.dockerinspector;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;
import com.synopsys.integration.exception.IntegrationException;

@Tag("integration")
public class ScriptHelpTest {

    @Test
    public void testNoTopic() throws IOException, IntegrationException, InterruptedException {
        final ProgramVersion programVersion = new ProgramVersion();
        programVersion.init();
        final List<String> cmd = IntegrationTestCommon.createSimpleDockerInspectorScriptCmd(programVersion, Arrays.asList("-h"));
        final String stdOut = TestUtils.execCmd(String.join(" ", cmd), 240000L, true, null);
        assertTrue(stdOut.contains("Black Duck Docker Inspector inspects Docker images to discover"));
    }

    @Test
    public void testTopic() throws IOException, IntegrationException, InterruptedException {
        final ProgramVersion programVersion = new ProgramVersion();
        programVersion.init();
        final List<String> cmd = IntegrationTestCommon.createSimpleDockerInspectorScriptCmd(programVersion, Arrays.asList("-h", "running"));
        final String stdOut = TestUtils.execCmd(String.join(" ", cmd), 240000L, true, null);
        assertTrue(stdOut.contains("Running the latest version"));
        assertTrue(stdOut.contains("Running a specific version"));
    }
}
