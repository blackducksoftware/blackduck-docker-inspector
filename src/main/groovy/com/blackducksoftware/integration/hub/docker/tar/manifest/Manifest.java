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

import com.blackducksoftware.integration.hub.docker.tar.LayerMapping;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class Manifest {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String dockerImageName;
    private final String dockerTagName;
    private final File tarExtractionDirectory;
    private final String dockerTarFileName;

    public Manifest(final String dockerImageName, final String dockerTagName, final File tarExtractionDirectory, final String dockerTarFileName) {
        this.dockerImageName = dockerImageName;
        this.dockerTagName = dockerTagName;
        this.tarExtractionDirectory = tarExtractionDirectory;
        this.dockerTarFileName = dockerTarFileName;
    }

    public List<LayerMapping> getLayerMappings() throws HubIntegrationException, IOException {
        final List<LayerMapping> mappings = new ArrayList<>();
        final List<ImageInfo> images = getManifestContents();
        for (final ImageInfo image : images) {

            logger.debug("getLayerMappings(): image: ${image}");

            String specifiedRepoTag = "";
            if (StringUtils.isNotBlank(dockerImageName)) {
                specifiedRepoTag = "${dockerImageName}:${dockerTagName}";
            }
            String imageName = "";
            String tagName = "";
            String foundRepoTag = null;

            for (final String repoTag : image.repoTags) {
                if (StringUtils.compare(repoTag, specifiedRepoTag) == 0) {
                    foundRepoTag = repoTag;
                }
            }

            if (StringUtils.isBlank(foundRepoTag)) {
                logger.debug("Attempting to parse repoTag from manifest");
                if (image.repoTags == null) {
                    final String msg = "The RepoTags field is missing from the tar file manifest. Please make sure this tar file was saved using the image name (vs. image ID)";
                    throw new HubIntegrationException(msg);
                }
                final String repoTag = image.repoTags.get(0);
                logger.debug("repoTag: ${repoTag}");
                imageName = repoTag.substring(0, repoTag.lastIndexOf(':'));
                tagName = repoTag.substring(repoTag.lastIndexOf(':') + 1);
                logger.debug("Parsed imageName: ${imageName}; tagName: ${tagName}");
            } else {
                logger.debug("foundRepoTag: ${foundRepoTag}");
                imageName = foundRepoTag.substring(0, foundRepoTag.lastIndexOf(':'));
                tagName = foundRepoTag.substring(foundRepoTag.lastIndexOf(':') + 1);
                logger.debug("Found imageName: ${imageName}; tagName: ${tagName}");
            }
            logger.info("Image: ${imageName}, Tag: ${tagName}");
            final List<String> layerIds = new ArrayList<>();
            for (final String layer : image.layers) {
                layerIds.add(layer.substring(0, layer.indexOf('/')));
            }
            final LayerMapping mapping = new LayerMapping(imageName.replaceAll(":", "_").replaceAll("/", "_"), tagName, layerIds);
            if (StringUtils.isNotBlank(dockerImageName)) {
                if (StringUtils.compare(imageName, dockerImageName) == 0 && StringUtils.compare(tagName, dockerTagName) == 0) {
                    logger.debug("Adding layer mapping");
                    logger.debug("Image: ${mapping.imageName}:${mapping.tagName}");
                    logger.debug("Layers: ${mapping.layers}");
                    mappings.add(mapping);
                }
            } else {
                logger.debug("Adding layer mapping");
                logger.debug("Image ${mapping.imageName} , Tag ${mapping.tagName}");
                logger.debug("Layers ${mapping.layers}");
                mappings.add(mapping);
            }
        }
        return mappings;
    }

    private List<ImageInfo> getManifestContents() throws IOException {
        logger.trace("getManifestContents()");
        final List<ImageInfo> images = new ArrayList<>();
        logger.debug("getManifestContents(): extracting manifest file content");
        final String manifestContentString = extractManifestFileContent(dockerTarFileName);
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
