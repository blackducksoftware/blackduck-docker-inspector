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

public class Result {
    private final Boolean succeeded;
    private final String message;
    private final String inspectOnOsName;
    private final String inspectOnImageName;
    private final String inspectOnImageTag;
    private final String dockerTarfilename;
    private final String bdioFilename;

    public Result(final Boolean succeeded, final String message, final String inspectOnOsName, final String inspectOnImageName, final String inspectOnImageTag, final String dockerTarfilename, final String bdioFilename) {
        this.succeeded = succeeded;
        this.message = message;
        this.inspectOnOsName = inspectOnOsName == null ? "" : inspectOnOsName;
        this.inspectOnImageName = inspectOnImageName == null ? "" : inspectOnImageName;
        this.inspectOnImageTag = inspectOnImageTag == null ? "" : inspectOnImageTag;
        this.dockerTarfilename = dockerTarfilename == null ? "" : dockerTarfilename;
        this.bdioFilename = bdioFilename == null ? "" : bdioFilename;
    }

    public boolean isSucceeded() {
        return succeeded;
    }

    public String getMessage() {
        return message;
    }

    public String getInspectOnOsName() {
        return inspectOnOsName;
    }

    public String getInspectOnImageName() {
        return inspectOnImageName;
    }

    public String getInspectOnImageTag() {
        return inspectOnImageTag;
    }

    public String getDockerTarfilename() {
        return dockerTarfilename;
    }

    public String getBdioFilename() {
        return bdioFilename;
    }
}
