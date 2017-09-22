package com.blackducksoftware.integration.hub.docker.tar.manifest;

import java.util.List;

import com.blackducksoftware.integration.hub.docker.ProgramPaths;

public class HardwiredManifestLayerMappingFactory implements ManifestLayerMappingFactory {

    @Override
    public ManifestLayerMapping createManifestLayerMapping(final String imageName, final String tagName, final List<String> layers) {
        final ManifestLayerMapping mapping = new ManifestLayerMapping(imageName, tagName, layers);
        mapping.setProgramPaths(new ProgramPaths());
        return mapping;
    }

}
