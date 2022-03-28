package com.synopsys.integration.blackduck.dockerinspector.httpclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.blackduck.dockerinspector.output.BdioFilename;

class BdioFilenameTest {

    @BeforeAll
    public static void setUpBeforeClass() {
    }

    @AfterAll
    public static void tearDownAfterClass() {
    }

    @Test
    public void testAlpine() {
        BdioFilename bdioFilename = new BdioFilename("alpine_3.6_APK");
        assertEquals("alpine_3.6_APK_bdio.jsonld", bdioFilename.getBdioFilename());
    }
}
