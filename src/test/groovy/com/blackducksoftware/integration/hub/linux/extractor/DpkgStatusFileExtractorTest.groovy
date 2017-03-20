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
import com.blackducksoftware.integration.hub.linux.BdioFileWriter
import com.blackducksoftware.integration.hub.linux.OperatingSystemEnum

class DpkgStatusFileExtractorTest {
    @Test
    public void extractDpkgComponentsFromStatusFile1(){
        List<BdioComponentDetails> bdioEntries= extractDpkgComponentsFromStatusFile('status',98,'libpam-modules-bin','1.1.8-3.2ubuntu2','amd64')
        def outputFile = new File(new File('.'), "Dpkg_Status_File_bdio.jsonld")
        if(outputFile.exists()){
            outputFile.delete()
        }
        def bdioFileWriter = new BdioFileWriter()
        def project = 'DpkgStatusFile'
        def version = '1'
        new FileOutputStream(outputFile).withStream { outputStream ->
            def bdioWriter = bdioFileWriter.createBdioWriter(outputStream, project, version)
            try {
                for (BdioComponentDetails bdioComponent : bdioEntries) {
                    bdioFileWriter.writeComponent(bdioWriter, bdioComponent)
                }
            } finally {
                bdioWriter.close()
            }
        }
    }

    public List<BdioComponentDetails> extractDpkgComponentsFromStatusFile(String fileName, int size, String name, String version, String arch){
        URL url = this.getClass().getResource("/$fileName")
        File file = new File(URLDecoder.decode(url.getFile(), 'UTF-8'))

        DpkgStatusFileExtractor extractor = new DpkgStatusFileExtractor()
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
        bdioEntries
    }
}
