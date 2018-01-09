package com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.manifest;

import java.io.File;

import org.springframework.stereotype.Component;

@Component
public class AutowiredManifestFactory implements ManifestFactory {

    public AutowiredManifestFactory() {
        System.out.println("AutowiredManifestFactory()");
    }

    @Override
    public Manifest createManifest(final File tarExtractionDirectory, final String dockerTarFileName) {
        final Manifest manifest = new Manifest(tarExtractionDirectory, dockerTarFileName);
        final ManifestLayerMappingFactory factory = new AutowiredManifestLayerMappingFactory();
        manifest.setManifestLayerMappingFactory(factory);
        return manifest;
    }

}
