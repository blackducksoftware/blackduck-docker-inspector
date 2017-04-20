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

import com.blackducksoftware.integration.hub.docker.image.DockerImages

class RuntimeOsTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void test() {
        DockerImages osMapper = new DockerImages()
        assertEquals("blackduck/hub-docker/centos", osMapper.getDockerImageName(OperatingSystemEnum.CENTOS))
        assertEquals("1.0", osMapper.getDockerImageVersion(OperatingSystemEnum.CENTOS))
        assertEquals(OperatingSystemEnum.CENTOS, osMapper.getDockerImageOs(OperatingSystemEnum.CENTOS))

        assertEquals("blackduck/hub-docker/ubuntu_16_04", osMapper.getDockerImageName(OperatingSystemEnum.UBUNTU))
        assertEquals("1.0", osMapper.getDockerImageVersion(OperatingSystemEnum.UBUNTU))
        assertEquals(OperatingSystemEnum.UBUNTU, osMapper.getDockerImageOs(OperatingSystemEnum.UBUNTU))
    }
}
