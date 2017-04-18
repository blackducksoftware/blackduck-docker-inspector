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
package com.blackducksoftware.integration.hub.docker;

import static org.junit.Assert.*

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

class RuntimeOsTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void test() {
        OsMapper osMapper = new OsMapper()
        assertEquals(OperatingSystemEnum.UBUNTU, osMapper.getRuntimeOsForTargetImageOs(OperatingSystemEnum.CENTOS))
        assertEquals(OperatingSystemEnum.CENTOS, osMapper.getRuntimeOsForTargetImageOs(OperatingSystemEnum.UBUNTU))
    }
}
