package com.blackducksoftware.integration.hub.docker.v2.imageinspector.extractor;


import static org.junit.Assert.*

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import com.blackducksoftware.integration.hub.bdio.BdioWriter
import com.blackducksoftware.integration.hub.docker.v2.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.v2.PackageManagerEnum
import com.blackducksoftware.integration.hub.docker.v2.TestUtils
import com.blackducksoftware.integration.hub.docker.v2.imageinspector.executor.ExecutorMock
import com.blackducksoftware.integration.hub.docker.v2.imageinspector.imageformat.docker.ImagePkgMgr
import com.google.gson.Gson

class DpkgExtractorTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    void testDpkgFile1() {
        testDpkgExtraction('ubuntu_dpkg_output_1.txt', 'testDpkgBdio1.jsonld')
    }

    void testDpkgExtraction(String resourceName, String bdioOutputFileName) {
        URL url = this.getClass().getResource("/$resourceName")
        File resourceFile = new File(URLDecoder.decode(url.getFile(), 'UTF-8'))

        DpkgExtractor extractor = new DpkgExtractor()
        ExecutorMock executor = new ExecutorMock(resourceFile)
        def forges = [
            OperatingSystemEnum.UBUNTU.forge
        ]
        extractor.initValues(PackageManagerEnum.DPKG, executor, forges)

        File bdioOutputFile = new File("test")
        bdioOutputFile = new File(bdioOutputFile, bdioOutputFileName)
        if(bdioOutputFile.exists()){
            bdioOutputFile.delete()
        }
        bdioOutputFile.getParentFile().mkdirs()
        BdioWriter bdioWriter = new BdioWriter(new Gson(), new FileWriter(bdioOutputFile))

        ExtractionDetails extractionDetails = new ExtractionDetails(OperatingSystemEnum.UBUNTU, 'x86')
        ImagePkgMgr imagePkgMgr = new ImagePkgMgr(new File("nonexistentdir"), PackageManagerEnum.DPKG)
        extractor.extract("root", "1.0", imagePkgMgr, bdioWriter, extractionDetails, "CodeLocationName", "Test", "1")
        bdioWriter.close()


        File file1 = new File("src/test/resources/testDpkgBdio1.jsonld");
        File file2 = new File("test/testDpkgBdio1.jsonld");
        println "Comparing ${file2.getAbsolutePath()} to ${file1.getAbsolutePath()}"
        boolean filesAreEqual = TestUtils.contentEquals(file1, file2, [
            "\"@id\":",
            "\"externalSystemTypeId\":"
        ])
        assertTrue(filesAreEqual)
    }
}