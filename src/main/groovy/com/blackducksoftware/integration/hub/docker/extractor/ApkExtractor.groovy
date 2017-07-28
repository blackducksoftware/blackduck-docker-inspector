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

import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.bdio.simple.model.BdioComponent
import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum
import com.blackducksoftware.integration.hub.docker.executor.ApkExecutor

@Component
class ApkExtractor extends Extractor {

    @Autowired
    ApkExecutor executor

    @PostConstruct
    void init() {
        def forges = [
            OperatingSystemEnum.ALPINE.forge
        ]
        initValues(PackageManagerEnum.APK, executor, forges)
    }

    List<BdioComponent> extractComponents(ExtractionDetails extractionDetails, String[] packageList) {
        def components = []
        packageList.each { packageLine ->
            if (!packageLine.toLowerCase().startsWith('warning')) {
                String[] parts = packageLine.split('-')
                def version = "${parts[parts.length -2]}-${parts[parts.length -1]}"
                def component = ''
                parts = parts.take(parts.length - 2)
                for(String part : parts){
                    if(StringUtils.isNotBlank(component)){
                        component += "-${part}"
                    } else{
                        component = part
                    }
                }
                // if a package starts with a period, we should ignore it because it is a virtual meta package and the version information is missing
                if(!component.startsWith('.')){
                    String externalId = "${component}/${version}/${extractionDetails.architecture}"
                    components.addAll(createBdioComponent(component, version, externalId))
                }
            }
        }
        components
    }
}