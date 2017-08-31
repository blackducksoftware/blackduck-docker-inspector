package com.blackducksoftware.integration.hub.docker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class EndToEndTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testUbuntu() throws IOException, InterruptedException {
        testImage("ubuntu:17.04", "ubuntu", "17.04", "var_lib_dpkg", true);
    }

    @Test
    public void testAlpine() throws IOException, InterruptedException {
        testImage("alpine:3.6", "alpine", "3.6", "lib_apk", true);
    }

    @Test
    public void testAlpineLatest() throws IOException, InterruptedException {
        testImage("alpine", "alpine", "latest", "lib_apk", false);
    }

    @Test
    public void testCentos() throws IOException, InterruptedException {
        testImage("centos:7.3.1611", "centos", "7.3.1611", "var_lib_rpm", true);
    }

    @Test
    public void testHubWebapp() throws IOException, InterruptedException {
        testImage("blackducksoftware/hub-webapp:4.0.0", "blackducksoftware_hub-webapp", "4.0.0", "lib_apk", true);
    }

    @Test
    public void testHubZookeeper() throws IOException, InterruptedException {
        testImage("blackducksoftware/hub-zookeeper:4.0.0", "blackducksoftware_hub-zookeeper", "4.0.0", "lib_apk", true);
    }

    @Test
    public void testTomcat() throws IOException, InterruptedException {
        testImage("tomcat:6.0.53-jre7", "tomcat", "6.0.53-jre7", "var_lib_dpkg", true);
    }

    @Test
    public void testRhel() throws IOException, InterruptedException {
        testImage("dnplus/rhel:6.5", "dnplus_rhel", "6.5", "var_lib_rpm", true);
    }

    @Test
    public void testWhiteout() throws IOException, InterruptedException {
        testTar("whiteouttest.tar", "blackducksoftware_whiteouttest", "blackducksoftware/whiteouttest", "1.0", "1.0", "var_lib_dpkg", true);
    }

    @Test
    public void testAggregateTarfileImageOne() throws IOException, InterruptedException {
        testTar("aggregated.tar", "blackducksoftware_whiteouttest", "blackducksoftware/whiteouttest", "1.0", "1.0", "var_lib_dpkg", true);
    }

    @Test
    public void testAggregateTarfileImageTwo() throws IOException, InterruptedException {
        testTar("aggregated.tar", "blackducksoftware_centos_minus_vim_plus_bacula", "blackducksoftware/centos_minus_vim_plus_bacula", "1.0", "1.0", "var_lib_rpm", true);
    }

    private void testImage(final String inspectTarget, final String imageForBdioFilename, final String tagForBdioFilename, final String pkgMgrPathString, final boolean requireBdioMatch) throws IOException, InterruptedException {
        test(imageForBdioFilename, pkgMgrPathString, null, null, tagForBdioFilename, inspectTarget, requireBdioMatch);
    }

    // TODO arg order is weird
    private void testTar(final String tarFilename, final String imageForBdioFilename, final String repo, final String tag, final String tagForBdioFilename, final String pkgMgrPathString, final boolean requireBdioMatch)
            throws IOException, InterruptedException {
        final String inspectTarget = String.format(String.format("build/images/test/%s", tarFilename));
        test(imageForBdioFilename, pkgMgrPathString, repo, tag, tagForBdioFilename, inspectTarget, requireBdioMatch);
    }

    // TODO arg order is weird
    private void test(final String imageForBdioFilename, final String pkgMgrPathString, final String repo, final String tag, final String tagForBdioFilename, final String inspectTarget, final boolean requireBdioMatch)
            throws IOException, InterruptedException {

        final File expectedBdio = new File(String.format(String.format("src/integration-test/resources/bdio/%s_%s_%s_%s_bdio.jsonld", imageForBdioFilename, pkgMgrPathString, imageForBdioFilename, tagForBdioFilename)));
        if (requireBdioMatch) {
            assertTrue(expectedBdio.exists());
        }
        final File actualBdio = new File(String.format(String.format("test/output/%s_%s_%s_%s_bdio.jsonld", imageForBdioFilename, pkgMgrPathString, imageForBdioFilename, tagForBdioFilename)));
        Files.deleteIfExists(actualBdio.toPath());
        assertFalse(actualBdio.exists());

        final List<String> partialCmd = Arrays.asList("build/hub-docker-inspector.sh", "--dry.run=true", "--bdio.output.path=test/output", "--dev.mode=true");
        // Arrays.asList returns a fixed size list; need a variable sized list
        final List<String> fullCmd = new ArrayList<>();
        fullCmd.addAll(partialCmd);
        if (repo != null) {
            fullCmd.add(String.format("--docker.image.repo=%s", repo));
        }
        if (tag != null) {
            fullCmd.add(String.format("--docker.image.tag=%s", tag));
        }
        fullCmd.add("--logging.level.com.blackducksoftware=DEBUG");
        fullCmd.add(inspectTarget);

        System.out.println(String.format("Running end to end test on %s with command %s", inspectTarget, fullCmd.toString()));
        final ProcessBuilder pb = new ProcessBuilder(fullCmd);
        final Map<String, String> env = pb.environment();
        env.put("PATH", "/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin");
        pb.redirectErrorStream(true);
        pb.redirectOutput(Redirect.INHERIT);
        final Process p = pb.start();
        final boolean finished = p.waitFor(480, TimeUnit.SECONDS);
        assertTrue(finished);
        System.out.println("hub-docker-inspector done; verifying results...");
        assertTrue(actualBdio.exists());
        if (requireBdioMatch) {
            final List<String> exceptLinesContainingThese = new ArrayList<>();
            exceptLinesContainingThese.add("\"@id\":");

            final boolean outputBdioMatches = TestUtils.contentEquals(expectedBdio, actualBdio, exceptLinesContainingThese);
            assertTrue(outputBdioMatches);
        }
    }
}
