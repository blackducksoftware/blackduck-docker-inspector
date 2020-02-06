/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2020 Synopsys, Inc.
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

import java.util.List;
import java.util.SortedSet;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.DockerInspectorOption;

import com.synopsys.integration.exception.IntegrationException;

@Component
public class HelpText {

    @Autowired
    private Config config;

    @Autowired
    private HelpTopicParser helpTopicParser;

    @Autowired
    private HelpReader helpReader;

    public String getMarkdownForTopics(final String givenHelpTopicNames) throws IntegrationException, IllegalAccessException {
        final String helpTopicNames = helpTopicParser.translateGivenTopicNames(givenHelpTopicNames);
        final String markdownContent = collectMarkdownContent(helpTopicNames);
        return markdownContent;
    }

    public String getMarkdownForTopic(final String helpTopicName) throws IntegrationException, IllegalAccessException {
        if (helpTopicParser.HELP_TOPIC_NAME_PROPERTIES.equalsIgnoreCase(helpTopicName)) {
            return getMarkdownForProperties();
        } else {
            return helpReader.getVariableSubstitutedTextFromHelpFile(helpTopicName);
        }
    }

    private String collectMarkdownContent(final String helpTopicNames) throws IntegrationException, IllegalAccessException {
        final List<String> helpTopics = helpTopicParser.deriveHelpTopicList(helpTopicNames);
        final StringBuilder markdownContent = new StringBuilder();
        for (final String helpTopicName : helpTopics) {
            markdownContent.append(getMarkdownForTopic(helpTopicName));
            markdownContent.append("\n");
        }
        return markdownContent.toString();
    }

    private String getMarkdownForProperties() throws IllegalAccessException, IntegrationException {
        final StringBuilder usage = new StringBuilder();
        usage.append("You can configure Docker Inspector by setting any of the following properties. " +
                         "Properties are typically set via the command line by adding a command line " +
                         "argument of the form:\n\n" +
                         "    --{property name}={property value}\n" +
                         "\n\n" +
                        "See the [Advanced](advanced.md) for other ways to set properties.\n\n" +
                        "Available properties:\n\n");
        usage.append("Property name | Type | Description | Default value\n");
        usage.append("------------- | ---- | ----------- | -------------\n");
        final SortedSet<DockerInspectorOption> configOptions = config.getPublicConfigOptions();
        for (final DockerInspectorOption opt : configOptions) {
            final StringBuilder usageLine = new StringBuilder(String.format("%s | %s | %s | ", opt.getKey(), opt.getValueTypeString(), opt.getDescription()));
            if (!StringUtils.isBlank(opt.getDefaultValue())) {
                usageLine.append(opt.getDefaultValue());
            } else {
                usageLine.append(" ");
            }
            usageLine.append("| ");
            if (opt.isDeprecated()) {
                throw new IntegrationException("Need to add a column in help for property deprecation status");
            }
            usage.append(usageLine.toString());
            usage.append("\n");
        }
        return usage.toString();
    }
}
