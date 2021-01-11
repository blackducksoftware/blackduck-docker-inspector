package com.synopsys.integration.blackduck.dockerinspector.dockerclient;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.ProgramPaths;
import com.synopsys.integration.blackduck.dockerinspector.output.ImageTarFilename;
import com.synopsys.integration.blackduck.dockerinspector.output.ImageTarWrapper;
import com.synopsys.integration.blackduck.exception.BlackDuckIntegrationException;
import com.synopsys.integration.exception.IntegrationException;

@Tag("integration")
public class DockerClientManagerTest {
    private final static String imageRepo = "dockerclientmanagertest";
    private final static String imageTag = "dockerclientmanagertest";

    private static DockerClientManager dockerClientManager;
    private static Config config;
    private static ProgramPaths programPaths;

    @BeforeAll
    public static void setUp() {
        config = Mockito.mock(Config.class);
        programPaths = Mockito.mock(ProgramPaths.class);
        dockerClientManager = new DockerClientManager(config, new ImageTarFilename(), programPaths);
    }

    @AfterAll
    public static void tearDown() {
        final Optional<String> foundImageIdInitial = dockerClientManager.lookupImageIdByRepoTag(imageRepo, imageTag);
        if (foundImageIdInitial.isPresent()) {
            dockerClientManager.removeImage(foundImageIdInitial.get());
        }
    }

    @Test
    public void test() throws IOException {

        final Optional<String> foundImageIdInitial = dockerClientManager.lookupImageIdByRepoTag(imageRepo, imageTag);
        if (foundImageIdInitial.isPresent()) {
            dockerClientManager.removeImage(foundImageIdInitial.get());
        }
        final Optional<String> foundImageIdShouldBeEmpty = dockerClientManager.lookupImageIdByRepoTag(imageRepo, imageTag);
        assertFalse(foundImageIdShouldBeEmpty.isPresent());

        final File testWorkingDir = new File("test/output/dockerClientManagerTest");
        testWorkingDir.mkdirs();
        final File dockerfile = new File(testWorkingDir, "Dockerfile");
        final File imageContents = new File(testWorkingDir, "test.txt");
        imageContents.createNewFile();
        final String dockerfileContents = String.format("FROM scratch\nCOPY test.txt .\n");
        FileUtils.writeStringToFile(dockerfile, dockerfileContents, StandardCharsets.UTF_8);

        final Set<String> tags = new HashSet<>();
        tags.add(String.format("%s:%s", imageRepo, imageTag));
        final String createdImageId = dockerClientManager.buildImage(testWorkingDir, tags);
        System.out.printf("Created image %s\n", createdImageId);

        final Optional<String> foundImageId = dockerClientManager.lookupImageIdByRepoTag(imageRepo, imageTag);

        assertTrue(foundImageId.isPresent());
        System.out.printf("Found image id: %s\n", foundImageId.get());
        assertTrue(foundImageId.get().startsWith("sha256:"));
    }

    @Test
    public void testDeriveDockerTarfileFromConfiguredTar() throws IOException, IntegrationException {
        Mockito.when(programPaths.getDockerInspectorTargetDirPath()).thenReturn("test/containerShared/target");
        Mockito.when(config.getDockerTar()).thenReturn("build/images/test/alpine.tar");
        final ImageTarWrapper imageTarWrapper = dockerClientManager.deriveDockerTarFileFromConfig();
        assertEquals("alpine.tar", imageTarWrapper.getFile().getName());
    }

    @Test
    public void testDeriveDockerTarfileFromConfiguredImage() throws IOException, IntegrationException {
        Mockito.when(programPaths.getDockerInspectorTargetDirPath()).thenReturn("test/containerShared/target");
        Mockito.when(config.getDockerImageRepo()).thenReturn("alpine");
        Mockito.when(config.getDockerImageTag()).thenReturn("latest");
        final ImageTarWrapper imageTarWrapper = dockerClientManager.deriveDockerTarFileFromConfig();
        assertEquals("alpine_latest.tar", imageTarWrapper.getFile().getName());
    }

    @Test
    public void testPullImageByDigest() throws InterruptedException, IntegrationException {
        String repo = "ubuntu";
        String tag = "20.04";
        String digest = "sha256:be2aa2178e05b3d1930b4192ba405cb1d260f6a573abab4a6e83e0ebec626cf1";
        try {
            dockerClientManager.pullImageByDigest(repo, tag, digest);
        } catch (BlackDuckIntegrationException e) {
            fail();
        }
    }
}
