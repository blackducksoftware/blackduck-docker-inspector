package com.blackducksoftware.integration.hub.docker.imageinspector.restclient;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.docker.dockerinspector.restclient.BdioFilename;

public class BdioFilenameTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testAlpine() throws IntegrationException {
        final BdioFilename bdioFilename = new BdioFilename("alpine_3.6_lib_apk_APK", "alpine", "3.6", "alpine");
        assertEquals("alpine_lib_apk_alpine_3.6_bdio.jsonld", bdioFilename.getBdioFilename());
    }

    @Test
    public void testEmpty() throws IntegrationException {
        final BdioFilename bdioFilename = new BdioFilename("tbd", "busybox", "1.0", "unknown");
        assertEquals("busybox_noPkgMgr_busybox_1.0_bdio.jsonld", bdioFilename.getBdioFilename());
    }
}
