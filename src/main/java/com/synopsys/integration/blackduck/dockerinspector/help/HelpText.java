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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.DockerInspectorOption;
import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;

@Component
public class HelpText {
    private final Parser parser;
    private final HtmlRenderer renderer;

    @Autowired
    private Config config;

    @Autowired
    private ProgramVersion programVersion;

    @Autowired
    private HelpTopicParser helpTopicParser;

    @Autowired
    private HelpFormatParser helpFormatParser;

    public HelpText() {
        final DataHolder options = new MutableDataSet().set(Parser.EXTENSIONS, Arrays.asList(TocExtension.create()));
        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).indentSize(2).build();
    }

    public String get(String givenHelpTopicNames) throws IllegalArgumentException, IOException, IllegalAccessException {
        final String helpTopicNames = helpTopicParser.translateGivenTopicNames(givenHelpTopicNames);
        final String markdownContent = collectMarkdownContent(helpTopicNames);
        final HelpFormat helpFormat = helpFormatParser.getHelpFormat();
        final String formattedHelp = translateMarkdownToGivenFormat(markdownContent, helpFormat);
        return formattedHelp;
    }

    private String collectMarkdownContent(final String helpTopicNames) throws IOException, IllegalAccessException {
        final List<String> helpTopics = helpTopicParser.deriveHelpTopicList(helpTopicNames);
        final StringBuilder markdownContent = new StringBuilder();
        for (final String helpTopicName : helpTopics) {
            markdownContent.append(getMarkdownForHelpTopic(helpTopicName));
            markdownContent.append("\n");
        }
        return markdownContent.toString();
    }

    private String translateMarkdownToGivenFormat(final String markdownContent, final HelpFormat helpFormat) {
        switch (helpFormat) {
            case MARKDOWN:
                return markdownContent;
            case HTML:
                return markdownToHtml(markdownContent);
            default:
                throw new UnsupportedOperationException(String.format("Help format %s not supported", helpFormat.name()));
        }
    }

    private String getMarkdownForHelpTopic(final String helpTopicName) throws IllegalArgumentException, IOException, IllegalAccessException {
        if (helpTopicParser.HELP_TOPIC_NAME_PROPERTIES.equalsIgnoreCase(helpTopicName)) {
            return getMarkdownForProperties();
        } else if (helpTopicParser.HELP_TOPIC_NAME_PROGRAM_NAMEVERSION.equalsIgnoreCase(helpTopicName)) {
            return getMarkdownForProgram();
        } else {
            return getStringFromHelpFile(helpTopicName);
        }
    }

    private String markdownToHtml(final String markdown) {
        final Node document = parser.parse(markdown);
        final String bodyHtml = renderer.render(document);
        final String fullHtml = String.format("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">\n</head>\n<body>\n%s\n</body>\n</html>", bodyHtml);
        return fullHtml;
    }

    private String getStringFromHelpFile(final String helpTopicName) throws IOException {
        InputStream helpFileInputStream = getInputStreamForHelpTopic(helpTopicName);
        if (helpFileInputStream == null) {
            helpFileInputStream = getInputStreamForHelpTopic(helpTopicParser.HELP_TOPIC_NAME_OVERVIEW);
        }
        return readFromInputStream(helpFileInputStream);
    }

    private InputStream getInputStreamForHelpTopic(final String helpTopicName) {
        final String pathRelToClasspath = String.format("/help/%s.md", helpTopicName.toLowerCase());
        return this.getClass().getResourceAsStream(pathRelToClasspath);
    }

    private String readFromInputStream(InputStream inputStream) throws IOException {
        final StringBuilder resultStringBuilder = new StringBuilder();
        try (final BufferedReader br
                 = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
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
