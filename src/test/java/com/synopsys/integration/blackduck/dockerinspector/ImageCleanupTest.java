package com.synopsys.integration.blackduck.dockerinspector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
public class ImageCleanupTest {

    private static final String USERNAME = "You Zer";
    private static final String PROJECT_NAME = "Pro Ject";
    private static final String PROJECT_VERSION = "Ver Sion";
    private static final String TARGET_IMAGE_NAME = "alpine";
    private static final String INSPECTOR_IMAGE_SUFFIX = "alpine";
    private static final String TARGET_IMAGE_TAG = "latest";

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        try {
            final boolean created = new File(TestUtils.TEST_DIR_REL_PATH).mkdirs();
            System.out.println(String.format("test dir created: %b", created));
        } catch (final Exception e) {
            System.out.println(String.format("mkdir %s: %s", TestUtils.TEST_DIR_REL_PATH, e.getMessage()));
        }
    }

    @AfterAll
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void test() throws IOException, InterruptedException {
        final String workingDirPath = String.format("%s/imageCleanup", TestUtils.TEST_DIR_REL_PATH);
        try {
            FileUtils.deleteDirectory(new File(workingDirPath));
        } catch (final Exception e) {
            System.out.println(String.format("Unable to delete %s", workingDirPath));
        }

        final ProgramVersion pgmVerObj = new ProgramVersion();
        pgmVerObj.init();

        // IFF inspector image is absent or can be removed: expect it to be gone at end
        boolean expectInspectOnImageRemoved = true;
        final String inspectOnImageRepoName = String.format("blackducksoftware/%s-%s", pgmVerObj.getInspectorImageFamily(), INSPECTOR_IMAGE_SUFFIX);
        final String inspectOnImageTag = pgmVerObj.getInspectorImageVersion();
        List<String> dockerImageList = getDockerImageList();
        if (isImagePresent(dockerImageList, inspectOnImageRepoName, inspectOnImageTag)) {
            final String runOnImageRepoAndTag = String.format("%s:%s", inspectOnImageRepoName, inspectOnImageTag);
            System.out.printf("RunOn image %s exists locally; will try to remove it\n", runOnImageRepoAndTag);
            final List<String> dockerRmiCmd = Arrays.asList("bash", "-c", String.format("docker rmi %s", runOnImageRepoAndTag));
            final String log = runCommand(dockerRmiCmd, false);
            System.out.println(log);
            dockerImageList = getDockerImageList();
            if (isImagePresent(dockerImageList, inspectOnImageRepoName, inspectOnImageTag)) {
                System.out.printf("InspectOn Image %s already exists and can't be removed, so won't expect DI to remove it when finished\n", runOnImageRepoAndTag);
                expectInspectOnImageRemoved = false;
            }
        }

        final String programVersion = pgmVerObj.getProgramVersion();
        final List<String> partialCmd = Arrays.asList("build/blackduck-docker-inspector.sh", "--upload.bdio=false", String.format("--blackduck.username=\"%s\"", USERNAME), String.format("--blackduck.project.name=\"%s\"", PROJECT_NAME),
                String.format("--blackduck.project.version=\"%s\"", PROJECT_VERSION), String.format("--jar.path=build/libs/blackduck-docker-inspector-%s.jar", programVersion),
                String.format("--output.path=%s/output", TestUtils.TEST_DIR_REL_PATH),
                "--output.include.dockertarfile=true",
                "--output.include.containerfilesystem=true", "--blackduck.always.trust.cert=true", "--include.target.image=true", "--include.inspector.image=true");
        final List<String> fullCmd = new ArrayList<>();
        fullCmd.addAll(partialCmd);
        fullCmd.add("--logging.level.com.synopsys=DEBUG");
        fullCmd.add("--cleanup.inspector.image=true");
        fullCmd.add("--cleanup.target.image=true");
        fullCmd.add(String.format("--working.dir.path=%s", workingDirPath));
        fullCmd.add(String.format("--docker.image=%s", TARGET_IMAGE_NAME, TARGET_IMAGE_TAG));
        final String log = runCommand(fullCmd, true);
        System.out.println(log);
        Thread.sleep(10000L); // give docker a few seconds
        dockerImageList = getDockerImageList();
        if (expectInspectOnImageRemoved) {
            assertFalse(isImagePresent(dockerImageList, inspectOnImageRepoName, inspectOnImageTag));
        }
        assertFalse(String.format("Target image %s:%s was not removed", TARGET_IMAGE_NAME, TARGET_IMAGE_TAG), isImagePresent(dockerImageList, TARGET_IMAGE_NAME, TARGET_IMAGE_TAG));
    }

    private String runCommand(final List<String> cmd, final boolean assertPasses) throws IOException, InterruptedException {
        System.out.println(String.format("Running command %s", cmd.toString()));
        final ProcessBuilder pb = new ProcessBuilder(cmd);
        final File outputFile = new File(String.format("%s/temp_cmd_output_%s.txt", TestUtils.TEST_DIR_REL_PATH, Long.toString(System.nanoTime())));
        outputFile.delete();
        pb.redirectErrorStream(true);
        pb.redirectOutput(outputFile);
        final Process p = pb.start();
        final int retCode = p.waitFor();
        final String log = FileUtils.readFileToString(outputFile, StandardCharsets.UTF_8);
        System.out.println(log);
        if (assertPasses) {
            assertEquals(0, retCode);
        }
        outputFile.delete();
        return log;
    }

    private List<String> getDockerImageList() throws IOException, InterruptedException {
        final List<String> dockerImagesCmd = new ArrayList<>();
        dockerImagesCmd.add("bash");
        dockerImagesCmd.add("-c");
        dockerImagesCmd.add("docker images");
        final String description = "dockerImages";

        return getCmdOutputLines(dockerImagesCmd, description);
    }

    private List<String> getCmdOutputLines(final List<String> dockerImagesCmd, final String description) throws IOException, InterruptedException {
        final String outputFilename = String.format("%s/imageCleanup_%sOutput.txt", TestUtils.TEST_DIR_REL_PATH, description);
        System.out.println(String.format("Running command %s", dockerImagesCmd.toString()));
        final File dockerImagesoutputFile = new File(outputFilename);
        dockerImagesoutputFile.delete();
        final ProcessBuilder pb = new ProcessBuilder(dockerImagesCmd);
        pb.redirectErrorStream(true);
        pb.redirectOutput(dockerImagesoutputFile);
        final Process p = pb.start();
        final int retCode = p.waitFor();
        final String dockerImagesCommandOutput = FileUtils.readFileToString(dockerImagesoutputFile, StandardCharsets.UTF_8);
        System.out.printf("%s: %s\n", description, dockerImagesCommandOutput);
        assertEquals(0, retCode);

        final String[] linesArray = dockerImagesCommandOutput.split("\\r?\\n");
        final List<String> linesList = Arrays.asList(linesArray);
        return linesList;
    }

    private boolean isImagePresent(final List<String> dockerImageList, final String targetImageName, final String targetImageTag) {
        System.out.printf("Checking docker image list for image %s:%s\n", targetImageName, targetImageTag);
        final String imageRegex = String.format("^%s +%s.*$", targetImageName, targetImageTag.replaceAll("\\.", "\\."));
        for (final String imageListLine : dockerImageList) {
            if (imageListLine.matches(imageRegex)) {
                System.out.println("\tFound it");
                return true;
            }
        }
        System.out.println("\tDid not find it");
        return false;
    }
}
