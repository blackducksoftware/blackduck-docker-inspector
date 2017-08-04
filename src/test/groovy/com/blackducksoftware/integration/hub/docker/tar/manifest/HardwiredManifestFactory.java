package com.blackducksoftware.integration.hub.docker.tar.manifest;

import java.io.File;

public class HardwiredManifestFactory implements ManifestFactory {

    @Override
    public Manifest createManifest(final String dockerImageName, final String dockerTagName, final File tarExtractionDirectory, final String dockerTarFileName) {
        final Manifest manifest = new Manifest(dockerImageName, dockerTagName, tarExtractionDirectory, dockerTarFileName);
        manifest.manifestLayerMappingFactory = new HardwiredManifestLayerMappingFactory();
        return manifest;
    }

}
