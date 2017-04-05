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

import org.junit.Test

import com.blackducksoftware.integration.hub.bdio.simple.BdioWriter
import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.mock.AptExecutorMock
import com.google.gson.Gson

class AptExtractorTest {

    @Test
    void testAptFile1() {
        testAptExtraction('ubuntu_apt_package_list_1.txt','testAptBdio1.jsonld')
    }

    @Test
    void testAptFile2() {
        testAptExtraction('ubuntu_apt_package_list_2.txt','testAptBdio2.jsonld')
    }

    @Test
    void testAptFile3() {
        testAptExtraction('ubuntu_apt_package_list_3.txt','testAptBdio3.jsonld')
    }

    void testAptExtraction(String resourceName, String outputFileName){
        URL url = this.getClass().getResource("/$resourceName")
        File resourceFile = new File(URLDecoder.decode(url.getFile(), 'UTF-8'))

        AptExtractor extractor = new AptExtractor()
        AptExecutorMock executor = new AptExecutorMock(resourceFile)
        extractor.executor = executor
        extractor.init()
        File outputFile = new File("test")
        outputFile = new File(outputFile, outputFileName)
        if(outputFile.exists()){
            outputFile.delete()
        }
        outputFile.getParentFile().mkdirs()
        BdioWriter writer = new BdioWriter(new Gson(), new FileWriter(outputFile))
        extractor.extract(writer, OperatingSystemEnum.UBUNTU, "Test", "1")
        writer.close()
    }
}
