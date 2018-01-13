package com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.manifest;

import java.util.List;

public class HardwiredManifestLayerMappingFactory implements ManifestLayerMappingFactory {

    @Override
    public ManifestLayerMapping createManifestLayerMapping(final String imageName, final String tagName, final List<String> layers) {
        final ManifestLayerMapping mapping = new ManifestLayerMapping(imageName, tagName, layers);
        return mapping;
    }

}
