package com.blackducksoftware.integration.hub.docker.dockerinspector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.blackducksoftware.integration.test.annotation.IntegrationTest;

@Category(IntegrationTest.class)
public class ImageCleanupTest {

    private static final String USERNAME = "You Zer";
    private static final String PROJECT_NAME = "Pro Ject";
    private static final String PROJECT_VERSION = "Ver Sion";
    private static final String TARGET_IMAGE_NAME = "alpine";
    private static final String TARGET_IMAGE_TAG = "latest";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        try {
            final boolean created = new File("test").mkdir();
            System.out.println(String.format("test dir created: %b", created));
        } catch (final Exception e) {
            System.out.println(String.format("mkdir test: %s", e.getMessage()));
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void test() throws IOException, InterruptedException {
        final String workingDirPath = "test/imageCleanup";
        try {
            FileUtils.deleteDirectory(new File(workingDirPath));
        } catch (final Exception e) {
            System.out.println(String.format("Unable to delete %s", workingDirPath));
        }

        final ProgramVersion pgmVerObj = new ProgramVersion();
        pgmVerObj.init();
        final String programVersion = pgmVerObj.getProgramVersion();
        final List<String> partialCmd = Arrays.asList("build/hub-docker-inspector.sh", "--upload.bdio=false", String.format("--hub.username=\"%s\"", USERNAME), String.format("--hub.project.name=\"%s\"", PROJECT_NAME),
                String.format("--hub.project.version=\"%s\"", PROJECT_VERSION), String.format("--jar.path=build/libs/hub-docker-inspector-%s.jar", programVersion), "--output.path=test/output", "--output.include.dockertarfile=true",
                "--output.include.containerfilesystem=true", "--hub.always.trust.cert=true", "--include.target.image=true", "--include.inspector.image=true");
        // Arrays.asList returns a fixed size list; need a variable sized list
        final List<String> fullCmd = new ArrayList<>();
        fullCmd.addAll(partialCmd);
        fullCmd.add("--logging.level.com.blackducksoftware=DEBUG");
        fullCmd.add("--cleanup.inspector.image=true");
        fullCmd.add("--cleanup.target.image=true");
        fullCmd.add(String.format("--working.dir.path=%s", workingDirPath));
        fullCmd.add(String.format("--docker.image=%s", TARGET_IMAGE_NAME, TARGET_IMAGE_TAG));
        final String log = runCommand(fullCmd, true);
        System.out.println(log);

        // See what image was used
        List<String> grepCmd = new ArrayList<>();
        grepCmd.add("fgrep");
        grepCmd.add("inspectOnImageName");
        grepCmd.add("test/output/result.json");
        final String inspectOnImageNameJsonLine = runCommand(grepCmd, true);
        final String[] inspectOnImageNameJsonLineParts = inspectOnImageNameJsonLine.split("\"");
        final String inspectOnImageName = inspectOnImageNameJsonLineParts[3];
        System.out.println(String.format("inspectOnImageName: %s", inspectOnImageName));

        grepCmd = new ArrayList<>();
        grepCmd.add("fgrep");
        grepCmd.add("inspectOnImageTag");
        grepCmd.add("test/output/result.json");
        final String inspectOnImageTagJsonLine = runCommand(grepCmd, true);
        final String[] inspectOnImageTagJsonLineParts = inspectOnImageTagJsonLine.split("\"");
        final String inspectOnImageTag = inspectOnImageTagJsonLineParts[3];
        System.out.println(String.format("inspectOnImageTag: %s", inspectOnImageTag));
        Thread.sleep(10000L); // give docker a few seconds
        final List<String> dockerImageList = getDockerImageList();
        assertFalse(isImagePresent(dockerImageList, inspectOnImageName, inspectOnImageTag));
        assertFalse(isImagePresent(dockerImageList, TARGET_IMAGE_NAME, TARGET_IMAGE_TAG));
    }

    private String runCommand(final List<String> cmd, final boolean assertPasses) throws IOException, InterruptedException {
        System.out.println(String.format("Running command %s", cmd.toString()));
        final ProcessBuilder pb = new ProcessBuilder(cmd);
        final Map<String, String> env = pb.environment();
        final String oldPath = System.getenv("PATH");
        final String newPath = String.format("/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:%s", oldPath);
        env.put("PATH", newPath);
        final File outputFile = new File(String.format("test/temp_cmd_output_%s.txt", Long.toString(System.nanoTime())));
        outputFile.delete();
        pb.redirectErrorStream(true);
        pb.redirectOutput(outputFile);
        final Process p = pb.start();
        final int retCode = p.waitFor();
        final String log = FileUtils.readFileToString(outputFile, StandardCharsets.UTF_8);
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

        System.out.println(String.format("Running command %s", dockerImagesCmd.toString()));
        final File dockerImagesoutputFile = new File("test/imageCleanup_dockerImagesOutput.txt");
        dockerImagesoutputFile.delete();
        final ProcessBuilder pb = new ProcessBuilder(dockerImagesCmd);
        final Map<String, String> env = pb.environment();
        final String oldPath = System.getenv("PATH");

        final String newPath = String.format("/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:%s", oldPath);
        env.put("PATH", newPath);
        final File outputFile = new File("test/imageCleanup_dockerImagesOutput.txt");
        outputFile.delete();
        pb.redirectErrorStream(true);
        pb.redirectOutput(dockerImagesoutputFile);
        final Process p = pb.start();
        final int retCode = p.waitFor();
        final String dockerImagesCommandOutput = FileUtils.readFileToString(dockerImagesoutputFile, StandardCharsets.UTF_8);
        System.out.println(dockerImagesCommandOutput);
        assertEquals(0, retCode);

        final String[] linesArray = dockerImagesCommandOutput.split("\\r?\\n");
        final List<String> linesList = Arrays.asList(linesArray);
        return linesList;
    }

    private boolean isImagePresent(final List<String> dockerImageList, final String targetImageName, final String targetImageTag) {
        final String imageRegex = String.format("^%s +%s.*$", targetImageName, targetImageTag.replaceAll("\\.", "\\."));
        for (final String imageListLine : dockerImageList) {
            if (imageListLine.matches(imageRegex)) {
                return true;
            }
        }
        return false;
    }
}
