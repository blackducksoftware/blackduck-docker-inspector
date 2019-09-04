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
import java.util.List;
import java.util.SortedSet;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.DockerInspectorOption;
import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;

import com.synopsys.integration.blackduck.dockerinspector.help.format.Converter;
import com.synopsys.integration.exception.IntegrationException;

import com.synopsys.integration.exception.IntegrationException;

@Component
public class HelpText {

    @Autowired
    private Config config;

    @Autowired
    private ProgramVersion programVersion;

    @Autowired
    private HelpTopicParser helpTopicParser;

    @Autowired
    private HelpReader helpReader;


    public String get(final Converter converter, final String givenHelpTopicNames) throws IntegrationException, IllegalAccessException {
        final String helpTopicNames = helpTopicParser.translateGivenTopicNames(givenHelpTopicNames);
        final String markdownContent = collectMarkdownContent(helpTopicNames);
        final String formattedHelp = converter.convert(markdownContent);
        return formattedHelp;
    }

    private String collectMarkdownContent(final String helpTopicNames) throws IntegrationException, IllegalAccessException {
        final List<String> helpTopics = helpTopicParser.deriveHelpTopicList(helpTopicNames);
        final StringBuilder markdownContent = new StringBuilder();
        for (final String helpTopicName : helpTopics) {
            markdownContent.append(getMarkdownForHelpTopic(helpTopicName));
            markdownContent.append("\n");
        }
        return markdownContent.toString();
    }

    private String getMarkdownForHelpTopic(final String helpTopicName) throws IntegrationException, IllegalAccessException {
        if (helpTopicParser.HELP_TOPIC_NAME_PROPERTIES.equalsIgnoreCase(helpTopicName)) {
            return getMarkdownForProperties();
        } else if (helpTopicParser.HELP_TOPIC_NAME_PROGRAM_NAMEVERSION.equalsIgnoreCase(helpTopicName)) {
            return getMarkdownForProgram();
        } else {
            return helpReader.getStringFromHelpFile(helpTopicName);
        }
    }

    private String getMarkdownForProperties() throws IllegalAccessException {
        final StringBuilder usage = new StringBuilder();
        usage.append("## Available properties:\n");
        final SortedSet<DockerInspectorOption> configOptions = config.getPublicConfigOptions();
        for (final DockerInspectorOption opt : configOptions) {
            final StringBuilder usageLine = new StringBuilder(String.format("* %s [%s]: %s", opt.getKey(), opt.getValueTypeString(), opt.getDescription()));
            if (!StringUtils.isBlank(opt.getDefaultValue())) {
                usageLine.append(String.format("; default: %s", opt.getDefaultValue()));
            }
            if (opt.isDeprecated()) {
                usageLine.append(String.format("; [DEPRECATED]"));
            }
            usage.append(usageLine.toString());
            usage.append("\n");
        }
        return usage.toString();
    }

    private String getMarkdownForProgram() {
        return String.format("# %s %s\n\n[TOC]\n\n", programVersion.getProgramNamePretty(), programVersion.getProgramVersion());
    }
}
