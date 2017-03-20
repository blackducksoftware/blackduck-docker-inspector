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

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import com.blackducksoftware.integration.hub.linux.BdioComponentDetails
import com.blackducksoftware.integration.hub.linux.OperatingSystemEnum

class RpmExtractorTest {
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    void testExtractingRpmFile1() {
        extractingRpm("centos_rpm_output_1.txt",631,"perl-Data-Dumper","2.145-3.el7","x86_64")
    }

    @Test
    void testExtractingRpmFile2() {
        extractingRpm("centos_rpm_output_2.txt",1255,"sysvinit-tools","2.87-6.dsf.el6","x86_64")
    }

    @Test
    void testExtractingRpmFile3() {
        extractingRpm("centos_rpm_output_3.txt",580,"perl-Data-Dumper","2.145-3.el7","x86_64")
    }

    void extractingRpm(String fileName, int size, String name, String version, String arch) {
        final RpmExtractor extractor = new RpmExtractor()
        final URL url = this.getClass().getResource("/$fileName")
        final File file = new File(URLDecoder.decode(url.getFile(), "UTF-8"))

        List<BdioComponentDetails> bdioEntries = extractor.extractComponents(OperatingSystemEnum.REDHAT , file)
        assertEquals(size, bdioEntries.size())
        boolean foundTargetEntry = false
        int validEntryCount = 0
        for (final BdioComponentDetails bdioEntry : bdioEntries) {
            if (bdioEntry != null) {
                validEntryCount++
                System.out.println(bdioEntry.externalIdentifier)
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
