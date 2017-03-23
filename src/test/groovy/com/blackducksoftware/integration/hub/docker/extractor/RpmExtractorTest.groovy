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

class RpmExtractorTest {

    @Test
    void testExtractingRpmFile1() {
        extractingRpm("rpmdb/1/rpm")
    }

    void extractingRpm(String fileName) {
        final RpmExtractor extractor = new RpmExtractor()
        final URL url = this.getClass().getResource("/$fileName")
        final File file = new File(URLDecoder.decode(url.getFile()))
        extractor.extractComponents(null,null, file)
    }
}
