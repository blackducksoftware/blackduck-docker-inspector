package com.blackducksoftware.integration.hub.docker.executor;

import static org.junit.Assert.*

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

class ApkExecutorTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
		ApkExecutor executor = new ApkExecutor()
		executor.init()
		assertEquals("apk info testPackageName -a", executor.getPackageInfoCommand("testPackageName"))
	}

}
