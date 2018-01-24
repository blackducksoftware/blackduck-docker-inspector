package com.blackducksoftware.integration.hub.docker.dockerinspector.config

import static org.junit.Assert.*

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test

class ConfigTest {

    private static final String STRING_OPTION_VALUE = "This is a test value"

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    // TODO delete this?
    @Ignore
    @Test
    public void test() {
        Config config = new Config();
        config.init();

        config.setHubCodelocationPrefix("testPrefix")
        config.setWorkingDirPath("testWorkingDirPath")
        config.setJarPath("testJarPath")

        assertNull(config.get("invalid"))
        assertEquals("testPrefix", config.getHubCodelocationPrefix());
        assertEquals("testWorkingDirPath", config.getWorkingDirPath())
        assertEquals("testJarPath", config.getJarPath())
    }
}
