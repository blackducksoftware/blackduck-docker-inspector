package com.blackducksoftware.integration.hub.docker.extractor;


import static org.junit.Assert.*

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import com.blackducksoftware.integration.hub.bdio.simple.BdioWriter
import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum
import com.blackducksoftware.integration.hub.docker.TestUtils
import com.blackducksoftware.integration.hub.docker.executor.ExecutorMock
import com.blackducksoftware.integration.hub.docker.tar.ImagePkgMgr
import com.google.gson.Gson

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
    // TODO also test dependency node output
    void testRpmExtraction(String resourceName, String outputFileName){
        URL url = this.getClass().getResource("/$resourceName")
        File resourceFile = new File(URLDecoder.decode(url.getFile(), 'UTF-8'))

        RpmExtractor extractor = new RpmExtractor()
        ExecutorMock executor = new ExecutorMock(resourceFile)
        def forges = [
            OperatingSystemEnum.CENTOS.forge
        ]
        extractor.initValues(PackageManagerEnum.RPM, executor, forges)

        File outputFile = new File("test")
        outputFile = new File(outputFile, outputFileName)
        if(outputFile.exists()){
            outputFile.delete()
        }
        outputFile.getParentFile().mkdirs()
        BdioWriter writer = new BdioWriter(new Gson(), new FileWriter(outputFile))
        ExtractionDetails extractionDetails = new ExtractionDetails(OperatingSystemEnum.CENTOS, 'x86')
        ImagePkgMgr imagePkgMgr = new ImagePkgMgr(new File("nonexistentdir"), PackageManagerEnum.RPM)
        extractor.extract(imagePkgMgr, writer, null, extractionDetails, "CodeLocationName", "Test", "1")
        writer.close()

        File file1 = new File("src/test/resources/testRpmBdio1.jsonld");
        File file2 = new File("test/testRpmBdio1.jsonld");
        println "Comparing ${file2.getAbsolutePath()} to ${file1.getAbsolutePath()}"
        boolean filesAreEqual = TestUtils.contentEquals(file1, file2, [
            "\"@id\":",
            "\"externalSystemTypeId\":"
        ])
        assertTrue(filesAreEqual)
    }
}