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
    void testAptExtraction(){
        AptExtractor extractor = new AptExtractor()
        AptExecutorMock executor = new AptExecutorMock('ubuntu_apt_package_list_1.txt')
        extractor.executor = executor
        extractor.init()
        File outputFile = new File('testBdio')
        new FileOutputStream(outputFile).withStream { outputStream ->
            BdioWriter writer = new BdioWriter(new Gson(), outputStream)
            extractor.extract(writer, OperatingSystemEnum.UBUNTU, "Test", "1")
        }
    }
}
