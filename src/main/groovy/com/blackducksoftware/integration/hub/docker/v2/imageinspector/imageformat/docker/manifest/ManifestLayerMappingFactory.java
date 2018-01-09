package com.blackducksoftware.integration.hub.docker.v2.imageinspector.imageformat.docker.manifest;

import java.util.List;

public interface ManifestLayerMappingFactory {
    ManifestLayerMapping createManifestLayerMapping(final String imageName, final String tagName, final List<String> layers);
}
