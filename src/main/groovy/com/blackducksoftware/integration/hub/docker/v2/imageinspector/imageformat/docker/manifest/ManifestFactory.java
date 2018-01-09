package com.blackducksoftware.integration.hub.docker.v2.imageinspector.imageformat.docker.manifest;

import java.io.File;

public interface ManifestFactory {
    Manifest createManifest(final File tarExtractionDirectory, final String dockerTarFileName);
}
