package com.synopsys.integration.blackduck.dockerinspector;

import static org.springframework.test.util.AssertionErrors.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.exception.IntegrationException;

@Tag("integration")
public class PullArtifactsTest {

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterAll
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testPullJar() throws IOException, InterruptedException, IntegrationException {
        testPullOperation("jar", "jar");
    }

    @Test
    public void testPullAirGapZip() throws IOException, InterruptedException, IntegrationException {
        testPullOperation("airgapzip", "zip");
    }

    private void testPullOperation(String pullTarget, String extension) throws IOException, InterruptedException, IntegrationException {
        String cmdLineOption = String.format("--pull%s", pullTarget);
        File workingDir = new File(String.format("%s/pull%s", TestUtils.TEST_DIR_REL_PATH, pullTarget));
        FileUtils.deleteDirectory(workingDir);
        workingDir.mkdirs();
        System.out.println(String.format("workingDir: %s", workingDir.getAbsolutePath()));
        FilenameFilter fileFilter = getFilenameFilterByExtension(extension);
        File[] filesBefore = workingDir.listFiles(fileFilter);
        assertTrue(String.format("%s should be an empty directory", workingDir.getAbsolutePath()), filesBefore.length == 0);

        File script = new File("build/blackduck-docker-inspector.sh");
        List<String> partialCmd = Arrays.asList(script.getAbsolutePath(), cmdLineOption);
        // Arrays.asList returns a fixed size list; need a variable sized list
        List<String> fullCmd = new ArrayList<>();
        fullCmd.addAll(partialCmd);

        System.out.println(String.format("Running %s end to end test", cmdLineOption));
        TestUtils.execCmd(workingDir, String.join(" ", fullCmd), 30000L, true, null);
        System.out.printf("blackduck-docker-inspector %s done; verifying results...\n", cmdLineOption);

        File[] filesAfter = workingDir.listFiles(fileFilter);
        boolean foundOne = filesAfter.length == 1;
        for (File f : filesAfter) {
            System.out.println(String.format("Found pulled file: %s", f.getName()));
            f.delete();
        }
        assertTrue("Expected a single pulled file", foundOne);
    }

    private FilenameFilter getFilenameFilterByExtension(String extension) {
        String dotExtension = String.format(".%s", extension);
        FilenameFilter jarFileFilter = (dir, name) -> {
            if (name.endsWith(dotExtension)) {
                return true;
            } else {
                return false;
            }
        };
        return jarFileFilter;
    }
}
