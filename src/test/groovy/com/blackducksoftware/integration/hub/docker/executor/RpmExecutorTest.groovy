package com.blackducksoftware.integration.hub.docker.executor;

import static org.junit.Assert.*

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

class RpmExecutorTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
		RpmExecutor executor = new RpmExecutor()
		executor.init()
		assertEquals("rpm -qR testPackageName", executor.getPackageInfoCommand("testPackageName"))
	}

}