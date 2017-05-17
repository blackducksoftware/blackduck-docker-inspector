package com.blackducksoftware.integration.hub.docker.executor;

import static org.junit.Assert.*

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

class DpkgExecutorTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
		DpkgExecutor executor = new DpkgExecutor()
		executor.init()
		assertEquals("dpkg -s testPackageName", executor.getPackageInfoCommand("testPackageName"))
	}

}
