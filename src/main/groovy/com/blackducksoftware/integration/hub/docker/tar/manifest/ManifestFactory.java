package com.blackducksoftware.integration.hub.docker.tar.manifest;

import java.io.File;

public interface ManifestFactory {
    Manifest createManifest(final String dockerImageName, final String dockerTagName, final File tarExtractionDirectory, final String dockerTarFileName);
}
