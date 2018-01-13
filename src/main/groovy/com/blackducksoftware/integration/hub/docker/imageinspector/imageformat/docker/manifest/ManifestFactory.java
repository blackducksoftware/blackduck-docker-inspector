package com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.manifest;

import java.io.File;

public interface ManifestFactory {
    Manifest createManifest(final File tarExtractionDirectory, final String dockerTarFileName);
}
