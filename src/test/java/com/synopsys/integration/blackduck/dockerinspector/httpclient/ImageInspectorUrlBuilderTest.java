package com.synopsys.integration.blackduck.dockerinspector.httpclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

import com.synopsys.integration.exception.IntegrationException;

public class ImageInspectorUrlBuilderTest {

    @Test
    public void testIncomplete() {
        ImageInspectorUrlBuilder builder = new ImageInspectorUrlBuilder();
        try {
            builder.build();
            fail("Expected exception");
        } catch (IntegrationException e) {
            // expected
        }
    }

    @Test
    public void testComplete() throws IntegrationException, URISyntaxException {
        String url = new ImageInspectorUrlBuilder()
            .imageInspectorUri(new URI("https://www.google.com"))
            .containerPathToTarfile("test_containerPathToTarfile")
            .givenImageRepo("test_givenImageRepo")
            .givenImageTag("test_givenImageTag")
            .containerPathToContainerFileSystemFile("test_containerPathToContainerFileSystemFile")
            .organizeComponentsByLayer(false)
            .includeRemovedComponents(false)
            .cleanup(true)
            .build();
        assertEquals(
            "https://www.google.com/getbdio?logginglevel=INFO&tarfile=test_containerPathToTarfile&organizecomponentsbylayer=false&includeremovedcomponents=false&cleanup=true&resultingcontainerfspath=test_containerPathToContainerFileSystemFile&imagerepo=test_givenImageRepo&imagetag=test_givenImageTag",
            url
        );
    }
}
