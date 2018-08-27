/**
 * hub-docker-inspector
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
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
package com.synopsys.integration.blackduck.dockerinspector.dockerclient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;

@PropertySource("classpath:application.properties")
@Component
class BlackDuckDockerProperties {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Config config;

    private Properties propsForSubContainer;

    public void load() throws IOException, IllegalArgumentException, IllegalAccessException {
        propsForSubContainer = new Properties();

        for (final String propertyKey : config.getAllKeys()) {
            logger.trace(String.format("Config option key: %s", propertyKey));
            final String value = config.get(propertyKey);
            logger.trace(String.format("load(): %s=%s (%s)", propertyKey, getValueObscuredIfSensitive(propertyKey, value), config.isPublic(propertyKey) ? "public" : "private"));
            propsForSubContainer.put(propertyKey, value);
        }
    }

    private String getValueObscuredIfSensitive(final String key, final String value) {
        if ((!StringUtils.isBlank(value)) && (key.endsWith(".password"))) {
            return "********";
        }
        return value;
    }

    public void set(final String key, final String value) {
        propsForSubContainer.setProperty(key, value);
    }

    public void save(final String path) throws FileNotFoundException, IOException {
        final File propertiesFile = new File(path);
        if (!propertiesFile.getParentFile().exists()) {
            propertiesFile.getParentFile().mkdirs();
        }
        if (propertiesFile.exists()) {
            propertiesFile.delete();
        }
        propsForSubContainer.store(new FileOutputStream(propertiesFile), null);
    }
}
