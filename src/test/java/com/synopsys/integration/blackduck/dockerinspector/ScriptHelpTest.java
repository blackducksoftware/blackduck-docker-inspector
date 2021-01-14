package com.synopsys.integration.blackduck.dockerinspector;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;
import com.synopsys.integration.exception.IntegrationException;

@Tag("integration")
public class ScriptHelpTest {
    private static CommandCreator commandCreator;

    @BeforeAll
    public static void setup() throws IOException {
        Random random = new Random();
        ProgramVersion programVersion = new ProgramVersion();
        programVersion.init();
        commandCreator = new CommandCreator(random, programVersion, null);
    }

    @Test
    public void testNoTopic() throws IOException, IntegrationException, InterruptedException {
        List<String> cmd = commandCreator.createSimpleDockerInspectorScriptCmd(Arrays.asList("-h"));
        String stdOut = TestUtils.execCmd(String.join(" ", cmd), 240000L, true, null);
        assertTrue(stdOut.contains("Black Duck Docker Inspector inspects Docker images to discover"));
    }

    @Test
    public void testTopic() throws IOException, IntegrationException, InterruptedException {
        ProgramVersion programVersion = new ProgramVersion();
        programVersion.init();
        List<String> cmd = commandCreator.createSimpleDockerInspectorScriptCmd(Arrays.asList("-h", "running"));
        String stdOut = TestUtils.execCmd(String.join(" ", cmd), 240000L, true, null);
        assertTrue(stdOut.contains("Running the latest version"));
        assertTrue(stdOut.contains("Running a specific version"));
    }
}
