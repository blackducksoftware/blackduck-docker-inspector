package com.blackducksoftware.integration.hub.docker;


import static org.junit.Assert.*

import java.lang.ProcessBuilder.Redirect
import java.nio.file.Files
import java.util.concurrent.TimeUnit

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

class EndToEndTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testUbuntu() {
        testImage("ubuntu", "17.04", "var_lib_dpkg")
    }

    @Test
    public void testAlpine() {
        testImage("alpine", "3.6", "lib_apk")
    }

    @Test
    public void testCentos() {
        testImage("centos", "7.3.1611", "var_lib_rpm")
    }

    @Test
    public void testHubWebapp() {
        testImage("blackducksoftware/hub-webapp", "4.0.0", "lib_apk")
    }

    @Test
    public void testHubZookeeper() {
        testImage("blackducksoftware/hub-zookeeper", "4.0.0", "lib_apk")
    }

    @Test
    public void testTomcat() {
        testImage("tomcat", "6.0.53-jre7", "var_lib_dpkg")
    }

    @Test
    public void testRhel() {
        testImage("dnplus/rhel", "6.5", "var_lib_rpm")
    }

    @Test
    public void testWhiteout() {
        testTar("blackducksoftware", "whiteouttest", "1.0", "var_lib_dpkg")
    }

    private void testImage(String image, String tag, String pkgMgrPathString) {
        String inspectTarget = "${image}:${tag}"
        String imageUnderscored = image.replace('/', '_')
        test(imageUnderscored, pkgMgrPathString, tag, inspectTarget)
    }

    private void testTar(String repo, String image, String tag, String pkgMgrPathString) {
        String inspectTarget = "build/images/test/${image}.tar"
        String imageUnderscored = "${repo}_${image}"
        test(imageUnderscored, pkgMgrPathString, tag, inspectTarget)
    }

    private void test(String imageUnderscored, String pkgMgrPathString, String tag, String inspectTarget) {
        File expectedBdio = new File("src/test/resources/bdio/${imageUnderscored}_${pkgMgrPathString}_${imageUnderscored}_${tag}_bdio.jsonld")
        assertTrue(expectedBdio.exists())
        File actualBdio = new File("test/output/${imageUnderscored}_${pkgMgrPathString}_${imageUnderscored}_${tag}_bdio.jsonld")
        Files.deleteIfExists(actualBdio.toPath())
        assertFalse(actualBdio.exists())

        println "Running end to end test on ${inspectTarget}"
        ProcessBuilder pb =
                new ProcessBuilder("build/hub-docker-inspector.sh",
                "--logging.level.com.blackducksoftware=TRACE",
                "--dry.run=true",
                "--bdio.output.path=test/output",
                "--dev.mode=true",
                inspectTarget);
        Map<String, String> env = pb.environment();
        env.put("PATH", "/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin");
        pb.redirectErrorStream(true);
        pb.redirectOutput(Redirect.INHERIT);
        Process p = pb.start();
        boolean finished = p.waitFor(240, TimeUnit.SECONDS)
        assertTrue(finished)
        println "hub-docker-inspector done; verifying results..."
        assertTrue(actualBdio.exists())
        List<String> exceptLinesContainingThese = new ArrayList<>()
        exceptLinesContainingThese.add("\"@id\":")

        boolean outputBdioMatches = TestUtils.contentEquals(expectedBdio, actualBdio, exceptLinesContainingThese)
        assertTrue(outputBdioMatches)
    }
}
