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

class DpkgExtractorTest {
    @Test
    public void extractDpkgComponentsFile1(){
        extractDpkgComponentsFromFile('ubuntu_dpkg_output_1.txt',745,'linux-headers-3.13.0-107-generic','3.13.0-107.154','amd64')
    }

    @Test
    public void extractDpkgComponentsFile2(){
        extractDpkgComponentsFromFile('ubuntu_dpkg_output_2.txt',98,'sed','4.2.2-7','amd64')
    }


    public void extractDpkgComponentsFromFile(String fileName, int size, String name, String version, String arch){
        URL url = this.getClass().getResource("/$fileName")
        File file = new File(URLDecoder.decode(url.getFile(), 'UTF-8'))

        DpkgExtractor extractor = new DpkgExtractor()
        List<BdioComponentDetails> bdioEntries =  extractor.extractComponents(OperatingSystemEnum.DEBIAN , file)

        assertEquals(size, bdioEntries.size())
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
