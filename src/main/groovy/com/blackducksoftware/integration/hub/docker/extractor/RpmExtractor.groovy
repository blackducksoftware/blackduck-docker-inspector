/**
 * Hub Docker Inspector
 *
 * Copyright (C) 2017 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.hub.docker.extractor

import javax.annotation.PostConstruct

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.bdio.simple.model.BdioComponent
import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum
import com.blackducksoftware.integration.hub.docker.executor.RpmExecutor

@Component
class RpmExtractor extends Extractor {

	@Autowired
	RpmExecutor executor

	@PostConstruct
	void init() {
		def forges = [
			OperatingSystemEnum.CENTOS.forge,
			OperatingSystemEnum.FEDORA.forge,
			OperatingSystemEnum.REDHAT.forge
		]
		initValues(PackageManagerEnum.RPM, executor, forges)
	}

	boolean valid(String packageLine) {
		packageLine.matches(".+-.+-.+\\..*")
	}

	List<BdioComponent> extractComponents(ExtractionDetails extractionDetails, String[] packageList) {
		def components = []
		packageList.each { packageLine ->
			if (valid(packageLine)) {
				def lastDotIndex = packageLine.lastIndexOf('.')
				def arch = packageLine.substring(lastDotIndex + 1)
				def lastDashIndex = packageLine.lastIndexOf('-')
				def nameVersion = packageLine.substring(0, lastDashIndex)
				def secondToLastDashIndex = nameVersion.lastIndexOf('-')

				def versionRelease = packageLine.substring(secondToLastDashIndex + 1, lastDotIndex)
				def artifact = packageLine.substring(0, secondToLastDashIndex)

				String externalId = "${artifact}/${versionRelease}/${arch}"

				components.addAll(createBdioComponent(artifact, versionRelease, externalId))
			}
		}
		components
	}
}