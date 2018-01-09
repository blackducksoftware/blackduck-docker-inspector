/**
 * Hub Docker Inspector
 *
 * Copyright (C) 2017 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
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
package com.blackducksoftware.integration.hub.docker.imageinspector.linux.executor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.docker.imageinspector.config.Config;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;

@Component
public class Executor {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private Config config;

    public String[] executeCommand(final String commandString) throws HubIntegrationException, UnsupportedEncodingException {
        logger.debug(String.format("Executing: %s with timeout %s", commandString, config.getCommandTimeout()));
        final CommandLine cmdLine = CommandLine.parse(commandString);
        final DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValue(1);
        final ExecuteWatchdog watchdog = new ExecuteWatchdog(config.getCommandTimeout());
        executor.setWatchdog(watchdog);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        final PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorStream);
        executor.setStreamHandler(streamHandler);
        int exitValue = -1;
        try {
            exitValue = executor.execute(cmdLine);
        } catch (final ExecuteException e) {
            exitValue = e.getExitValue();
            logger.trace(String.format("Execution of command: %s: ExecutionException: %s; exitCode: %d; Continuing anyway...", commandString, e.getMessage(), exitValue));
            // throw new HubIntegrationException(String.format("Execution of command: %s: ExecutionException: %s", commandString, e.getMessage()));
        } catch (final IOException e) {
            throw new HubIntegrationException(String.format("Execution of command: %s: IOException: %s", commandString, e.getMessage()));
        }
        if (watchdog.killedProcess()) {
            throw new HubIntegrationException(String.format("Execution of command: %s with timeout %d timed out", commandString, new Long(config.getCommandTimeout()), exitValue));
        }
        if (exitValue == 0) {
            logger.debug(String.format("Success executing command: %s", commandString));
        } else {
            throw new HubIntegrationException(String.format("Execution of command: %s: Error code: %d: stderr: %s", commandString, exitValue, errorStream.toString(StandardCharsets.UTF_8.name())));
        }

        logger.debug(String.format("Command output: %s", outputStream.toString(StandardCharsets.UTF_8.name())));
        return outputStream.toString(StandardCharsets.UTF_8.name()).split(System.lineSeparator());
    }

}
