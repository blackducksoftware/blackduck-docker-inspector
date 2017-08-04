package com.blackducksoftware.integration.hub.docker.tar.manifest;

import java.io.File;

import org.springframework.stereotype.Component;

@Component
public class AutowiredManifestFactory implements ManifestFactory {

    @Override
    public Manifest createManifest(final String dockerImageName, final String dockerTagName, final File tarExtractionDirectory, final String dockerTarFileName) {
        return new Manifest(dockerImageName, dockerTagName, tarExtractionDirectory, dockerTarFileName);
    }

}
