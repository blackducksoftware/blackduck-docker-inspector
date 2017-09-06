package com.blackducksoftware.integration.hub.docker.dependencynode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.blackducksoftware.integration.hub.bdio.simple.DependencyNodeBuilder;
import com.blackducksoftware.integration.hub.bdio.simple.model.DependencyNode;
import com.blackducksoftware.integration.hub.bdio.simple.model.Forge;
import com.blackducksoftware.integration.hub.bdio.simple.model.externalid.ArchitectureExternalId;
import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum;
import com.google.gson.Gson;

public class DependencyNodeWriterTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void test() throws IOException {
        final DependencyNode rootNode = createDependencyNode(OperatingSystemEnum.ALPINE.getForge(), "root", "1.0", "testArch");
        final DependencyNodeBuilder dNodeBuilder = new DependencyNodeBuilder(rootNode);
        for (int i = 1; i <= 3; i++) {
            final DependencyNode dNode = createDependencyNode(OperatingSystemEnum.ALPINE.getForge(), "pkg" + i, "version", "testArch");
            dNodeBuilder.addParentNodeWithChildren(rootNode, Arrays.asList(dNode));
        }
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final DependencyNodeWriter dNodeWriter = new DependencyNodeWriter(new Gson(), out);
        dNodeWriter.writeDependencyNode(rootNode);
        dNodeWriter.close();

        final String outString = out.toString();
        assertEquals(1210, outString.length());
        assertTrue(outString.contains("pkg1"));
        assertTrue(outString.contains("pkg2"));
        assertTrue(outString.contains("pkg3"));
        System.out.println(outString);
    }

    private DependencyNode createDependencyNode(final String forge, final String name, final String version, final String arch) {
        final Forge forgeObj = new Forge(forge, ":");
        final DependencyNode dNode = new DependencyNode(name, version, new ArchitectureExternalId(forgeObj, name, version, arch));
        return dNode;
    }
}
