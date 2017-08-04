package com.blackducksoftware.integration.hub.docker.tar.manifest;

import java.util.List;

import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.docker.client.ProgramPaths;

@Component
public class AutowiredManifestLayerMappingFactory implements ManifestLayerMappingFactory {

    public AutowiredManifestLayerMappingFactory() {
        // TODO remove this comment
    }

    @Override
    public ManifestLayerMapping createManifestLayerMapping(final String imageName, final String tagName, final List<String> layers) {
        final ManifestLayerMapping mapping = new ManifestLayerMapping(imageName, tagName, layers);
        mapping.programPaths = new ProgramPaths();
        return mapping;
    }

}
