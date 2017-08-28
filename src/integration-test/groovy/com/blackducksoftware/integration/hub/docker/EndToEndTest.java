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
        testImage("ubuntu", "17.04", "var_lib_dpkg");
    }

    @Test
    public void testAlpine() throws IOException, InterruptedException {
        testImage("alpine", "3.6", "lib_apk");
    }

    @Test
    public void testCentos() throws IOException, InterruptedException {
        testImage("centos", "7.3.1611", "var_lib_rpm");
    }

    @Test
    public void testHubWebapp() throws IOException, InterruptedException {
        testImage("blackducksoftware/hub-webapp", "4.0.0", "lib_apk");
    }

    @Test
    public void testHubZookeeper() throws IOException, InterruptedException {
        testImage("blackducksoftware/hub-zookeeper", "4.0.0", "lib_apk");
    }

    @Test
    public void testTomcat() throws IOException, InterruptedException {
        testImage("tomcat", "6.0.53-jre7", "var_lib_dpkg");
    }

    @Test
    public void testRhel() throws IOException, InterruptedException {
        testImage("dnplus/rhel", "6.5", "var_lib_rpm");
    }

    @Test
    public void testWhiteout() throws IOException, InterruptedException {
        testTar("whiteouttest.tar", "blackducksoftware/whiteouttest", "1.0", "var_lib_dpkg", false);
    }

    @Test
    public void testAggregateTarfileImageOne() throws IOException, InterruptedException {
        testTar("aggregated.tar", "blackducksoftware/whiteouttest", "1.0", "var_lib_dpkg", true);
    }

    @Test
    public void testAggregateTarfileImageTwo() throws IOException, InterruptedException {
        testTar("aggregated.tar", "blackducksoftware/centos_minus_vim_plus_bacula", "1.0", "var_lib_rpm", true);
    }

    private void testImage(final String image, final String tag, final String pkgMgrPathString) throws IOException, InterruptedException {
        final String inspectTarget = String.format(String.format("%s:%s", image, tag));
        final String imageUnderscored = image.replace('/', '_');
        test(imageUnderscored, pkgMgrPathString, image, tag, inspectTarget, false);
    }

    private void testTar(final String tarFilename, final String image, final String tag, final String pkgMgrPathString, final boolean specifyImageTag) throws IOException, InterruptedException {
        final String inspectTarget = String.format(String.format("build/images/test/%s", tarFilename));
        final String imageUnderscored = image.replace('/', '_'); // TODO move this into test?
        test(imageUnderscored, pkgMgrPathString, image, tag, inspectTarget, specifyImageTag);
    }

    private void test(final String imageUnderscored, final String pkgMgrPathString, final String image, final String tag, final String inspectTarget, final boolean specifyImageTag) throws IOException, InterruptedException {
        final File expectedBdio = new File(String.format(String.format("src/integration-test/resources/bdio/%s_%s_%s_%s_bdio.jsonld", imageUnderscored, pkgMgrPathString, imageUnderscored, tag)));
        assertTrue(expectedBdio.exists());
        final File actualBdio = new File(String.format(String.format("test/output/%s_%s_%s_%s_bdio.jsonld", imageUnderscored, pkgMgrPathString, imageUnderscored, tag)));
        Files.deleteIfExists(actualBdio.toPath());
        assertFalse(actualBdio.exists());

        final List<String> partialCmd = Arrays.asList("build/hub-docker-inspector.sh", "--logging.level.com.blackducksoftware=INFO", "--dry.run=true", "--bdio.output.path=test/output", "--dev.mode=true");
        // Arrays.asList returns a fixed size list; need a variable sized list
        final List<String> fullCmd = new ArrayList<>();
        fullCmd.addAll(partialCmd);
        if (specifyImageTag) {
            fullCmd.add(String.format("--docker.image=%s", image));
            fullCmd.add(String.format("--docker.image.tag=%s", tag));
        }
        fullCmd.add(inspectTarget);

        System.out.println(String.format("Running end to end test on %s", inspectTarget));
        final ProcessBuilder pb = new ProcessBuilder(fullCmd);
        final Map<String, String> env = pb.environment();
        env.put("PATH", "/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin");
        pb.redirectErrorStream(true);
        pb.redirectOutput(Redirect.INHERIT);
        final Process p = pb.start();
        final boolean finished = p.waitFor(240, TimeUnit.SECONDS);
        assertTrue(finished);
        System.out.println("hub-docker-inspector done; verifying results...");
        assertTrue(actualBdio.exists());
        final List<String> exceptLinesContainingThese = new ArrayList<>();
        exceptLinesContainingThese.add("\"@id\":");

        final boolean outputBdioMatches = TestUtils.contentEquals(expectedBdio, actualBdio, exceptLinesContainingThese);
        assertTrue(outputBdioMatches);
    }
}
