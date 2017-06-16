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
package com.blackducksoftware.integration.hub.docker.client


import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@PropertySource("classpath:application.properties")
@Component
class HubDockerProperties {

    private final Logger logger = LoggerFactory.getLogger(HubDockerProperties.class)

	@Autowired
	private Environment env

    private Properties propsForSubContainer

    public void load() {
        propsForSubContainer = new Properties()
		
		final ClassPathPropertiesFile propertiesFromFile = new ClassPathPropertiesFile('application.properties')
		for (String propertyKey : propertiesFromFile.keySet()) {
			String value = env.getProperty(propertyKey)
			logger.debug("load(): ${propertyKey}=${value}")
			propsForSubContainer.put(propertyKey, value)
		}
    }

    public void set(String key, String value) {
        propsForSubContainer.setProperty(key, value)
    }

    public void save(String path) {
        File propertiesFile = new File(path)
        if (!propertiesFile.getParentFile().exists()) {
            propertiesFile.getParentFile().mkdirs()
        }
        if (propertiesFile.exists()) {
            propertiesFile.delete()
        }
        propsForSubContainer.store(new FileOutputStream(propertiesFile), null)
    }
}
