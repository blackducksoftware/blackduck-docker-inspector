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
package com.synopsys.integration.blackduck.dockerinspector.help.format;

import java.util.Arrays;

import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;

public class MarkdownToHtmlConverter implements Converter {

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownToHtmlConverter() {
        final DataHolder options = new MutableDataSet().set(Parser.EXTENSIONS, Arrays.asList(TocExtension.create()));
        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).indentSize(2).build();
    }

    @Override
    public String convert(final String markdown) {
        final Node document = parser.parse(markdown);
        final String bodyHtml = renderer.render(document);
        final String fullHtml = String.format("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">\n</head>\n<body>\n%s\n</body>\n</html>", bodyHtml);
        return fullHtml;
    }
}
