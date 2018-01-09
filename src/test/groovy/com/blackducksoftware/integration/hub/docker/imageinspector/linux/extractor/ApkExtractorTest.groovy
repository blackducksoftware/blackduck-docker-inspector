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
package com.blackducksoftware.integration.hub.docker.imageinspector.linux.extractor


import static org.junit.Assert.*

import org.junit.Test

import com.blackducksoftware.integration.hub.bdio.BdioWriter
import com.blackducksoftware.integration.hub.docker.TestUtils
import com.blackducksoftware.integration.hub.docker.imageinspector.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.imageinspector.PackageManagerEnum
import com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.ImagePkgMgr
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.executor.ExecutorMock
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.extractor.ApkExtractor
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.extractor.ExtractionDetails
import com.google.gson.Gson

class ApkExtractorTest {

    @Test
    void testApkFile1() {
        testApkExtraction('alpine_apk_output_1.txt', 'testApkBdio1.jsonld')
    }

    void testApkExtraction(String resourceName, String bdioOutputFileName) {
        URL url = this.getClass().getResource("/$resourceName")
        File resourceFile = new File(URLDecoder.decode(url.getFile(), 'UTF-8'))

        ApkExtractor extractor = new ApkExtractor()
        ExecutorMock executor = new ExecutorMock(resourceFile)
        def forges = [
            OperatingSystemEnum.ALPINE.forge
        ]
        extractor.initValues(PackageManagerEnum.APK, executor, forges)

        File bdioOutputFile = new File("test")
        bdioOutputFile = new File(bdioOutputFile, bdioOutputFileName)
        if(bdioOutputFile.exists()){
            bdioOutputFile.delete()
        }
        bdioOutputFile.getParentFile().mkdirs()
        BdioWriter bdioWriter = new BdioWriter(new Gson(), new FileWriter(bdioOutputFile))

        ExtractionDetails extractionDetails = new ExtractionDetails(OperatingSystemEnum.ALPINE, 'x86')
        final ImagePkgMgr imagePkgMgr = new ImagePkgMgr(new File("nonexistentdir"), PackageManagerEnum.APK)
        extractor.extract("root", "1.0", imagePkgMgr, bdioWriter, extractionDetails, "CodeLocationName", "Test", "1")
        bdioWriter.close()

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