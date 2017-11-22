package com.blackducksoftware.integration.hub.docker.config.formatter

import static org.junit.Assert.*

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import com.blackducksoftware.integration.hub.docker.config.Config

class UsageFormatterTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void test() {
        final UsageFormatter helpPrinter = new UsageFormatter();
        helpPrinter.config = new Config();
        helpPrinter.config.testPropPublicString = "test prop public string value";
        List<String> usageStrings = helpPrinter.getStringList();
        for (String line : usageStrings) {
            println line
        }
    }
}
