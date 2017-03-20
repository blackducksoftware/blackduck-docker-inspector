package com.blackducksoftware.integration.hub.linux.extractor

import static org.junit.Assert.*

import org.junit.Test

import com.blackducksoftware.integration.hub.linux.BdioComponentDetails
import com.blackducksoftware.integration.hub.linux.OperatingSystemEnum

class AptListTest {
    @Test
    void testThing() {
        def (hubProjectName, hubProjectVersionName, forge, packageManager) = 'int-bamboo01_bdsi_2017-02-08_bdsi_ubuntu_bdsi_apt.txt'.split('_bdsi_')
        println hubProjectName
        println hubProjectVersionName
        println forge
        println packageManager
    }
    @Test
    void testExtractingAptFile1() {
        extractAptComponentsFromFile('ubuntu_apt_package_list_1.txt', 714,'python3-commandnotfound','0.3ubuntu12')
    }

    @Test
    void testExtractingAptFile2() {
        extractAptComponentsFromFile('ubuntu_apt_package_list_2.txt', 691,'ntp','1:4.2.6.p5+dfsg-3ubuntu2.14.04.10')
    }

    @Test
    void testExtractingAptFile3() {
        extractAptComponentsFromFile('ubuntu_apt_package_list_3.txt', 631,'lsb-base','4.1+Debian11ubuntu6.1')
    }

    public void extractAptComponentsFromFile(String fileName, int size, String name, String version){
        AptExtractor extractor = new AptExtractor()
        URL url = this.getClass().getResource("/$fileName")
        File file = new File(URLDecoder.decode(url.getFile(), 'UTF-8'))
        List<BdioComponentDetails> bdioEntries =  extractor.extractComponents(OperatingSystemEnum.UBUNTU , file)

        assertEquals(size, bdioEntries.size())
        boolean foundTargetEntry = false
        int validEntryCount = 0
        for (final BdioComponentDetails bdioEntry : bdioEntries) {
            if (bdioEntry != null) {
                validEntryCount++
                // println(bdioEntry.getExternalIdentifier())
                def match = String.join("/",name,version)
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
