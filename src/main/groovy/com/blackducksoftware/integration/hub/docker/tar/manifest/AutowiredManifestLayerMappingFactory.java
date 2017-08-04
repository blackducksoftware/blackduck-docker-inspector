package com.blackducksoftware.integration.hub.docker.tar.manifest;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class AutowiredManifestLayerMappingFactory implements ManifestLayerMappingFactory {

    @Override
    public ManifestLayerMapping createManifestLayerMapping(final String imageName, final String tagName, final List<String> layers) {
        return new ManifestLayerMapping(imageName, tagName, layers);
    }

}
