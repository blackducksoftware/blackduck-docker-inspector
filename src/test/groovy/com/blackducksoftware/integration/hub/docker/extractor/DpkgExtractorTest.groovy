package com.blackducksoftware.integration.hub.docker.extractor;


import static org.junit.Assert.*

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import com.blackducksoftware.integration.hub.bdio.simple.BdioWriter
import com.blackducksoftware.integration.hub.bdio.simple.model.externalid.ExternalIdFactory
import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum
import com.blackducksoftware.integration.hub.docker.TestUtils
import com.blackducksoftware.integration.hub.docker.executor.ExecutorMock
import com.blackducksoftware.integration.hub.docker.tar.ImagePkgMgr
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
        testDpkgExtraction('ubuntu_dpkg_output_1.txt', 'testDpkgBdio1.jsonld', 'testDpkgDependencies1.json')
    }

    void testDpkgExtraction(String resourceName, String bdioOutputFileName, String dependenciesOutputFileName) {
        URL url = this.getClass().getResource("/$resourceName")
        File resourceFile = new File(URLDecoder.decode(url.getFile(), 'UTF-8'))

        DpkgExtractor extractor = new DpkgExtractor()
        ExecutorMock executor = new ExecutorMock(resourceFile)
        def forges = [
            OperatingSystemEnum.UBUNTU.forge
        ]
        extractor.initValues(PackageManagerEnum.DPKG, executor, forges, new ExternalIdFactory())

        File bdioOutputFile = new File("test")
        bdioOutputFile = new File(bdioOutputFile, bdioOutputFileName)
        if(bdioOutputFile.exists()){
            bdioOutputFile.delete()
        }
        bdioOutputFile.getParentFile().mkdirs()
        BdioWriter bdioWriter = new BdioWriter(new Gson(), new FileWriter(bdioOutputFile))
        File dependenciesOutputFile = new File("test")
        dependenciesOutputFile = new File(dependenciesOutputFile, dependenciesOutputFileName)
        if(dependenciesOutputFile.exists()){
            dependenciesOutputFile.delete()
        }
        dependenciesOutputFile.getParentFile().mkdirs()
        BdioWriter dependenciesWriter = new BdioWriter(new Gson(), new FileWriter(dependenciesOutputFile))

        ExtractionDetails extractionDetails = new ExtractionDetails(OperatingSystemEnum.UBUNTU, 'x86')
        ImagePkgMgr imagePkgMgr = new ImagePkgMgr(new File("nonexistentdir"), PackageManagerEnum.DPKG)
        extractor.extract("root", "1.0", imagePkgMgr, bdioWriter, dependenciesWriter, extractionDetails, "CodeLocationName", "Test", "1")
        bdioWriter.close()
        dependenciesWriter.close()


        File file1 = new File("src/test/resources/testDpkgBdio1.jsonld");
        File file2 = new File("test/testDpkgBdio1.jsonld");
        println "Comparing ${file2.getAbsolutePath()} to ${file1.getAbsolutePath()}"
        boolean filesAreEqual = TestUtils.contentEquals(file1, file2, [
            "\"@id\":",
            "\"externalSystemTypeId\":"
        ])
        assertTrue(filesAreEqual)

        file1 = new File("src/test/resources/testDpkgDependencies1.json");
        file2 = new File("test/testDpkgDependencies1.json");
        println "Comparing ${file2.getAbsolutePath()} to ${file1.getAbsolutePath()}"
        filesAreEqual = TestUtils.contentEquals(file1, file2, [
            "\"@id\":",
            "\"externalSystemTypeId\":"
        ])
        assertTrue(filesAreEqual)
    }
}