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
package com.blackducksoftware.integration.hub.docker;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.docker.extractor.Extractor;
import com.blackducksoftware.integration.hub.docker.tar.ImagePkgMgr;

@Component
public class PackageManagerFiles {
    private final Logger logger = LoggerFactory.getLogger(Extractor.class);

    public void stubPackageManagerFiles(final ImagePkgMgr imagePkgMgr) throws IOException {
        final File packageManagerDirectory = new File(imagePkgMgr.getPackageManager().getDirectory());
        if (packageManagerDirectory.exists()) {
            deleteFilesOnly(packageManagerDirectory);
            if (imagePkgMgr.getPackageManager() == PackageManagerEnum.DPKG) {
                final File statusFile = new File(packageManagerDirectory, "status");
                statusFile.createNewFile();
                final File updatesDir = new File(packageManagerDirectory, "updates");
                updatesDir.mkdir();
            }
        }
        logger.debug(String.format("Copying %s to %s", imagePkgMgr.getExtractedPackageManagerDirectory().getAbsolutePath(), packageManagerDirectory.getAbsolutePath()));
        FileUtils.copyDirectory(imagePkgMgr.getExtractedPackageManagerDirectory(), packageManagerDirectory);
    }

    private void deleteFilesOnly(final File file) {
        if (file.isDirectory()) {
            for (final File subFile : file.listFiles()) {
                deleteFilesOnly(subFile);
            }
        } else {
            file.delete();
        }
    }
}
