package com.blackducksoftware.integration.hub.docker.tar.manifest;

import java.util.List;

public interface ManifestLayerMappingFactory {
    ManifestLayerMapping createManifestLayerMapping(final String imageName, final String tagName, final List<String> layers);
}
