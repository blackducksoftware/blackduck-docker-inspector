package com.blackducksoftware.integration.hub.docker.tar.manifest;

import java.io.File;

import org.springframework.stereotype.Component;

@Component
public class AutowiredManifestFactory implements ManifestFactory {

    public AutowiredManifestFactory() {
        System.out.println("AutowiredManifestFactory()");
    }

    @Override
    public Manifest createManifest(final String dockerImageName, final String dockerTagName, final File tarExtractionDirectory, final String dockerTarFileName) {
        final Manifest manifest = new Manifest(dockerImageName, dockerTagName, tarExtractionDirectory, dockerTarFileName);
        final ManifestLayerMappingFactory factory = new AutowiredManifestLayerMappingFactory();
        manifest.setManifestLayerMappingFactory(factory);
        return manifest;
    }

}
