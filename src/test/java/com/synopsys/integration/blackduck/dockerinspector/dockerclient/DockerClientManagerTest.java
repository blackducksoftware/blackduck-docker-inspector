package com.synopsys.integration.blackduck.dockerinspector.dockerclient;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.synopsys.integration.blackduck.imageinspector.api.name.ImageNameResolver;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.Mockito;

import com.github.dockerjava.api.exception.BadRequestException;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.ProgramPaths;
import com.synopsys.integration.blackduck.dockerinspector.output.ImageTarFilename;
import com.synopsys.integration.blackduck.dockerinspector.output.ImageTarWrapper;
import com.synopsys.integration.blackduck.exception.BlackDuckIntegrationException;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
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
        FileOperations fileOperations = new FileOperations();
        dockerClientManager = new DockerClientManager(fileOperations, new ImageNameResolver(), config, new ImageTarFilename(), programPaths);
    }

    @AfterAll
    public static void tearDown() {
        removeImage(imageRepo, imageTag);
    }

    private static void removeImage(String imageRepo, String imageTag) {
        Optional<String> foundImageIdInitial = dockerClientManager.lookupImageIdByRepoTag(imageRepo, imageTag);
        if (foundImageIdInitial.isPresent()) {
            dockerClientManager.removeImage(foundImageIdInitial.get());
        }
    }

    @Test
    public void test() throws IOException {

        Optional<String> foundImageIdInitial = dockerClientManager.lookupImageIdByRepoTag(imageRepo, imageTag);
        if (foundImageIdInitial.isPresent()) {
            dockerClientManager.removeImage(foundImageIdInitial.get());
        }
        Optional<String> foundImageIdShouldBeEmpty = dockerClientManager.lookupImageIdByRepoTag(imageRepo, imageTag);
        assertFalse(foundImageIdShouldBeEmpty.isPresent());

        File testWorkingDir = new File("test/output/dockerClientManagerTest");
        testWorkingDir.mkdirs();
        File dockerfile = new File(testWorkingDir, "Dockerfile");
        File imageContents = new File(testWorkingDir, "test.txt");
        imageContents.createNewFile();
        String dockerfileContents = String.format("FROM scratch\nCOPY test.txt .\n");
        FileUtils.writeStringToFile(dockerfile, dockerfileContents, StandardCharsets.UTF_8);

        Set<String> tags = new HashSet<>();
        tags.add(String.format("%s:%s", imageRepo, imageTag));
        String createdImageId = dockerClientManager.buildImage(testWorkingDir, tags);
        System.out.printf("Created image %s\n", createdImageId);

        Optional<String> foundImageId = dockerClientManager.lookupImageIdByRepoTag(imageRepo, imageTag);

        assertTrue(foundImageId.isPresent());
        System.out.printf("Found image id: %s\n", foundImageId.get());
        assertTrue(foundImageId.get().startsWith("sha256:"));
    }

    @Test
    public void testDeriveDockerTarfileFromConfiguredTar() throws IOException, IntegrationException {
        Mockito.when(programPaths.getDockerInspectorTargetDirPath()).thenReturn("test/containerShared/target");
        Mockito.when(config.getDockerTar()).thenReturn("build/images/test/alpine.tar");
        ImageTarWrapper imageTarWrapper = dockerClientManager.deriveDockerTarFileFromConfig();
        assertEquals("alpine.tar", imageTarWrapper.getFile().getName());
    }

    @Test
    public void testDeriveDockerTarfileFromConfiguredImage() throws IOException, IntegrationException {
        Mockito.when(programPaths.getDockerInspectorTargetDirPath()).thenReturn("test/containerShared/target");
        Mockito.when(config.getDockerImageRepo()).thenReturn("alpine");
        Mockito.when(config.getDockerImageTag()).thenReturn("latest");
        ImageTarWrapper imageTarWrapper = dockerClientManager.deriveDockerTarFileFromConfig();
        assertEquals("alpine_latest.tar", imageTarWrapper.getFile().getName());
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/dockerclient/pullImageByPlatformValuesThatThrowExceptions.txt")
    public void testPullImageByPlatformDifferentFormats(String platform, boolean shouldThrowException) throws InterruptedException, IntegrationException {
        String repo = "ubuntu";
        String tag = "20.04";

        // remove image from local registry to ensure new image is pulled
        removeImage(repo, tag);

        boolean threwException = false;
        try {
            dockerClientManager.pullImageByPlatform(repo, tag, platform);
        } catch (BlackDuckIntegrationException | BadRequestException | DockerClientException | InternalServerErrorException e) {
            threwException = true;
        }
        assertEquals(shouldThrowException, threwException);
    }

}
