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
import com.blackducksoftware.integration.hub.docker.executor.DpkgExecutor

@Component
class DpkgExtractor extends Extractor {

	@Autowired
	DpkgExecutor executor

	@PostConstruct
	void init() {
		def forges = [
			OperatingSystemEnum.DEBIAN.forge,
			OperatingSystemEnum.UBUNTU.forge
		]
		initValues(PackageManagerEnum.DPKG, executor, forges)
	}

	List<BdioComponent> extractComponents(ExtractionDetails extractionDetails, String[] packageList) {
		def components = []
		boolean startOfComponents = false
		packageList.each { packageLine ->
			if (packageLine != null) {
				if (packageLine.matches("\\+\\+\\+-=+-=+-=+-=+")) {
					startOfComponents = true
				} else if (startOfComponents){
					String componentInfo = packageLine.substring(3)
					def(name,version,architecture,description) = componentInfo.tokenize(" ")
					if (name.contains(":")) {
						name = name.substring(0, name.indexOf(":"))
					}
					String externalId = "$name/$version/$architecture"

					components.addAll(createBdioComponent(name, version, externalId))
				}
			}
		}
		components
	}
}