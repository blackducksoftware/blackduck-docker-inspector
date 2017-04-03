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
package com.blackducksoftware.integration.hub.docker

import org.junit.Test

import com.blackducksoftware.integration.hub.docker.tar.DockerTarParser
import com.blackducksoftware.integration.hub.docker.tar.TarExtractionResults

class DockerTarParserTest {

    @Test
    void testPerformExtractOfDockerTar(){
        //File dockerTar = new File("ubuntu.tar")
        File dockerTar = new File("alpine.tar")

        DockerTarParser tarParser = new DockerTarParser()
        tarParser.workingDirectory = new File("docker")

        TarExtractionResults results = tarParser.parseImageTar(dockerTar)
        println results.operatingSystemEnum.name()
    }
}
