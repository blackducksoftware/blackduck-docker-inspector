/**
 * hub-docker-inspector
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
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
package com.blackducksoftware.integration.hub.docker.imageinspector.result;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.docker.imageinspector.lib.OperatingSystemEnum;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.google.gson.Gson;

@Component
public class ResultFile {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public Result read(final Gson gson, final String resultFilePath) throws HubIntegrationException {
        Result result = null;
        try {
            final File resultFile = new File(resultFilePath);
            final String resultFileContent = FileUtils.readFileToString(resultFile, "UTF8");
            result = gson.fromJson(resultFileContent, Result.class);
        } catch (final IOException e) {
            throw new HubIntegrationException(String.format("Error reading result file %s: %s", resultFilePath, e.getMessage()));
        }
        if (result == null) {
            throw new HubIntegrationException(String.format("Error reading result file %s: result object is null", resultFilePath));
        }
        return result;
    }

    public void write(final Gson gson, final String resultFilePath, final boolean succeeded, final String msg, final OperatingSystemEnum targetOs, final String runOnImageName, final String runOnImageTag, final String dockerTarfilename,
            final String bdioFilename) {
        final String runOnOsName = targetOs == null ? "" : targetOs.name().toLowerCase();
        final Result result = new Result(succeeded, msg, runOnOsName, runOnImageName, runOnImageTag, dockerTarfilename, bdioFilename);
        try {
            final File resultOutputFile = new File(resultFilePath);
            resultOutputFile.getParentFile().mkdirs();
            try (FileOutputStream resultOutputStream = new FileOutputStream(resultOutputFile)) {
                try (ResultWriter resultWriter = new ResultWriter(gson, resultOutputStream)) {
                    resultWriter.writeResult(result);
                }
            }
        } catch (final Exception e) {
            logger.error(String.format("Error writing result file: %s", e.getMessage()));
        }
    }
}
