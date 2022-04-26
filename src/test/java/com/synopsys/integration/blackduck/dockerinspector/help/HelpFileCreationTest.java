package com.synopsys.integration.blackduck.dockerinspector.help;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@Tag("integration")
public class HelpFileCreationTest {

    @BeforeAll
    public static void setUp() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.INFO);
        Logger integrationLogger = (Logger) LoggerFactory.getLogger("com.synopsys.integration");
        integrationLogger.setLevel(Level.DEBUG);
    }

    @Test
    public void test() throws IOException {
        ProgramVersion programVersion = new ProgramVersion();
        programVersion.init();
        File helpFile = new File("docs/generated/overview.md");
        assertTrue(helpFile.exists());
    }
}
