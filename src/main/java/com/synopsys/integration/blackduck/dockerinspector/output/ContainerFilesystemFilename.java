/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.output;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;

@Component
public class ContainerFilesystemFilename {
    private static final String CONTAINER_FILESYSTEM_IDENTIFIER = "containerfilesystem";
    private static final String APP_ONLY_HINT = "app";
    private static final String TWO_PART_STRING_FORMAT = "%s_%s";
    private static final String THREE_PART_STRING_FORMAT = "%s_%s_%s";

    private Config config;

    @Autowired
    public void setConfig(final Config config) {
        this.config = config;
    }

    public String deriveContainerFilesystemFilename(final String repo, final String tag) {
        final String containerFileSystemFilename;
        if (StringUtils.isBlank(config.getDockerPlatformTopLayerId())) {
            containerFileSystemFilename = getContainerFileSystemTarFilename(repo, tag, config.getDockerTar());
        } else {
            containerFileSystemFilename = getContainerFileSystemAppLayersTarFilename(repo, tag, config.getDockerTar());
        }
        return containerFileSystemFilename;
    }

    private String getContainerFileSystemTarFilename(final String repo, final String tag, final String tarPath) {
        return getContainerOutputTarFileNameUsingBase(CONTAINER_FILESYSTEM_IDENTIFIER, repo, tag, tarPath);
    }

    private String getContainerFileSystemAppLayersTarFilename(final String repo, final String tag, final String tarPath) {
        final String contentHint = String.format(TWO_PART_STRING_FORMAT, APP_ONLY_HINT, CONTAINER_FILESYSTEM_IDENTIFIER);
        return getContainerOutputTarFileNameUsingBase(contentHint, repo, tag, tarPath);
    }

    private static String getContainerOutputTarFileNameUsingBase(final String contentHint, final String repo, final String tag, final String tarPath) {
        final String containerFilesystemFilenameSuffix = String.format("%s.tar.gz", contentHint);
        if (StringUtils.isNotBlank(repo)) {
            return String.format(THREE_PART_STRING_FORMAT, slashesToUnderscore(repo), slashesToUnderscore(tag), containerFilesystemFilenameSuffix);
        } else {
            final File tarFile = new File(tarPath);
            final String tarFilename = tarFile.getName();
            if (tarFilename.contains(".")) {
                final int finalPeriodIndex = tarFilename.lastIndexOf('.');
                return String.format(TWO_PART_STRING_FORMAT, tarFilename.substring(0, finalPeriodIndex), containerFilesystemFilenameSuffix);
            }
            return String.format(TWO_PART_STRING_FORMAT, cleanImageName(tarFilename), containerFilesystemFilenameSuffix);
        }
    }

    private static String cleanImageName(final String imageName) {
        return colonsToUnderscores(slashesToUnderscore(imageName));
    }

    private static String colonsToUnderscores(final String imageName) {
        return imageName.replace(":", "_");
    }

    private static String slashesToUnderscore(final String givenString) {
        return givenString.replace("/", "_");
    }
}
