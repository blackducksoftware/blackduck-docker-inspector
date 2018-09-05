package com.synopsys.integration.blackduck.dockerinspector;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.google.common.io.Files;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.test.annotation.IntegrationTest;

// TODO restore this
@Ignore
@Category(IntegrationTest.class)
public class CalledFromDetectTest {
    private static final String TEXT_PRECEDING_BDIO_FILE_DIR_PATH = "Writing BDIO to ";
    private static ProgramVersion programVersion;
    private static File executionDir;

    private static long ONE_MINUTE_IN_MS = 1L * 60L * 1000L;
    private static long FIVE_MINUTES_IN_MS = 5L * 60L * 1000L;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        programVersion = new ProgramVersion();
        programVersion.init();
        executionDir = Files.createTempDir();
        executionDir.deleteOnExit();
        final File fakeDockerExe = new File(executionDir, "docker");
        fakeDockerExe.createNewFile();
        fakeDockerExe.setExecutable(true, false);
    }

    @Test
    public void test() throws IOException, InterruptedException, IntegrationException {

        final String cmdGetDetectScriptString = "curl -s https://blackducksoftware.github.io/hub-detect/hub-detect.sh";
        final String detectScriptString = TestUtils.execCmd(executionDir, cmdGetDetectScriptString, ONE_MINUTE_IN_MS, true, null);
        final File detectScriptFile = File.createTempFile("latestDetect", ".sh");
        detectScriptFile.setExecutable(true);
        detectScriptFile.deleteOnExit();
        System.out.printf("script file: %s\n", detectScriptFile.getAbsolutePath());
        FileUtils.write(detectScriptFile, detectScriptString, StandardCharsets.UTF_8);

        final File detectOutputFile = File.createTempFile("detectOutput", ".txt");
        detectOutputFile.setWritable(true);
        detectScriptFile.deleteOnExit();

        final StringBuffer sb = new StringBuffer();
        sb.append("#\n");
        sb.append(String.format("export DETECT_DOCKER_PASSTHROUGH_DOCKER_INSPECTOR_JAR_PATH=%s/build/libs/blackduck-docker-inspector-%s.jar\n", System.getProperty("user.dir"), programVersion.getProgramVersion()));
        sb.append(detectScriptFile.getAbsolutePath());
        sb.append(String.format(" --detect.docker.inspector.path=%s/build/blackduck-docker-inspector.sh", System.getProperty("user.dir")));
        sb.append(" --blackduck.blackduck.offline.mode=true");
        sb.append(" --detect.docker.image=alpine:latest");
        sb.append(" --detect.blackduck.signature.scanner.disabled=true");
        sb.append(String.format(" --detect.docker.passthrough.logging.level.com.synopsys=%s", "DEBUG"));
        sb.append(String.format(" --detect.docker.passthrough.cleanup.inspector.container=%b", true));
        sb.append(String.format(" --detect.cleanup=%b", false));
        sb.append(String.format(" > %s", detectOutputFile.getAbsolutePath()));
        sb.append("");
        final String detectWrapperScriptString = sb.toString();
        System.out.printf("Detect wrapper script content:\n%s\n", detectWrapperScriptString);
        final File detectWrapperScriptFile = File.createTempFile("detectWrapper", ".sh");
        detectWrapperScriptFile.setExecutable(true);
        detectScriptFile.deleteOnExit();
        System.out.printf("script file: %s\n", detectWrapperScriptFile.getAbsolutePath());
        FileUtils.write(detectWrapperScriptFile, detectWrapperScriptString, StandardCharsets.UTF_8);
        final String wrapperScriptOutput = TestUtils.execCmd(executionDir, detectWrapperScriptFile.getAbsolutePath(), FIVE_MINUTES_IN_MS, true, null);
        System.out.printf("Wrapper script output:\n%s\n", wrapperScriptOutput);
        final String detectOutputString = FileUtils.readFileToString(detectOutputFile, StandardCharsets.UTF_8);
        System.out.printf("Detect output: %s", detectOutputString);

        final File bdioFile = getBdioFile(detectOutputString);
        assertTrue(bdioFile.exists());
        final String dockerInspectorBdioFileContents = FileUtils.readFileToString(bdioFile, StandardCharsets.UTF_8);
        assertTrue(dockerInspectorBdioFileContents.contains("\"spdx:name\": \"alpine_latest_lib_apk_APK\","));

        assertTrue(detectOutputString.contains("DOCKER: SUCCESS"));
        assertTrue(detectOutputString.contains("Overall Status: SUCCESS"));
    }

    private File getBdioFile(final String detectOutputString) throws IntegrationException {
        final String bdioFilePath = getBdioFilePath(detectOutputString);
        final File bdioFile = new File(bdioFilePath);
        return bdioFile;
    }

    private String getBdioFilePath(final String detectOutputString) throws IntegrationException {
        for (final String line : detectOutputString.split("\n")) {
            if (line.matches(".*Output\\s+: Writing BDIO to .*")) {
                System.out.printf("found line: %s\n", line);
                final int bdioFilePathStart = line.indexOf(TEXT_PRECEDING_BDIO_FILE_DIR_PATH) + TEXT_PRECEDING_BDIO_FILE_DIR_PATH.length();
                final String bdioFilePath = line.substring(bdioFilePathStart);
                System.out.printf("BDIO file path: %s\n", bdioFilePath);
                return bdioFilePath;
            }
        }
        throw new IntegrationException("BDIO file path not found");
    }
}
