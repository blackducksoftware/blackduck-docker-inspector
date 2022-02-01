/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
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
