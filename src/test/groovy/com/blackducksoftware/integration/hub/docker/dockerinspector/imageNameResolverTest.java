package com.blackducksoftware.integration.hub.docker.dockerinspector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.blackducksoftware.integration.hub.docker.imageinspector.name.ImageNameResolver;

public class imageNameResolverTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testNone() {
        final ImageNameResolver resolver = new ImageNameResolver("");
        assertFalse(resolver.getNewImageRepo().isPresent());
        assertFalse(resolver.getNewImageTag().isPresent());
    }

    @Test
    public void testRepoOnly() {
        final ImageNameResolver resolver = new ImageNameResolver("alpine");
        assertTrue(resolver.getNewImageRepo().isPresent());
        assertTrue(resolver.getNewImageTag().isPresent());
        assertEquals("alpine", resolver.getNewImageRepo().get());
        assertEquals("latest", resolver.getNewImageTag().get());
    }

    @Test
    public void testBoth() {
        final ImageNameResolver resolver = new ImageNameResolver("alpine:1.0");
        assertTrue(resolver.getNewImageRepo().isPresent());
        assertTrue(resolver.getNewImageTag().isPresent());
        assertEquals("alpine", resolver.getNewImageRepo().get());
        assertEquals("1.0", resolver.getNewImageTag().get());
    }

    @Test
    public void testBothColonInRepoSpecifier() {
        final ImageNameResolver resolver = new ImageNameResolver("artifactoryserver:5000/alpine:1.0");
        assertTrue(resolver.getNewImageRepo().isPresent());
        assertTrue(resolver.getNewImageTag().isPresent());
        assertEquals("artifactoryserver:5000/alpine", resolver.getNewImageRepo().get());
        assertEquals("1.0", resolver.getNewImageTag().get());
    }

    @Test
    public void testEndsWithColon() {
        final ImageNameResolver resolver = new ImageNameResolver("alpine:");
        assertTrue(resolver.getNewImageRepo().isPresent());
        assertTrue(resolver.getNewImageTag().isPresent());
        assertEquals("alpine", resolver.getNewImageRepo().get());
        assertEquals("latest", resolver.getNewImageTag().get());
    }

}
