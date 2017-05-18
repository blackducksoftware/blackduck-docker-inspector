package com.blackducksoftware.integration.hub.docker.extractor;

import static org.junit.Assert.*
import com.blackducksoftware.integration.hub.docker.TestUtils

import com.blackducksoftware.integration.hub.docker.mock.ExecutorMock
import com.google.gson.Gson
import com.blackducksoftware.integration.hub.bdio.simple.BdioWriter
import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.apache.commons.io.FileUtils

class RpmExtractorTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
    void testRpmFile1() {
        testRpmExtraction('centos_rpm_output_1.txt','testRpmBdio1.jsonld')
    }

    void testRpmExtraction(String resourceName, String outputFileName){
        URL url = this.getClass().getResource("/$resourceName")
        File resourceFile = new File(URLDecoder.decode(url.getFile(), 'UTF-8'))

        RpmExtractor extractor = new RpmExtractor()
        ExecutorMock executor = new ExecutorMock(resourceFile)
        extractor.executor = executor
        def forges = [
            OperatingSystemEnum.CENTOS.forge
        ]
        extractor.initValues(PackageManagerEnum.APK, executor, forges)

        File outputFile = new File("test")
        outputFile = new File(outputFile, outputFileName)
        if(outputFile.exists()){
            outputFile.delete()
        }
        outputFile.getParentFile().mkdirs()
        BdioWriter writer = new BdioWriter(new Gson(), new FileWriter(outputFile))
        ExtractionDetails extractionDetails = new ExtractionDetails()
        extractionDetails.operatingSystem = OperatingSystemEnum.CENTOS
        extractionDetails.architecture = 'x86'
        extractor.extract(writer, extractionDetails, "CodeLocationName", "Test", "1")
        writer.close()
		
		File file1 = new File("src/test/resources/testRpmBdio1.jsonld");
		File file2 = new File("test/testRpmBdio1.jsonld");
		println "Comparing ${file2.getAbsolutePath()} to ${file1.getAbsolutePath()}"
		boolean filesAreEqual = TestUtils.contentEquals(file1, file2, ["\"@id\":", "\"externalSystemTypeId\":"])
		assertTrue(filesAreEqual)
    }
}
