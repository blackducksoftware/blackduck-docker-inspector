package com.synopsys.integration.blackduck.dockerinspector.help;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;

@Tag("integration")
public class HelpFileCreationTest {

    @Test
    public void test() throws IOException {
        final ProgramVersion programVersion = new ProgramVersion();
        programVersion.init();
        final File helpFile = new File("docs/generated/overview.md");
        assertTrue(helpFile.exists());
    }
}
