package com.synopsys.integration.blackduck.dockerinspector.help;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;
import com.synopsys.integration.exception.IntegrationException;

@ExtendWith(SpringExtension.class)
public class HelpReaderTest {

    public static final String TEST_PROGRAM_VERSION = "11.22.33";

    @Mock
    private ProgramVersion programVersion;

    @Mock
    private Config config;

    @InjectMocks
    private HelpReader helpReader;

    @Test
    public void test() throws IntegrationException {
        Mockito.when(programVersion.getProgramVersion()).thenReturn(TEST_PROGRAM_VERSION);

        String helpFileContents = helpReader.getVariableSubstitutedTextFromHelpFile("overview");

        assertTrue(helpFileContents.contains("Black Duck Docker Inspector is invoked by Synopsys Detect on a Docker image"));
        assertTrue(helpFileContents.contains(TEST_PROGRAM_VERSION));
    }

    @Test
    public void testReadFromGivenDir() throws IntegrationException {
        Mockito.when(programVersion.getProgramVersion()).thenReturn(TEST_PROGRAM_VERSION);
        Mockito.when(config.getHelpInputFilePath()).thenReturn("src/test/resources/help/content");

        String helpFileContents = helpReader.getVariableSubstitutedTextFromHelpFile("test");

        assertTrue(helpFileContents.contains("This is some test help content."));
    }
}
