/*
 * Copyright (C) 2017 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Black Duck Software.
 */
package com.blackducksoftware.integration.hub.docker.extractor


import static org.junit.Assert.*

import org.junit.Test

import com.blackducksoftware.integration.hub.bdio.simple.BdioWriter
import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum
import com.blackducksoftware.integration.hub.docker.TestUtils
import com.blackducksoftware.integration.hub.docker.mock.ExecutorMock
import com.google.gson.Gson

class ApkExtractorTest {

    @Test
    void testApkFile1() {
        testApkExtraction('alpine_apk_output_1.txt','testApkBdio1.jsonld')
    }

    void testApkExtraction(String resourceName, String outputFileName){
        URL url = this.getClass().getResource("/$resourceName")
        File resourceFile = new File(URLDecoder.decode(url.getFile(), 'UTF-8'))

        ApkExtractor extractor = new ApkExtractor()
        ExecutorMock executor = new ExecutorMock(resourceFile)
        extractor.executor = executor
        def forges = [
            OperatingSystemEnum.ALPINE.forge
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
        extractionDetails.operatingSystem = OperatingSystemEnum.ALPINE
        extractionDetails.architecture = 'x86'
        extractor.extract(writer, extractionDetails, "CodeLocationName", "Test", "1")
        writer.close()

        File file1 = new File("src/test/resources/testApkBdio1.jsonld");
        File file2 = new File("test/testApkBdio1.jsonld");
        println "Comparing ${file2.getAbsolutePath()} to ${file1.getAbsolutePath()}"
        boolean filesAreEqual = TestUtils.contentEquals(file1, file2, [
            "\"@id\":",
            "\"externalSystemTypeId\":"
        ])
        assertTrue(filesAreEqual)
    }
}