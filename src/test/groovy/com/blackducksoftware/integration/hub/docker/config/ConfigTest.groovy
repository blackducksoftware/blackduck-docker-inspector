package com.blackducksoftware.integration.hub.docker.config

import static org.junit.Assert.*

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

class ConfigTest {

    private static final String STRING_OPTION_VALUE = "This is a test value"

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void test() {
        Config config = new Config();
        config.onHost = new Boolean(true);
        config.dryRun = new Boolean(true);
        config.hubUrl = STRING_OPTION_VALUE;

        String invalidKeyValue = config.get("invalid");
        assertNull(invalidKeyValue)
        String privateBooleanValue = config.get("on.host");
        assertEquals("true", privateBooleanValue);
        String publicBooleanValue = config.get("dry.run");
        assertEquals("true", publicBooleanValue);
        String publicStringValue = config.get("hub.url");
        assertEquals(STRING_OPTION_VALUE, publicStringValue);

        println "Values: ${privateBooleanValue}, ${publicBooleanValue}, ${publicStringValue}"
    }
}
