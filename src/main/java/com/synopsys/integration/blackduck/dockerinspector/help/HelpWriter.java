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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.exception.HelpGenerationException;
import com.synopsys.integration.exception.IntegrationException;

@Component
public class HelpWriter {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private HelpText helpText;

    @Autowired
    private HelpTopicParser helpTopicParser;

    public void writeIndividualFilesToDir(final File outputDir, final String helpTopicNames) throws HelpGenerationException {
        try {
            final List<String> helpTopics = getTopicList(helpTopicNames);
            for (final String helpTopicName : helpTopics) {
                final String markdownFilename = deriveMarkdownFilename(helpTopicName);
                try (final PrintStream printStreamMarkdown = derivePrintStream(outputDir, markdownFilename)) {
                    printStreamMarkdown.println(helpText.getMarkdownForTopic(helpTopicName));
                }
            }
        } catch (Exception e) {
            throw new HelpGenerationException(String.format("Error generating help: %s", e.getMessage()), e);
        }
    }

    public void concatinateContentToPrintStream(final PrintStream printStream, final String helpTopicNames) throws HelpGenerationException {
        try {
            final List<String> helpTopics = getTopicList(helpTopicNames);
            for (final String helpTopicName : helpTopics) {
                printStream.println(helpText.getMarkdownForTopic(helpTopicName));
            }
        } catch (Exception e) {
            throw new HelpGenerationException(String.format("Error generating help: %s", e.getMessage()), e);
        }
    }

    private List<String> getTopicList(final String helpTopicNames) {
        final String expandedTopicNames = helpTopicParser.translateGivenTopicNames(helpTopicNames);
        return helpTopicParser.deriveHelpTopicList(expandedTopicNames);
    }

    private String deriveMarkdownFilename(final String helpTopicName) {
        return String.format("%s.md", helpTopicName);
    }

    private PrintStream derivePrintStream(final File outputDir, final String markdownFilename) throws FileNotFoundException {
        final File finalHelpOutputFile = new File(outputDir, markdownFilename);
        logger.info(String.format("Writing help output to: %s", finalHelpOutputFile.getAbsolutePath()));
        final PrintStream printStream = new PrintStream(finalHelpOutputFile);
        return printStream;
    }
}
