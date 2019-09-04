package com.synopsys.integration.blackduck.dockerinspector.help;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.synopsys.integration.exception.IntegrationException;

public class HelpReaderTest {

    @Test
    public void test() throws IntegrationException {
        final HelpReader helpReader = new HelpReader();

        final String helpFileContents = helpReader.getStringFromHelpFile("overview");

        assertTrue(helpFileContents.contains("The current version of Black Duck. Visit [this page](https://github.com/blackducksoftware/hub/releases) to determine the current version."));
    }
}
