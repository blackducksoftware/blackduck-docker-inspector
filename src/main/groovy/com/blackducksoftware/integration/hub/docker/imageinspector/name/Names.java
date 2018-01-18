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
package com.blackducksoftware.integration.hub.docker.imageinspector.name;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Names {
    private static final Logger logger = LoggerFactory.getLogger(Names.class);

    public static String getImageTarFilename(final String imageName, final String tagName) {
        return String.format("%s_%s.tar", cleanImageName(imageName), tagName);
    }

    public static String getTargetImageFileSystemRootDirName(final String imageName, final String imageTag) {
        return String.format("image_%s_v_%s", cleanImageName(imageName), imageTag);
    }

    public static String getCodeLocationName(final String codelocationPrefix, final String imageName, final String imageTag, final String pkgMgrFilePath, final String pkgMgrName) {
        if (!StringUtils.isBlank(codelocationPrefix)) {
            return String.format("%s_%s_%s_%s_%s", codelocationPrefix, cleanImageName(imageName), imageTag, slashesToUnderscore(pkgMgrFilePath), pkgMgrName);
        }
        return String.format("%s_%s_%s_%s", cleanImageName(imageName), imageTag, slashesToUnderscore(pkgMgrFilePath), pkgMgrName);
    }

    private static String slashesToUnderscore(final String givenString) {
        return givenString.replaceAll("/", "_");
    }

    public static String getBdioFilename(final String imageName, final String pkgMgrFilePath, final String hubProjectName, final String hubVersionName) {
        logger.debug(String.format("imageName: %s, pkgMgrFilePath: %s, hubProjectName: %s, hubVersionName: %s", imageName, pkgMgrFilePath, hubProjectName, hubVersionName));
        return createBdioFilename(cleanImageName(imageName), cleanPath(pkgMgrFilePath), cleanHubProjectName(hubProjectName), hubVersionName);
    }

    private static String createBdioFilename(final String cleanImageName, final String cleanPkgMgrFilePath, final String cleanHubProjectName, final String hubVersionName) {
        final String[] parts = new String[4];
        parts[0] = cleanImageName;
        parts[1] = cleanPkgMgrFilePath;
        parts[2] = cleanHubProjectName;
        parts[3] = hubVersionName;

        String filename = generateFilename(cleanImageName, cleanPkgMgrFilePath, cleanHubProjectName, hubVersionName);
        for (int i = 0; (filename.length() >= 255) && (i < 4); i++) {
            parts[i] = DigestUtils.sha1Hex(parts[i]);
            if (parts[i].length() > 15) {
                parts[i] = parts[i].substring(0, 15);
            }

            filename = generateFilename(parts[0], parts[1], parts[2], parts[3]);
        }
        return filename;
    }

    public static String getHubProjectNameFromImageName(final String imageName) {
        return cleanImageName(imageName);
    }

    private static String cleanImageName(final String imageName) {
        return colonsToUnderscores(slashesToUnderscore(imageName));
    }

    private static String cleanHubProjectName(final String hubProjectName) {
        return slashesToUnderscore(hubProjectName);
    }

    public static String getContainerFileSystemTarFilename(final String imageName, final String tagName) {
        return String.format("%s_%s_containerfilesystem.tar.gz", cleanImageName(imageName), tagName);
    }

    private static String colonsToUnderscores(final String imageName) {
        return imageName.replaceAll(":", "_");
    }

    private static String generateFilename(final String cleanImageName, final String cleanPkgMgrFilePath, final String cleanHubProjectName, final String hubVersionName) {
        return String.format("%s_%s_%s_%s_bdio.jsonld", cleanImageName, cleanPkgMgrFilePath, cleanHubProjectName, hubVersionName);
    }

    private static String cleanPath(final String path) {
        return slashesToUnderscore(path);
    }
}
