package com.synopsys.integration.blackduck.dockerinspector.httpclient;

import static org.junit.Assert.assertEquals;

import com.synopsys.integration.blackduck.dockerinspector.common.BdioFilename;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.synopsys.integration.exception.IntegrationException;

public class BdioFilenameTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testAlpine() throws IntegrationException {
        final BdioFilename bdioFilename = new BdioFilename("alpine_3.6_APK");
        assertEquals("alpine_3.6_APK_bdio.jsonld", bdioFilename.getBdioFilename());
    }
}
