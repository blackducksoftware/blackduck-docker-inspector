package com.blackducksoftware.integration.hub.docker.extractor;

import static org.junit.Assert.*

import com.blackducksoftware.integration.hub.bdio.simple.BdioWriter
import com.blackducksoftware.integration.hub.docker.mock.ExecutorMock
import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum
import com.google.gson.Gson
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.apache.commons.io.FileUtils

class DpkgExtractorTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
    void testDpkgFile1() {
        testDpkgExtraction('ubuntu_dpkg_output_1.txt','testDpkgBdio1.jsonld')
    }

    void testDpkgExtraction(String resourceName, String outputFileName){
        URL url = this.getClass().getResource("/$resourceName")
        File resourceFile = new File(URLDecoder.decode(url.getFile(), 'UTF-8'))

        DpkgExtractor extractor = new DpkgExtractor()
        ExecutorMock executor = new ExecutorMock(resourceFile)
        extractor.executor = executor
        def forges = [
            OperatingSystemEnum.UBUNTU.forge
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
        extractionDetails.operatingSystem = OperatingSystemEnum.UBUNTU
        extractionDetails.architecture = 'x86'
        extractor.extract(writer, extractionDetails, "CodeLocationName", "Test", "1")
        writer.close()
		
		File file1 = new File("src/test/resources/testDpkgBdio1.jsonld");
		File file2 = new File("test/testDpkgBdio1.jsonld");
		boolean isTwoEqual = FileUtils.contentEquals(file1, file2);
    }

}
