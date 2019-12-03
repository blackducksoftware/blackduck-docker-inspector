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
package com.synopsys.integration.blackduck.dockerinspector.output;

import java.io.File;

import com.synopsys.integration.util.Stringable;

public class Result extends Stringable {
    private final Boolean succeeded;
    private final String message;
    private final String imageRepo;
    private final String imageTag;
    // these filenames are just the filename, not the full path:
    private final String dockerTarfilename;
    private final String bdioFilename;
    private final String containerFilesystemFilename;
    private final String squashedImageFilename;

    public static Result createResultFailure(final String message) {
        return new Result(false, message, "unknown", "unknwon", "unknown","none", "none", "none");
    }

    public static Result createResultSuccess(final String imageRepo, final String imageTag, final String dockerTarfilename, final File bdioFile, final File containerFilesystemFile, final File squashedImageFile) {
        final String bdioFilename = bdioFile == null ? "" : bdioFile.getName();
        final String containerFilesystemFilename = containerFilesystemFile == null ? "" : containerFilesystemFile.getName();
        final String squashedImageFilename = squashedImageFile == null ? "" : squashedImageFile.getName();
        return new Result(true, "Docker Inspector succeeded.", imageRepo, imageTag, dockerTarfilename, bdioFilename, containerFilesystemFilename, squashedImageFilename);
    }

    private Result(final Boolean succeeded, final String message, final String imageRepo, final String imageTag, final String dockerTarfilename, final String bdioFilename,
        final String containerFilesystemFilename, final String squashedImageFilename) {
        this.succeeded = succeeded;
        this.message = message;
        this.imageRepo = imageRepo;
        this.imageTag = imageTag;
        this.dockerTarfilename = dockerTarfilename == null ? "" : dockerTarfilename;
        this.bdioFilename = bdioFilename;
        this.containerFilesystemFilename = containerFilesystemFilename;
        this.squashedImageFilename = squashedImageFilename;
    }

    public boolean isSucceeded() {
        return succeeded;
    }

    public String getMessage() {
        return message;
    }

    public String getImageRepo() {
        return imageRepo;
    }

    public String getImageTag() {
        return imageTag;
    }

    public String getDockerTarfilename() {
        return dockerTarfilename;
    }

    public String getBdioFilename() {
        return bdioFilename;
    }

    public String getContainerFilesystemFilename() {
        return containerFilesystemFilename;
    }

    public String getSquashedImageFilename() {
        return squashedImageFilename;
    }

    public int getReturnCode() {
        return isSucceeded() ? 0 : -1;
    }
}
