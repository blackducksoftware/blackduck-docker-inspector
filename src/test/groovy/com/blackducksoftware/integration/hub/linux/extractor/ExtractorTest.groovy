package com.blackducksoftware.integration.hub.linux.extractor

import static org.junit.Assert.*

import org.junit.Test

import com.blackducksoftware.integration.hub.linux.OperatingSystemEnum

class ExtractorTest {
    @Test
    void testCreatingCentosExternalIdentifier() {
        def extractor = [init: {}] as Extractor
        def externalIdentifier = extractor.createLinuxIdentifier(OperatingSystemEnum.CENTOS, 'name/version')
        assertEquals('centos', externalIdentifier.forge)
        assertEquals('name/version', externalIdentifier.externalId)
    }
}
