package com.blackducksoftware.integration.hub.docker

import static org.junit.Assert.*

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import com.blackducksoftware.integration.hub.docker.config.Config

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
        config.testPropPrivate = new Boolean(true);
        config.testPropPublicBoolean = new Boolean(true);
        config.testPropPublicString = STRING_OPTION_VALUE;

        String invalidKeyValue = config.get("invalid");
        assertNull(invalidKeyValue)
        String privateBooleanValue = config.get("test.prop.private");
        assertEquals("true", privateBooleanValue);
        String publicBooleanValue = config.get("test.prop.public.boolean");
        assertEquals("true", publicBooleanValue);
        String publicStringValue = config.get("test.prop.public.string");
        assertEquals(STRING_OPTION_VALUE, publicStringValue);

        println "Values: ${privateBooleanValue}, ${publicBooleanValue}, ${publicStringValue}"
    }
}
