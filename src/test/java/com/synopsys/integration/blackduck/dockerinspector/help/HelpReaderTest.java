package com.synopsys.integration.blackduck.dockerinspector.help;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;
import com.synopsys.integration.exception.IntegrationException;

@RunWith(SpringRunner.class)
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

        final String helpFileContents = helpReader.getVariableSubstitutedTextFromHelpFile("overview");
        
        assertTrue(helpFileContents.contains("The current version of Black Duck. Visit [this page](https://github.com/blackducksoftware/hub/releases) to determine the current version."));
        assertTrue(helpFileContents.contains(TEST_PROGRAM_VERSION));
    }

    @Test
    public void testReadFromGivenDir() throws IntegrationException {
        Mockito.when(programVersion.getProgramVersion()).thenReturn(TEST_PROGRAM_VERSION);
        Mockito.when(config.getHelpInputFilePath()).thenReturn("src/test/resources/help/content");

        final String helpFileContents = helpReader.getVariableSubstitutedTextFromHelpFile("test");

        assertTrue(helpFileContents.contains("This is some test help content."));
    }
}
