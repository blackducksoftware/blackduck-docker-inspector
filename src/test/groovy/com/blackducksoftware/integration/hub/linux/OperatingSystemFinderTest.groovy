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
package com.blackducksoftware.integration.hub.linux

import static org.junit.Assert.assertEquals

import org.junit.Test

class OperatingSystemFinderTest {

    String getInputFilePath(String fileName) {
        URL url = this.getClass().getResource("/osdetection/$fileName")
        File file = new File(URLDecoder.decode(url.getFile(), 'UTF-8'))

        file.getCanonicalPath()
    }

    OperatingSystemFinder createMock(String filePath, String prefixMatch, String delimeter) {
        def finder = new OperatingSystemFinder();
        finder.commandCheckList = []
        def cmdObject = [command: "cat $filePath", prefixMatch: "$prefixMatch", delimeter: "$delimeter"]
        finder.commandCheckList.add(cmdObject)

        finder
    }

    @Test
    void testCentosLsbReleaseCommand() {
        String filePath = getInputFilePath('centos_lsb_release.txt');
        OperatingSystemFinder finder = createMock(filePath,'Distributor ID:',':')
        OperatingSystemEnum os = finder.determineOperatingSystem()
        assertEquals(OperatingSystemEnum.CENTOS, os)
    }

    @Test
    void testUbuntuLsbReleaseCommand() {
        String filePath = getInputFilePath('ubuntu_lsb_release.txt');
        OperatingSystemFinder finder = createMock(filePath,'Distributor ID:',':')
        OperatingSystemEnum os = finder.determineOperatingSystem()
        assertEquals(OperatingSystemEnum.UBUNTU, os)
    }

    @Test
    void testCentosLsbReleaseFile() {
        String filePath = getInputFilePath('centos_lsb_release_file.txt');
        OperatingSystemFinder finder = createMock(filePath,'DISTRIB_ID=','=')
        OperatingSystemEnum os = finder.determineOperatingSystem()
        assertEquals(OperatingSystemEnum.CENTOS, os)
    }

    @Test
    void testUbuntuLsbReleaseFile() {
        String filePath = getInputFilePath('ubuntu_lsb_release_file.txt');
        OperatingSystemFinder finder = createMock(filePath,'DISTRIB_ID=','=')
        OperatingSystemEnum os = finder.determineOperatingSystem()
        assertEquals(OperatingSystemEnum.UBUNTU, os)
    }

    @Test
    void testCentosOsReleaseFile() {
        String filePath = getInputFilePath('centos_os_release_file.txt');
        OperatingSystemFinder finder = createMock(filePath,'ID=','=')
        OperatingSystemEnum os = finder.determineOperatingSystem()
        assertEquals(OperatingSystemEnum.CENTOS, os)
    }

    @Test
    void testUbuntuOsReleaseFile() {
        String filePath = getInputFilePath('ubuntu_os_release_file.txt');
        OperatingSystemFinder finder = createMock(filePath,'ID=','=')
        OperatingSystemEnum os = finder.determineOperatingSystem()
        assertEquals(OperatingSystemEnum.UBUNTU, os)
    }
}
