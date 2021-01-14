/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2021 Synopsys, Inc.
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
package com.synopsys.integration.blackduck.dockerinspector.httpclient;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.ProgramPaths;
import com.synopsys.integration.blackduck.dockerinspector.output.ImageTarWrapper;
import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.client.IntHttpClient;

public abstract class ImageInspectorClient {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public ImageTarWrapper copyTarfileToSharedDir(Config config, ProgramPaths programPaths, ImageTarWrapper givenDockerTarfile) throws IOException {
        if (fileIsInsideDir(new File(config.getSharedDirPathLocal()), givenDockerTarfile.getFile())) {
            logger.debug(String.format("File %s is already inside shared dir %s; leaving it there",
                givenDockerTarfile.getFile().getCanonicalPath(),
                config.getSharedDirPathLocal()));
            return givenDockerTarfile;
        }
        // Copy the tarfile to the shared/target dir
        logger.debug(String.format("File %s is not inside shared dir %s; copying it there",
            givenDockerTarfile.getFile().getCanonicalPath(),
            config.getSharedDirPathLocal()));
        File finalDockerTarfile = new File(programPaths.getDockerInspectorTargetDirPath(), givenDockerTarfile.getFile().getName());
        logger.debug(String.format("Required docker tarfile location: %s", finalDockerTarfile.getCanonicalPath()));
        if (!finalDockerTarfile.getCanonicalPath().equals(givenDockerTarfile.getFile().getCanonicalPath())) {
            logger.debug(String.format("Copying %s to %s", givenDockerTarfile.getFile().getCanonicalPath(), finalDockerTarfile.getCanonicalPath()));
            FileUtils.copyFile(givenDockerTarfile.getFile(), finalDockerTarfile);
        }
        logger.debug(String.format("Final docker tar file path: %s", finalDockerTarfile.getCanonicalPath()));
        return new ImageTarWrapper(finalDockerTarfile, givenDockerTarfile.getImageRepo(), givenDockerTarfile.getImageTag());
    }

    private boolean fileIsInsideDir(File dir, File file) {
        Path sharedDirPathObj = dir.toPath();
        Path givenFilePathObj = file.toPath();
        if (givenFilePathObj.startsWith(sharedDirPathObj)) {
            return true;
        }
        return false;
    }

    public abstract String getBdio(String hostPathToTarFile, String containerPathToInputDockerTarfile, String givenImageRepo, String givenImageTag,
        String containerPathToOutputFileSystemFile, String containerFileSystemExcludedPaths,
        boolean organizeComponentsByLayer, boolean includeRemovedComponents, boolean cleanup,
        String platformTopLayerId, String targetLinuxDistro)
        throws IntegrationException, IOException, InterruptedException;

    public abstract boolean isApplicable();

    protected void checkServiceVersion(ProgramVersion programVersion, ImageInspectorServices imageInspectorServices, IntHttpClient httpClient, URI imageInspectorUri) {
        String serviceVersion = imageInspectorServices.getServiceVersion(httpClient, imageInspectorUri);
        logger.info(String.format("Image Inspector Service version: %s", serviceVersion));
        String expectedServiceVersion = programVersion.getInspectorImageVersion();
        if (!serviceVersion.equals(expectedServiceVersion)) {
            logger.warn(String.format(
                "Expected image inspector service version %s, but the running image inspector service is version %s; This version of Docker Inspector is designed to work with image inspector service version %s. Please stop and remove all running image inspector containers.",
                expectedServiceVersion, serviceVersion, expectedServiceVersion));
        }
    }
}
