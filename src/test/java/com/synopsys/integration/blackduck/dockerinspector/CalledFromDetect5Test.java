package com.synopsys.integration.blackduck.dockerinspector;

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.synopsys.integration.blackduck.dockerinspector.IntegrationTestCommon.Mode;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.test.annotation.IntegrationTest;

@Category(IntegrationTest.class)
public class CalledFromDetect5Test {
    private static ProgramVersion programVersion;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        programVersion = new ProgramVersion();
        programVersion.init();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testUbuntu() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "ubuntu:17.04", "ubuntu", "17.04", "blackduck_docker_inspector_ubuntu_17_04_ubuntu_17_04_docker_docker.jsonld", false, Mode.DETECT, "dpkg", 10, null, null);
    }

    @Test
    public void testAlpine() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "alpine:3.6", "alpine", "3.6", "blackduck_docker_inspector_alpine_3_6_alpine_3_6_docker_docker.jsonld", false, Mode.DETECT, "apk-", 5, null, null);
    }

    @Test
    public void testBusybox() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "busybox:latest", "busybox", "latest", "blackduck_docker_inspector_busybox_latest_busybox_latest_docker_docker.jsonld", false, Mode.DETECT, null, 0, null, null);
    }

    @Test
    public void testAlpineLatest() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "alpine", "alpine", "latest", "blackduck_docker_inspector_alpine_latest_alpine_docker_docker.jsonld", false, Mode.DETECT, "apk-", 5, null, null);
    }

    @Test
    public void testCentos() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "centos:7.3.1611", "centos", "7.3.1611", "blackduck_docker_inspector_centos_7_3_1611_centos_7_3_1611_docker_docker.jsonld", false, Mode.DETECT, "1:openssl-libs", 15, null, null);
    }

    @Test
    public void testBlackDuckWebapp() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "blackducksoftware/hub-webapp:4.0.0", "blackducksoftware_hub-webapp", "4.0.0",
                "blackduck_docker_inspector_blackducksoftware_hub_webapp_4_0_0_blackducksoftware_hub_webapp_4_0_0_docker_docker.jsonld", true, Mode.DETECT,
                "apk-", 5, null, null);
    }

    @Test
    public void testBlackDuckZookeeper() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "blackducksoftware/hub-zookeeper:4.0.0", "blackducksoftware_hub-zookeeper", "4.0.0",
                "blackduck_docker_inspector_blackducksoftware_hub_zookeeper_4_0_0_blackducksoftware_hub_zookeeper_4_0_0_docker_docker.jsonld", true,
                Mode.DETECT, "apk-", 5,
                null, null);
    }

    @Test
    public void testTomcat() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "tomcat:6.0.53-jre7", "tomcat", "6.0.53-jre7", "blackduck_docker_inspector_tomcat_6_0_53_jre7_tomcat_6_0_53_jre7_docker_docker.jsonld", false, Mode.DETECT, "dpkg", 5, null, null);
    }

    @Test
    public void testRhel() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "dnplus/rhel:6.5", "dnplus_rhel", "6.5", "blackduck_docker_inspector_dnplus_rhel_6_5_dnplus_rhel_6_5_docker_docker.jsonld", false, Mode.DETECT, "rpm", 10, null, null);
    }

    @Test
    public void testNonLinux() throws IOException, InterruptedException, IntegrationException {
        final String repo = "osless";
        final String tag = "1.0";
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("osless.tar");
        IntegrationTestCommon.testTar(programVersion, "src/test/resources/osless.tar", "blackduck_docker_inspector_osless_1_0_osless_tar_docker_docker.jsonld", repo, tag, true, Mode.DETECT, null, true,
                outputContainerFileSystemFile, null);
    }

    @Test
    public void testWhiteout() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/whiteouttest";
        final String tag = "1.0";
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("whiteouttest.tar");
        IntegrationTestCommon.testTar(programVersion, "build/images/test/whiteouttest.tar", "blackduck_docker_inspector_blackducksoftware_whiteouttest_1_0_whiteouttest_tar_docker_docker.jsonld", repo, tag, true, Mode.DETECT, null, true,
                outputContainerFileSystemFile, null);
    }

    @Test
    public void testAggregateTarfileImageOne() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/whiteouttest";
        final String tag = "1.0";
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("aggregated.tar");
        IntegrationTestCommon.testTar(programVersion, "build/images/test/aggregated.tar", "blackduck_docker_inspector_blackducksoftware_whiteouttest_1_0_aggregated_tar_docker_docker.jsonld", repo, tag, true, Mode.DETECT, null, true,
                outputContainerFileSystemFile, null);
    }

    @Test
    public void testAggregateTarfileImageTwo() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/centos_minus_vim_plus_bacula";
        final String tag = "1.0";
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("aggregated.tar");
        IntegrationTestCommon.testTar(programVersion, "build/images/test/aggregated.tar", "blackduck_docker_inspector_blackducksoftware_centos_minus_vim_plus_bacula_1_0_aggregated_tar_docker_docker.jsonld", repo, tag, true, Mode.DETECT,
                null, true,
                outputContainerFileSystemFile, null);
    }

    @Test
    public void testAlpineLatestTarRepoTagSpecified() throws IOException, InterruptedException, IntegrationException {
        final String repo = "alpine";
        final String tag = "latest";
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("alpine.tar");
        IntegrationTestCommon.testTar(programVersion, "build/images/test/alpine.tar", "blackduck_docker_inspector_alpine_latest_alpine_tar_docker_docker.jsonld", repo, tag, false, Mode.DETECT, null, true, outputContainerFileSystemFile,
                null);
    }

    @Test
    public void testAlpineLatestTarRepoTagNotSpecified() throws IOException, InterruptedException, IntegrationException {
        final String repo = "alpine";
        final String tag = "latest";
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("alpine.tar");
        IntegrationTestCommon.testTar(programVersion, "build/images/test/alpine.tar", "blackduck_docker_inspector_alpine_latest_alpine_tar_docker_docker.jsonld", repo, tag, false, Mode.DETECT, null, true, outputContainerFileSystemFile,
                null);
    }
}
