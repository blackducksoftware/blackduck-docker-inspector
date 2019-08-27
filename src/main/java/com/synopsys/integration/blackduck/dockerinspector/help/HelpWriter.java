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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import javax.naming.OperationNotSupportedException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.exception.HelpGenerationException;

@Component
public class HelpWriter {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Config config;

    @Autowired
    private HelpText helpText;

    @Autowired
    private HelpFilename helpFilename;

    public void write(final String helpTopicNames) throws HelpGenerationException {
        try {
            final PrintStream printStream = derivePrintStream();
            printStream.println(helpText.get(helpTopicNames));
        } catch (Exception e) {
            throw new HelpGenerationException(String.format("Error generating help: %s", e.getMessage()), e);
        }
    }

    private PrintStream derivePrintStream() throws FileNotFoundException, OperationNotSupportedException {
        final String givenHelpOutputFilePath = config.getHelpOutputFilePath();
        if (StringUtils.isBlank(givenHelpOutputFilePath)) {
            return System.out;
        }

        final File givenHelpOutputLocation = new File(givenHelpOutputFilePath);
        final File finalHelpOutputFile;
        if (givenHelpOutputLocation.isDirectory()) {
            finalHelpOutputFile = new File(givenHelpOutputLocation, helpFilename.getDefaultHelpFilename());
        } else {
            finalHelpOutputFile = givenHelpOutputLocation;
        }
        logger.info(String.format("Writing help output to: %s", finalHelpOutputFile.getAbsolutePath()));
        final PrintStream printStream = new PrintStream(finalHelpOutputFile);
        return printStream;
    }
}
