package com.blackducksoftware.integration.hub.docker.tar.manifest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private ManifestLayerMappingFactory manifestLayerMappingFactory;

    public Manifest(final String dockerImageName, final String dockerTagName, final File tarExtractionDirectory, final String dockerTarFileName) {
        this.dockerImageName = dockerImageName;
        this.dockerTagName = dockerTagName;
        this.tarExtractionDirectory = tarExtractionDirectory;
        this.dockerTarFileName = dockerTarFileName;
    }

    public void setManifestLayerMappingFactory(final ManifestLayerMappingFactory manifestLayerMappingFactory) {
        this.manifestLayerMappingFactory = manifestLayerMappingFactory;
    }

    public List<ManifestLayerMapping> getLayerMappings() throws HubIntegrationException, IOException {
        final List<ManifestLayerMapping> mappings = new ArrayList<>();
        final List<ImageInfo> images = getManifestContents();
        for (final ImageInfo image : images) {

            logger.debug(String.format("getLayerMappings(): image: %s", image));

            String specifiedRepoTag = "";
            if (StringUtils.isNotBlank(dockerImageName)) {
                specifiedRepoTag = String.format("%s:%s", dockerImageName, dockerTagName);
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
                logger.debug(String.format("repoTag: %s", repoTag));
                imageName = repoTag.substring(0, repoTag.lastIndexOf(':'));
                tagName = repoTag.substring(repoTag.lastIndexOf(':') + 1);
                logger.debug(String.format("Parsed imageName: %s; tagName: %s", imageName, tagName));
            } else {
                logger.debug(String.format("foundRepoTag: %s", foundRepoTag));
                imageName = foundRepoTag.substring(0, foundRepoTag.lastIndexOf(':'));
                tagName = foundRepoTag.substring(foundRepoTag.lastIndexOf(':') + 1);
                logger.debug(String.format("Found imageName: %s; tagName: %s", imageName, tagName));
            }
            logger.info(String.format("Image: %s, Tag: %s", imageName, tagName));
            final List<String> layerIds = new ArrayList<>();
            for (final String layer : image.layers) {
                layerIds.add(layer.substring(0, layer.indexOf('/')));
            }
            final ManifestLayerMapping mapping = manifestLayerMappingFactory.createManifestLayerMapping(imageName, tagName, layerIds);
            if (StringUtils.isNotBlank(dockerImageName)) {
                if (StringUtils.compare(imageName, dockerImageName) == 0 && StringUtils.compare(tagName, dockerTagName) == 0) {
                    logger.debug("Adding layer mapping");
                    logger.debug(String.format("Image: %s:%s", mapping.getImageName(), mapping.getTagName()));
                    logger.debug(String.format("Layers: %s", mapping.getLayers()));
                    mappings.add(mapping);
                }
            } else {
                logger.debug("Adding layer mapping");
                logger.debug(String.format("Image %s , Tag %s", mapping.getImageName(), mapping.getTagName()));
                logger.debug(String.format("Layers %s", mapping.getLayers()));
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
        logger.debug(String.format("getManifestContents(): parsing: %s", manifestContentString));
        final JsonParser parser = new JsonParser();
        final JsonArray manifestContent = parser.parse(manifestContentString).getAsJsonArray();
        final Gson gson = new Gson();
        for (final JsonElement element : manifestContent) {
            logger.debug(String.format("getManifestContents(): element: %s", element.toString()));
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

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, RecursiveToStringStyle.JSON_STYLE);
    }
}
