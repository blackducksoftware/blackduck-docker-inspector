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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.SortedBag;
import org.apache.commons.collections4.bag.TreeBag;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.blackducksoftware.integration.hub.docker.PackageManagerFiles;
import com.blackducksoftware.integration.hub.docker.tar.ImagePkgMgr;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;

public abstract class Executor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private String upgradeCommand;
    private String listPackagesCommand;
    private int sampleSize; // list pkgs this # times, use mode to pick the winner

    @Value("${command.timeout}")
    long commandTimeout;

    abstract void init();

    void initValues(final String upgradeCommand, final String listPackagesCommand, final int sampleSize) {
        this.upgradeCommand = upgradeCommand;
        this.listPackagesCommand = listPackagesCommand;
        this.sampleSize = sampleSize;
    }

    public String[] runPackageManager(final ImagePkgMgr imagePkgMgr) throws HubIntegrationException, IOException, InterruptedException {
        final PackageManagerFiles pkgMgrFiles = new PackageManagerFiles();
        final Map<Integer, String[]> packagesByCount = new HashMap<>();
        final SortedBag<Integer> counts = new TreeBag<>();
        // TODO: I put the sampling to address an intermittent problem with incomplete results, but
        // I have not seen the problem since converting this class from groovy to java
        // ("".execute() to ProcessBuilder). If the problem stays away, remove this loop.
        // In the groovy implementation, the output was occasionally truncated.
        // It was subject to command.timeout, but at 2 minutes (default) you'd think that'd be pretty safe.
        // -- Steve B.
        for (int i = 0; i < sampleSize; i++) {
            pkgMgrFiles.stubPackageManagerFiles(imagePkgMgr);
            final String[] packages = listPackages();
            logger.trace(String.format("Package count: %d", packages.length));
            counts.add(packages.length);
            packagesByCount.put(packages.length, packages);
        }
        if (counts.first() != counts.last()) {
            logger.warn(String.format("Package manager reported inconsistent results (%d:%d); Using %d", counts.first(), counts.last(), counts.first()));
        }
        return packagesByCount.get(counts.first());
    }

    private String[] listPackages() throws HubIntegrationException, IOException, InterruptedException {
        String[] results;
        logger.debug("Executing package manager");
        try {
            results = executeCommand(listPackagesCommand);
            logger.info(String.format("Command %s executed successfully", listPackagesCommand));
        } catch (final Exception e) {
            if (!StringUtils.isBlank(upgradeCommand)) {
                logger.warn(String.format("Error executing \"%s\": %s; Trying to upgrade package database by executing: %s", listPackagesCommand, e.getMessage(), upgradeCommand));
                executeCommand(upgradeCommand);
                results = executeCommand(listPackagesCommand);
                logger.info(String.format("Command %s executed successfully on 2nd attempt (after db upgrade)", listPackagesCommand));
            } else {
                logger.error(String.format("Error executing \"%s\": %s; No upgrade command has been provided for this package manager", listPackagesCommand), e.getMessage());
                throw e;
            }
        }
        logger.debug(String.format("Package manager reported %s package lines", results.length));
        return results;
    }

    private String[] executeCommand(final String commandString, final int sampleSize) throws IOException, InterruptedException, HubIntegrationException {
        final Map<Integer, String[]> packagesByCount = new HashMap<>();
        final SortedBag<Integer> counts = new TreeBag<>();

        for (int i = 0; i < sampleSize; i++) {
            final String[] packages = executeCommand(commandString);
            logger.info(String.format("*** Count: %d", packages.length));
            counts.add(packages.length);
            packagesByCount.put(packages.length, packages);
        }
        logger.info(String.format("***** First count: %s", counts.first()));
        logger.info(String.format("***** Last count: %s", counts.last()));
        return packagesByCount.get(counts.first());
    }

    private String[] executeCommand(final String commandString) throws IOException, InterruptedException, HubIntegrationException {
        final List<String> commandStringList = Arrays.asList(commandString.split(" "));
        final ProcessBuilder builder = new ProcessBuilder();
        builder.command(commandStringList.toArray(new String[commandStringList.size()]));
        builder.directory(new File("."));
        final Process process = builder.start();
        final int errCode = process.waitFor();
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
