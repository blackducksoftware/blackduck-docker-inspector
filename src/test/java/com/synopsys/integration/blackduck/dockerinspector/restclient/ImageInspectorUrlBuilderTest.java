package com.synopsys.integration.blackduck.dockerinspector.restclient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

import com.synopsys.integration.exception.IntegrationException;

public class ImageInspectorUrlBuilderTest {

    @Test
    public void testIncomplete() {
        final ImageInspectorUrlBuilder builder = new ImageInspectorUrlBuilder();
        try {
            builder.build();
            fail("Expected exception");
        } catch (final IntegrationException e) {
            // expected
        }
    }

    @Test
    public void testComplete() throws IntegrationException, URISyntaxException {
        final String url = new ImageInspectorUrlBuilder()
                .setImageInspectorUri(new URI("https://www.google.com"))
                .setContainerPathToTarfile("test_containerPathToTarfile")
                .setGivenImageRepo("test_givenImageRepo")
                .setGivenImageTag("test_givenImageTag")
                .setContainerPathToContainerFileSystemFile("test_containerPathToContainerFileSystemFile")
                .setCleanup(true)
                .setForgeDerivedFromDistro(true)
                .build();
        assertEquals(
                "https://www.google.com/getbdio?logginglevel=INFO&tarfile=test_containerPathToTarfile&cleanup=true&resultingcontainerfspath=test_containerPathToContainerFileSystemFile&imagerepo=test_givenImageRepo&imagetag=test_givenImageTag&forgederivedfromdistro=true",
                url);
    }
}
