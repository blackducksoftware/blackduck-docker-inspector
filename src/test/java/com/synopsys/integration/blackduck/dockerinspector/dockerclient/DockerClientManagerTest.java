package com.synopsys.integration.blackduck.dockerinspector.dockerclient;

import static org.junit.Assert.assertFalse;
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

import com.synopsys.integration.blackduck.dockerinspector.output.ImageTarFilename;
import com.synopsys.integration.exception.IntegrationException;

@Tag("integration")
public class DockerClientManagerTest {
    private final static String imageRepo = "dockerclientmanagertest";
    private final static String imageTag = "dockerclientmanagertest";

    private static DockerClientManager dockerClientManager;

    @BeforeAll
    public static void setUp() {
        dockerClientManager = new DockerClientManager();
        dockerClientManager.setImageTarFilename(new ImageTarFilename());
    }

    @AfterAll
    public static void tearDown() {
        final Optional<String> foundImageIdInitial = dockerClientManager.lookupImageIdByRepoTag(imageRepo, imageTag);
        if (foundImageIdInitial.isPresent()) {
            try {
                dockerClientManager.removeImage(foundImageIdInitial.get());
            } catch (IntegrationException e) {
            }
        }
    }

    @Test
    public void test() throws IOException, IntegrationException {

        final Optional<String> foundImageIdInitial = dockerClientManager.lookupImageIdByRepoTag(imageRepo, imageTag);
        if (foundImageIdInitial.isPresent()) {
            dockerClientManager.removeImage(foundImageIdInitial.get());
        }
        final Optional<String> foundImageIdShouldBeEmpty = dockerClientManager.lookupImageIdByRepoTag(imageRepo, imageTag);
        assertFalse(foundImageIdShouldBeEmpty.isPresent());

        final File testWorkingDir = new File("test/output/dockerClientManagerTest");
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
}
