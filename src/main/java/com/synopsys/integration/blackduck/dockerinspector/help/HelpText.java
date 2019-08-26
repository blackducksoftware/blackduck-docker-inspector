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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.DockerInspector;
import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.DockerInspectorOption;
import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;
import com.vladsch.flexmark.ext.toc.TocBlock;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.ext.toc.internal.TocNodeRenderer;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlRenderer.Builder;
import com.vladsch.flexmark.html.HtmlRenderer.HtmlRendererExtension;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.DelegatingNodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.builder.Extension;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;

@Component
public class HelpText {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // TODO: auto generate "all"?
    private static final String HELP_TOPIC_NAME_PROGRAM_NAMEVERSION = "program";
    private static final String HELP_TOPIC_NAME_OVERVIEW = "overview";
    private static final String HELP_TOPIC_NAME_PROPERTIES = "properties";
    private static final String HELP_TOPIC_NAME_ALL = "all";
    private static final String ALL_HELP_TOPICS = String.format("%s,%s,architecture,running,%s,advanced,deployment,troubleshooting,releasenotes",
        HELP_TOPIC_NAME_PROGRAM_NAMEVERSION, HELP_TOPIC_NAME_OVERVIEW, HELP_TOPIC_NAME_PROPERTIES);

//    private final Parser parser;
//    private final HtmlRenderer renderer;

    @Autowired
    private Config config;

    @Autowired
    private ProgramVersion programVersion;

    static final DataHolder OPTIONS = new MutableDataSet().set(Parser.EXTENSIONS, Arrays.asList(
        TocExtension.create(),
        CustomExtension.create()
    ));

    static final Parser PARSER = Parser.builder(OPTIONS).build();
    static final HtmlRenderer RENDERER = HtmlRenderer.builder(OPTIONS).indentSize(2).build();

    // TODO look for old references to topics that are named topic, here and in DockerInspector.java

    public HelpText() {
        // From https://github.com/vsch/flexmark-java/blob/master/flexmark-ext-toc/src/test/java/com/vladsch/flexmark/ext/toc/ComboSimTocSpecTest.java
//        final DataHolder options = new MutableDataSet()
//                                       .set(HtmlRenderer.INDENT_SIZE, 2)
//                                       .set(HtmlRenderer.RENDER_HEADER_ID, true)
//                                       .set(Parser.EXTENSIONS, Collections.singletonList(SimTocExtension.create()));



        // TODO clean this up
//        parser = PARSER;
//        renderer = RENDERER;
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
        final StringBuilder markdownSb = new StringBuilder();
        for (final String helpTopicName : helpTopics) {
            markdownSb.append(getMarkdownForHelpTopic(helpTopicName));
        }
        switch (helpFormat) {
            case MARKDOWN:
                return markdownSb.toString();
            case HTML:
                return markdownToHtml(markdownSb.toString());
            default:
                throw new UnsupportedOperationException(String.format("Help format %s not supported", helpFormat.name()));
        }
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
        } else if (HELP_TOPIC_NAME_PROGRAM_NAMEVERSION.equalsIgnoreCase(helpTopicName)) {
            return getMarkdownForProgram();
        } else {
            return getStringFromHelpFile(helpTopicName.toLowerCase());
        }
    }

    private String getHtmlForTopic(final String helpTopicName) throws IOException, IllegalAccessException {
        if (HELP_TOPIC_NAME_PROPERTIES.equalsIgnoreCase(helpTopicName)) {
            return markdownToHtml(getMarkdownForProperties());
        } else if (HELP_TOPIC_NAME_PROGRAM_NAMEVERSION.equalsIgnoreCase(helpTopicName)) {
            return markdownToHtml(getMarkdownForProgram());
        } else {
            final String helpFileContents = getStringFromHelpFile(helpTopicName);
            return markdownToHtml(helpFileContents);
        }
    }

    private String markdownToHtml(final String markdown) {
        final Node document = PARSER.parse(markdown);
        final String html = RENDERER.render(document);
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
        return String.format("[TOC]\n\n# %s %s\n\n", programVersion.getProgramNamePretty(), programVersion.getProgramVersion());
    }

    static class CustomExtension implements HtmlRendererExtension {
        @Override
        public void rendererOptions(MutableDataHolder options) {

        }

        @Override
        public void extend(Builder rendererBuilder, String rendererType) {
            rendererBuilder.nodeRendererFactory(new CustomNodeRenderer.Factory());
        }

        static Extension create() {
            return new CustomExtension();
        }
    }

    static class CustomNodeRenderer implements NodeRenderer {
        public static class Factory implements DelegatingNodeRendererFactory {
            @Override
            public NodeRenderer apply(DataHolder options) {
                return new CustomNodeRenderer();
            }

            @Override
            public Set<Class<? extends NodeRendererFactory>> getDelegates() {
                Set<Class<? extends NodeRendererFactory>> set = new HashSet<Class<? extends NodeRendererFactory>>();
                // add node renderer factory classes to which this renderer will delegate some of its rendering
                // core node renderer is assumed to have all depend it so there is no need to add it
                set.add(TocNodeRenderer.Factory.class);
                return set;

                // return null if renderer does not delegate or delegates only to core node renderer
                //return null;
            }

        }
        @Override
        public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
            HashSet<NodeRenderingHandler<?>> set = new HashSet<NodeRenderingHandler<?>>();
            set.add(new NodeRenderingHandler<TocBlock>(TocBlock.class, new com.vladsch.flexmark.html.CustomNodeRenderer<TocBlock>() {
                @Override
                public void render(TocBlock node, NodeRendererContext context, HtmlWriter html) {
                    // test the node to see if it needs overriding
                    NodeRendererContext subContext = context.getDelegatedSubContext(true);
                    subContext.delegateRender();
                    String tocText = subContext.getHtmlWriter().toString(0);

                    // output to separate stream
                    System.out.println("---- TOC HTML --------------------");
                    System.out.print(tocText);
                    System.out.println("----------------------------------\n");

                    html.append(tocText);

                    html.tagLineIndent("div", () -> html.append(subContext.getHtmlWriter()));
                }
            }));

            return set;
        }
    }
}
