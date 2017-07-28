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
		test("ubuntu", "17.04", "var_lib_dpkg")
	}

	@Test
	public void testAlpine() {
		test("alpine", "3.6", "lib_apk")
	}

	@Test
	public void testCentos() {
		test("centos", "7.3.1611", "var_lib_rpm")
	}

	@Test
	public void testHubWebapp() {
		test("blackducksoftware/hub-webapp", "4.0.0", "lib_apk")
	}

	@Test
	public void testHubZookeeper() {
		test("blackducksoftware/hub-zookeeper", "4.0.0", "lib_apk")
	}

	private void test(String image, String tag, String pkgMgrPathString) {
		println "Running end to end test on ${image}:${tag}"
		String imageUnderscored = image.replace('/', '_')
		File expectedBdio = new File("src/test/resources/bdio/${imageUnderscored}_${pkgMgrPathString}_${imageUnderscored}_${tag}_bdio.jsonld")
		assertTrue(expectedBdio.exists())
		File actualBdio = new File("test/output/${imageUnderscored}_${pkgMgrPathString}_${imageUnderscored}_${tag}_bdio.jsonld")
		Files.deleteIfExists(actualBdio.toPath())
		assertFalse(actualBdio.exists())
		ProcessBuilder pb =
				new ProcessBuilder("build/hub-docker-inspector.sh",
				"--logging.level.com.blackducksoftware=INFO",
				"--dry.run=true",
				"--bdio.output.path=test/output",
				"--dev.mode=true",
				"${image}:${tag}");
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

		boolean outputBdioIsGood = TestUtils.contentEquals(expectedBdio, actualBdio, exceptLinesContainingThese)
		assertTrue(outputBdioIsGood)
	}

}
