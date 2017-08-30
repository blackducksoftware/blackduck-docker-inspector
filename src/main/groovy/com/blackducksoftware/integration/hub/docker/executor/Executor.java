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
package com.blackducksoftware.integration.hub.docker.executor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.exception.HubIntegrationException;

@Component
public class Executor {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${command.timeout}")
    private long commandTimeout;

    public String[] executeCommand(final String commandString) throws IOException, InterruptedException, HubIntegrationException {
        final List<String> commandStringList = Arrays.asList(commandString.split(" "));
        final ProcessBuilder builder = new ProcessBuilder();
        builder.command(commandStringList.toArray(new String[commandStringList.size()]));
        builder.directory(new File("."));
        final Process process = builder.start();
        final boolean finished = process.waitFor(this.commandTimeout, TimeUnit.MILLISECONDS);
        if (!finished) {
            throw new HubIntegrationException(String.format("Execution of command %s timed out (timeout: %d milliseconds)", commandString, commandTimeout));
        }
        final int errCode = process.exitValue();
        if (errCode == 0) {
            logger.debug(String.format("Execution of command: %s: Succeeded", commandString));
        } else {
            throw new HubIntegrationException(String.format("Execution of command: %s: Error code: %d", commandString, errCode));
        }
        final InputStream inputStream = process.getInputStream();
        final String outputString = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        logger.debug(String.format("Command output:/n%s", outputString));
        return outputString.split(System.lineSeparator());
    }

}
