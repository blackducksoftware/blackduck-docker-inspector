/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2019 Synopsys, Inc.
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
package com.synopsys.integration.blackduck.dockerinspector.help;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.programversion.ClassPathPropertiesFile;
import com.synopsys.integration.exception.IntegrationException;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

@Component
public class HelpReader {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Configuration cfg;

    private Map<String, Object> variableData;

    public HelpReader() {
        cfg = new Configuration(Configuration.VERSION_2_3_29);
        cfg.setClassForTemplateLoading(this.getClass(), "/help/content");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setFallbackOnNullLoopVariable(false);
    }

    public String getStringFromHelpFile(final String givenHelpTopicName) throws IntegrationException {
        final String helpTopicName = ensureNotNull(givenHelpTopicName);
        try {
            ensureVariableDataLoaded();
            Template template = createFreemarkerTemplate(helpTopicName);
            final String helpFileContents = populateVariableValuesInHelpContent(template);
            return helpFileContents;
        } catch (IOException | TemplateException e) {
            final String msg = String.format("Error processing help file for help topic: %s", helpTopicName);
            logger.error(msg, e);
            throw new IntegrationException(msg, e);
        }
    }

    private void ensureVariableDataLoaded() throws IOException {
        if (variableData == null) {
            variableData = new HashMap<>();
            // TODO it's odd having two different loading methods, but ClassPathPropertiesFile
            // doesn't work with the /help/data/help.properties path
            final Properties helpProperties = new Properties();
            helpProperties.load(this.getClass().getResourceAsStream("/help/data/help.properties"));

            final ClassPathPropertiesFile classPathPropertiesFileProgram = new ClassPathPropertiesFile("version.properties");
            final Properties programProperties = classPathPropertiesFileProgram.getProperties();

            for (final Object rawKey : programProperties.keySet()) {
                // TODO avoidable?
                final String key = (String) rawKey;
                final String value = programProperties.getProperty(key);
                final String adjustedKey = key.replaceAll("\\.", "_");
                helpProperties.put(adjustedKey, value);
            }

            for (final String propertyName : helpProperties.stringPropertyNames()) {
                variableData.put(propertyName, helpProperties.getProperty(propertyName));
            }
        }
    }

    private String ensureNotNull(String helpTopicName) {
        if (helpTopicName == null) {
            helpTopicName = HelpTopicParser.HELP_TOPIC_NAME_OVERVIEW;
        }
        return helpTopicName;
    }

    private String populateVariableValuesInHelpContent(final Template template) throws TemplateException, IOException {
        final Writer out = new StringWriter();
        template.process(variableData, out);
        return out.toString();
    }

    private Template createFreemarkerTemplate(final String helpTopicName) throws IOException {
        Template template;
        try {
            template = cfg.getTemplate(String.format("/%s.md", helpTopicName.toLowerCase()));
        } catch (IOException e) {
            logger.info(String.format("Help topic %s not found; providing %s instead", helpTopicName, HelpTopicParser.HELP_TOPIC_NAME_OVERVIEW));
            template = cfg.getTemplate(String.format("/%s.md", HelpTopicParser.HELP_TOPIC_NAME_OVERVIEW));
        } return template;
    }
}
