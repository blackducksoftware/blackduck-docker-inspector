package com.synopsys.integration.blackduck.dockerinspector.restclient;

import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.exception.IntegrationException;

public class ImageInspectorUrlBuilder {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String GETBDIO_ENDPOINT = "getbdio";
    private static final String LOGGING_LEVEL_QUERY_PARAM = "logginglevel";
    private static final String CLEANUP_QUERY_PARAM = "cleanup";
    private static final String FORGE_DERIVED_FROM_DISTRO_QUERY_PARAM = "forgederivedfromdistro";
    private static final String RESULTING_CONTAINER_FS_PATH_QUERY_PARAM = "resultingcontainerfspath";
    private static final String IMAGE_REPO_QUERY_PARAM = "imagerepo";
    private static final String IMAGE_TAG_QUERY_PARAM = "imagetag";
    private static final String TARFILE_QUERY_PARAM = "tarfile";
    private static final String BASE_LOGGER_NAME = "com.synopsys";

    private URI imageInspectorUri = null;
    private String containerPathToTarfile = null;
    private String givenImageRepo = null;
    private String givenImageTag = null;
    private String containerPathToContainerFileSystemFile = null;
    private boolean cleanup = true;
    private boolean forgeDerivedFromDistro = false;

    public ImageInspectorUrlBuilder setImageInspectorUri(final URI imageInspectorUri) {
        this.imageInspectorUri = imageInspectorUri;
        return this;
    }

    public ImageInspectorUrlBuilder setContainerPathToTarfile(final String containerPathToTarfile) {
        this.containerPathToTarfile = containerPathToTarfile;
        return this;
    }

    public ImageInspectorUrlBuilder setGivenImageRepo(final String givenImageRepo) {
        this.givenImageRepo = givenImageRepo;
        return this;
    }

    public ImageInspectorUrlBuilder setGivenImageTag(final String givenImageTag) {
        this.givenImageTag = givenImageTag;
        return this;
    }

    public ImageInspectorUrlBuilder setContainerPathToContainerFileSystemFile(final String containerPathToContainerFileSystemFile) {
        this.containerPathToContainerFileSystemFile = containerPathToContainerFileSystemFile;
        return this;
    }

    public ImageInspectorUrlBuilder setCleanup(final boolean cleanup) {
        this.cleanup = cleanup;
        return this;
    }

    public ImageInspectorUrlBuilder setForgeDerivedFromDistro(final boolean forgeDerivedFromDistro) {
        this.forgeDerivedFromDistro = forgeDerivedFromDistro;
        return this;
    }

    public String build() throws IntegrationException {
        if (imageInspectorUri == null) {
            throw new IntegrationException("imageInspectorUri not specified");
        }
        if (containerPathToTarfile == null) {
            throw new IntegrationException("imageInspectorUri not specified");
        }
        final StringBuilder urlSb = new StringBuilder();
        urlSb.append(imageInspectorUri.toString());
        urlSb.append("/");
        urlSb.append(GETBDIO_ENDPOINT);
        urlSb.append("?");
        urlSb.append(String.format("%s=%s", LOGGING_LEVEL_QUERY_PARAM, getLoggingLevel()));
        urlSb.append(String.format("&%s=%s", TARFILE_QUERY_PARAM, containerPathToTarfile));
        urlSb.append(String.format("&%s=%b", CLEANUP_QUERY_PARAM, cleanup));
        if (StringUtils.isNotBlank(containerPathToContainerFileSystemFile)) {
            urlSb.append(String.format("&%s=%s", RESULTING_CONTAINER_FS_PATH_QUERY_PARAM, containerPathToContainerFileSystemFile));
        }
        if (StringUtils.isNotBlank(givenImageRepo)) {
            urlSb.append(String.format("&%s=%s", IMAGE_REPO_QUERY_PARAM, givenImageRepo));
        }
        if (StringUtils.isNotBlank(givenImageTag)) {
            urlSb.append(String.format("&%s=%s", IMAGE_TAG_QUERY_PARAM, givenImageTag));
        }
        if (forgeDerivedFromDistro) {
            urlSb.append(String.format("&%s=true", FORGE_DERIVED_FROM_DISTRO_QUERY_PARAM));
        }
        final String url = urlSb.toString();
        return url;
    }

    private String getLoggingLevel() {
        String loggingLevel = "INFO";
        try {
            final ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(BASE_LOGGER_NAME);
            loggingLevel = root.getLevel().toString();
            logger.debug(String.format("Logging level: %s", loggingLevel));
        } catch (final Exception e) {
            logger.debug(String.format("No logging level set. Defaulting to %s", loggingLevel));
        }
        return loggingLevel;
    }
}
