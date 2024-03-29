/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
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
        usage.append("Property name | Type | Description | Default value | Deprecation Status | Deprecation Message\n");
        usage.append("------------- | ---- | ----------- | ------------- | ------------------ | -------------------\n");
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
                usageLine.append("Deprecated");
                usageLine.append(" | ");
                if (StringUtils.isNotBlank(opt.getDeprecationMessage())) {
                    usageLine.append(opt.getDeprecationMessage());
                }
            } else {
                usageLine.append("  |  ");
            }
            usageLine.append("| ");
            usage.append(usageLine.toString());
            usage.append("\n");
        }
        return usage.toString();
    }
}
