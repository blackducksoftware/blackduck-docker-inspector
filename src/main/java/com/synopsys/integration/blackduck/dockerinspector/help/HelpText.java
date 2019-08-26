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
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.DockerInspectorOption;
import com.vladsch.flexmark.ext.toc.SimTocExtension;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;

@Component
public class HelpText {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // These help topics are special; this class doesn't need to know about the rest
    private static final String HELP_TOPIC_NAME_OVERVIEW = "overview";
    private static final String HELP_TOPIC_NAME_PROPERTIES = "properties";
    private static final String HELP_TOPIC_NAME_ALL = "all";
    private static final String ALL_HELP_TOPICS = "overview,architecture,running,properties,advanced,deployment,troubleshooting,releasenotes";

    private final Parser parser;
    private final HtmlRenderer renderer;

    @Autowired
    private Config config;

    public HelpText() {
        // From https://github.com/vsch/flexmark-java/blob/master/flexmark-ext-toc/src/test/java/com/vladsch/flexmark/ext/toc/ComboSimTocSpecTest.java
//        final DataHolder options = new MutableDataSet()
//                                       .set(HtmlRenderer.INDENT_SIZE, 2)
//                                       .set(HtmlRenderer.RENDER_HEADER_ID, true)
//                                       .set(Parser.EXTENSIONS, Collections.singletonList(SimTocExtension.create()));
        final MutableDataSet options = new MutableDataSet();
        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();
    }

    public String get(String givenHelpTopicName) throws IllegalArgumentException, IOException, IllegalAccessException {
        String helpTopicName = translateGivenTopicName(givenHelpTopicName);
        HelpFormat helpFormat = getHelpFormat();
        return get(helpTopicName, helpFormat);
    }

    private HelpFormat getHelpFormat() {
        final String givenHelpFormatName = config.getHelpOutputFormat();
        if (StringUtils.isBlank(givenHelpFormatName)) {
            return HelpFormat.MARKDOWN;
        }
        HelpFormat helpFormat;
        try {
            helpFormat = HelpFormat.valueOf(config.getHelpOutputFormat().toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn(String.format("Invalid help format requested: %s; using MARKDOWN (text) instead", config.getHelpOutputFormat()));
            helpFormat = HelpFormat.MARKDOWN;
        }
        return helpFormat;
    }

    private String get(String helpTopicNames, HelpFormat helpFormat) throws IllegalArgumentException, IOException, IllegalAccessException {
        final List<String> helpTopics = deriveHelpTopicList(helpTopicNames);
        final StringBuilder sb = new StringBuilder();
        for (final String helpTopicName : helpTopics) {
            switch (helpFormat) {
                case MARKDOWN:
                    sb.append(getMarkdownForHelpTopic(helpTopicName));
                    break;
                case HTML:
                    sb.append(getHtmlForTopic(helpTopicName));
                    break;
                default:
                    throw new UnsupportedOperationException(String.format("Help format %s not supported", helpFormat.name()));
            }
        }
        return sb.toString();
    }

    private List<String> deriveHelpTopicList(final String helpTopicsString) {
        if (StringUtils.isBlank(helpTopicsString)) {
            return Arrays.asList("");
        }
        return Arrays.asList(helpTopicsString.split(","));
    }

    private String translateGivenTopicName(final String givenHelpTopic) {
        if (givenHelpTopic == null) {
            return HELP_TOPIC_NAME_OVERVIEW;
        }
        if (HELP_TOPIC_NAME_ALL.equalsIgnoreCase(givenHelpTopic)) {
            return ALL_HELP_TOPICS;
        }
        return givenHelpTopic;
    }

    private String getMarkdownForHelpTopic(final String helpTopicName) throws IllegalArgumentException, IOException, IllegalAccessException {
        if (HELP_TOPIC_NAME_PROPERTIES.equalsIgnoreCase(helpTopicName)) {
            return getMarkdownForProperties();
        } else {
            return getStringFromHelpFile(helpTopicName.toLowerCase());
        }
    }

    private String getHtmlForTopic(final String helpTopicName) throws IOException, IllegalAccessException {
        if (HELP_TOPIC_NAME_PROPERTIES.equalsIgnoreCase(helpTopicName)) {
            return markdownToHtml(getMarkdownForProperties());
        } else {
            final String helpFileContents = getStringFromHelpFile(helpTopicName);
            return markdownToHtml(helpFileContents);
        }
    }

    private String markdownToHtml(final String markdown) {
        final Node document = parser.parse(markdown);
        final String html = renderer.render(document);
        return html;
    }

    private String getStringFromHelpFile(final String helpTopicName) throws IOException {
        InputStream helpFileInputStream = getInputStreamForHelpTopic(helpTopicName);
        if (helpFileInputStream == null) {
            helpFileInputStream = getInputStreamForHelpTopic(HELP_TOPIC_NAME_OVERVIEW);
        }
        return readFromInputStream(helpFileInputStream);
    }

    private InputStream getInputStreamForHelpTopic(final String helpTopicName) {
        final String pathRelToClasspath = String.format("/help/%s.md", helpTopicName);
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
        usage.append("# Available properties:\n");
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

}
