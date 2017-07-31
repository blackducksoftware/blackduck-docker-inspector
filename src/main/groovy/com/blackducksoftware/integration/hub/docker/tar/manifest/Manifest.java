package com.blackducksoftware.integration.hub.docker.tar.manifest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class Manifest {
    private final Logger logger = LoggerFactory.getLogger(Manifest.class);
    private final File tarExtractionDirectory;

    public Manifest(final File tarExtractionDirectory) {
        this.tarExtractionDirectory = tarExtractionDirectory;
    }

    public List<ImageInfo> getManifestContents(final String tarFileName) throws IOException {
        logger.info("*** getManifestContents()");
        final List<ImageInfo> images = new ArrayList<>();
        logger.debug("getManifestContents(): extracting manifest file content");
        final String manifestContentString = extractManifestFileContent(tarFileName);
        logger.debug("getManifestContents(): parsing: ${manifestContentString}");
        final JsonParser parser = new JsonParser();
        final JsonArray manifestContent = parser.parse(manifestContentString).getAsJsonArray();
        final Gson gson = new Gson();
        for (final JsonElement element : manifestContent) {
            logger.debug("getManifestContents(): element: ${element.toString()}");
            images.add(gson.fromJson(element, ImageInfo.class));
        }
        return images;
    }

    private String extractManifestFileContent(final String dockerTarName) throws IOException {
        final File dockerTarDirectory = new File(tarExtractionDirectory, dockerTarName);
        final File manifest = new File(dockerTarDirectory, "manifest.json");
        final String manifestFileContents = StringUtils.join(FileUtils.readLines(manifest, StandardCharsets.UTF_8), "\n");
        return manifestFileContents;
    }
}
