package com.blackducksoftware.integration.hub.docker.imageinspector.restclient;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.docker.dockerinspector.restclient.BdioFilename;

public class OutputFilenameTest {

    @Test
    public void testAlpine() throws IntegrationException {
        final BdioFilename outputFilename = new BdioFilename("alpine_latest_lib_apk_APK", "alpine", "latest", "alpine");
        assertEquals("alpine_lib_apk_alpine_latest_bdio.jsonld", outputFilename.getBdioFilename());
    }

    @Test
    public void testCentos() throws IntegrationException {
        final BdioFilename outputFilename = new BdioFilename("blackducksoftware_centos_minus_vim_plus_bacula_1.0_var_lib_rpm_RPM", "blackducksoftware_centos_minus_vim_plus_bacula", "1.0", "centos");
        assertEquals("blackducksoftware_centos_minus_vim_plus_bacula_var_lib_rpm_blackducksoftware_centos_minus_vim_plus_bacula_1.0_bdio.jsonld", outputFilename.getBdioFilename());
    }

    @Test
    public void testUbuntu() throws IntegrationException {
        final BdioFilename outputFilename = new BdioFilename("ubuntu_latest_var_lib_dpkg_DPKG", "ubuntu", "latest", "ubuntu");
        assertEquals("ubuntu_var_lib_dpkg_ubuntu_latest_bdio.jsonld", outputFilename.getBdioFilename());
    }
}
