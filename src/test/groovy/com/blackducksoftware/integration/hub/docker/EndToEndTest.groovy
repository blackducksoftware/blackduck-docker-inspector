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
	public void testAlpine() {
		test("alpine", "3.6", "lib_apk")
	}

	@Test
	public void testCentos() {
		test("centos", "7.3.1611", "var_lib_rpm")
	}
	private void test(String image, String tag, String pkgMgrPathString) {
		println "Running end to end test on ${image}:${tag}"

		File expectedBdio = new File("src/test/resources/bdio/${image}_${pkgMgrPathString}_${image}_${tag}_bdio.jsonld")
		File actualBdio = new File("test/output/${image}_${pkgMgrPathString}_${image}_${tag}_bdio.jsonld")
		Files.deleteIfExists(actualBdio.toPath())

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

		List<String> exceptLinesContainingThese = new ArrayList<>()
		exceptLinesContainingThese.add("\"@id\":")
		boolean outputBdioIsGood = TestUtils.contentEquals(expectedBdio, actualBdio, exceptLinesContainingThese)
		assertTrue(outputBdioIsGood)
	}

}
