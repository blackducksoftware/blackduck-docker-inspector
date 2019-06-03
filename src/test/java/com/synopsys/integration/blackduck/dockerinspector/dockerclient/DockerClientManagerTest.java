package com.synopsys.integration.blackduck.dockerinspector.dockerclient;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.output.ImageTarFilename;

@Tag("integration")
public class DockerClientManagerTest {

    @Test
    public void test() throws IOException {
        DockerClientManager dockerClientManager = new DockerClientManager();
        dockerClientManager.setImageTarFilename(new ImageTarFilename());
        final Config config = Mockito.mock(Config.class);
        dockerClientManager.setConfig(config);

        final File testWorkingDir = new File("test/output/dockerClientManagerTest");
        final File dockerfile = new File(testWorkingDir, "Dockerfile");
        final File imageContents = new File(testWorkingDir, "test.txt");
        imageContents.createNewFile();
        final String dockerfileContents = String.format("FROM scratch\nCOPY test.txt .\n");
        FileUtils.writeStringToFile(dockerfile, dockerfileContents, StandardCharsets.UTF_8);

        final Set<String> tags = new HashSet<>();
        tags.add("ttttaa:ttttaa"); // TODO
        final String createdImageId = dockerClientManager.buildImage(testWorkingDir, tags);
        System.out.printf("Created image %s\n", createdImageId);

        final Optional<String> foundImageId = dockerClientManager.lookupImageIdForRepoTag("ttttaa", "ttttaa");

        assertTrue(foundImageId.isPresent());
        System.out.printf("Found image id: %s\n", foundImageId.get());
        assertTrue(foundImageId.get().startsWith("sha256:"));
    }
}
