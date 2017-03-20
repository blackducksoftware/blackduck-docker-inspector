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
package com.blackducksoftware.integration.hub.linux.extractor

import static org.junit.Assert.*

import org.junit.Test

import com.blackducksoftware.integration.hub.linux.BdioComponentDetails
import com.blackducksoftware.integration.hub.linux.OperatingSystemEnum

class YumExtractorTest {
    @Test
    public void extractYumComponentsFile1(){
        extractYumComponentsFromFile('centos_yum_output_1.txt', 580,'python-backports-ssl_match_hostname','3.4.0.2-4.el7','noarch')
    }

    @Test
    public void extractYumComponentsFile2(){
        extractYumComponentsFromFile('centos_yum_output_2.txt', 631,'redhat-lsb-submod-multimedia','4.1-27.el7.centos.1','x86_64')
    }

    @Test
    public void extractYumComponentsFile3(){
        extractYumComponentsFromFile('centos_yum_output_3.txt', 1255,'at-spi-python','1.28.1-2.el6.centos','x86_64')
    }

    public void extractYumComponentsFromFile(String fileName, int size, String name, String version, String arch){
        URL url = this.getClass().getResource("/$fileName")
        File file = new File(URLDecoder.decode(url.getFile(), 'UTF-8'))

        YumExtractor extractor = new YumExtractor()
        List<BdioComponentDetails> bdioEntries =  extractor.extractComponents(OperatingSystemEnum.UBUNTU , file)

        // assertEquals(size, bdioEntries.size())
        boolean foundTargetEntry = false
        int validEntryCount = 0
        for (final BdioComponentDetails bdioEntry : bdioEntries) {
            if (bdioEntry != null) {
                validEntryCount++
                // println(bdioEntry.getExternalIdentifier())
                def match = String.join("/",name,version,arch)
                if (match.contentEquals(bdioEntry.externalIdentifier.externalId)) {
                    foundTargetEntry = true
                    assertEquals(name, bdioEntry.name)
                    assertEquals(version, bdioEntry.version)
                }
            }
        }
        assertEquals(validEntryCount, bdioEntries.size())
        assertTrue(foundTargetEntry)
        println(bdioEntries.size())
    }
}
