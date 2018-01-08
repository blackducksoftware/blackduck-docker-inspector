package com.blackducksoftware.integration.hub.docker;

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

import com.blackducksoftware.integration.hub.docker.client.ProgramVersion;

public class ImageCleanupTest {

    private static final String USERNAME = "You Zer";
    private static final String PROJECT_NAME = "Pro Ject";
    private static final String PROJECT_VERSION = "Ver Sion";
    private static final String TARGET_IMAGE_NAME = "alpine";
    private static final String TARGET_IMAGE_TAG = "latest";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testUsernameProjectNameProjectVersionWithSpaces() throws IOException, InterruptedException {
        final String workingDirPath = "test/imageCleanup";
        try {
            FileUtils.deleteDirectory(new File(workingDirPath));
        } catch (final Exception e) {
            System.out.println(String.format("Unable to delete %s", workingDirPath));
        }

        final String programVersion = (new ProgramVersion()).getProgramVersion();
        final List<String> partialCmd = Arrays.asList("build/hub-docker-inspector.sh", "--dry.run=true", String.format("--hub.username=\"%s\"", USERNAME), String.format("--hub.project.name=\"%s\"", PROJECT_NAME),
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

        System.out.println(String.format("Running image cleanup test with command %s", fullCmd.toString()));
        ProcessBuilder pb = new ProcessBuilder(fullCmd);
        final Map<String, String> env = pb.environment();
        final String oldPath = System.getenv("PATH");
        final String newPath = String.format("/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:%s", oldPath);
        env.put("PATH", newPath);
        final File outputFile = new File("test/imageCleanup_output.txt");
        outputFile.delete();
        pb.redirectErrorStream(true);
        pb.redirectOutput(outputFile);
        Process p = pb.start();
        int retCode = p.waitFor();
        if (retCode != 0) {
            final String log = FileUtils.readFileToString(outputFile, StandardCharsets.UTF_8);
            System.out.println(log);
        }
        assertEquals(0, retCode);

        // See what image was used
        List<String> grepCmd = new ArrayList<>();
        grepCmd.add("fgrep");
        grepCmd.add("inspectOnImageName");
        grepCmd.add("test/output/result.json");
        System.out.println(String.format("Running command %s", grepCmd.toString()));
        pb = new ProcessBuilder(grepCmd);

        outputFile.delete();
        pb.redirectErrorStream(true);
        pb.redirectOutput(outputFile);
        p = pb.start();
        retCode = p.waitFor();
        String log = FileUtils.readFileToString(outputFile, StandardCharsets.UTF_8);
        assertEquals(0, retCode);
        String[] parts = log.split("\"");
        final String inspectOnImageName = parts[3];
        System.out.println(String.format("inspectOnImageName: %s", inspectOnImageName));

        grepCmd = new ArrayList<>();
        grepCmd.add("fgrep");
        grepCmd.add("inspectOnImageTag");
        grepCmd.add("test/output/result.json");
        System.out.println(String.format("Running command %s", grepCmd.toString()));
        pb = new ProcessBuilder(grepCmd);

        outputFile.delete();
        pb.redirectErrorStream(true);
        pb.redirectOutput(outputFile);
        p = pb.start();
        retCode = p.waitFor();
        log = FileUtils.readFileToString(outputFile, StandardCharsets.UTF_8);
        assertEquals(0, retCode);
        parts = log.split("\"");
        final String inspectOnImageTag = parts[3];
        System.out.println(String.format("inspectOnImageTag: %s", inspectOnImageTag));

        final List<String> dockerImageList = getDockerImageList();
        assertFalse(isImagePresent(dockerImageList, inspectOnImageName, inspectOnImageTag));
        assertFalse(isImagePresent(dockerImageList, TARGET_IMAGE_NAME, TARGET_IMAGE_TAG));
    }

    private List<String> getDockerImageList() throws IOException, InterruptedException {
        final List<String> dockerImagesCmd = new ArrayList<>();
        dockerImagesCmd.add("/usr/local/bin/docker");
        dockerImagesCmd.add("images");

        System.out.println(String.format("Running command %s", dockerImagesCmd.toString()));
        final File dockerImagesoutputFile = new File("test/imageCleanup_dockerImagesOutput.txt");
        dockerImagesoutputFile.delete();
        final ProcessBuilder pb = new ProcessBuilder(dockerImagesCmd);
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
