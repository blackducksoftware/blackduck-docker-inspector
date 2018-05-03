package com.blackducksoftware.integration.hub.docker.dockerinspector;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.google.common.io.Files;

public class CalledFromDetectTest {
    private static ProgramVersion programVersion;
    private static File executionDir;

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

    @AfterClass
    public static void tearDownAfterClass() {

    }

    @Test
    public void test() throws IOException, InterruptedException, IntegrationException {

        final File resultsFile = new File(String.format("%s/blackduck/docker/result.json", System.getProperty("user.home")));
        resultsFile.delete();

        final String cmdGetDetectScriptString = "curl -s https://blackducksoftware.github.io/hub-detect/hub-detect.sh";
        final String detectScriptString = executeCmd(cmdGetDetectScriptString);
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
        sb.append(String.format("export DETECT_DOCKER_PASSTHROUGH_DOCKER_INSPECTOR_JAR_PATH=%s/build/libs/hub-docker-inspector-%s.jar\n", System.getProperty("user.dir"), programVersion.getProgramVersion()));
        sb.append(detectScriptFile.getAbsolutePath());
        sb.append(String.format(" --detect.docker.inspector.path=%s/build/hub-docker-inspector.sh", System.getProperty("user.dir")));
        sb.append(" --blackduck.hub.offline.mode=true");
        sb.append(" --detect.docker.image=alpine:latest");
        sb.append(" --detect.hub.signature.scanner.disabled=true");
        sb.append(String.format(" --logging.level.com.blackducksoftware.integration=%s", "DEBUG"));
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
        final String wrapperScriptOutput = executeCmd(detectWrapperScriptFile.getAbsolutePath());
        System.out.printf("Wrapper script output:\n%s\n", wrapperScriptOutput);
        final String detectOutputString = FileUtils.readFileToString(detectOutputFile, StandardCharsets.UTF_8);
        System.out.printf("Detect output: %s", detectOutputString);

        assertTrue(resultsFile.exists());
        final String dockerInspectorResultsFileContents = FileUtils.readFileToString(resultsFile, StandardCharsets.UTF_8);
        assertTrue(dockerInspectorResultsFileContents.contains("\"succeeded\": true"));
        assertTrue(detectOutputString.contains("DOCKER: SUCCESS"));
        assertTrue(detectOutputString.contains("Overall Status: SUCCESS"));
    }

    private String executeCmd(final String cmdString) throws IOException, InterruptedException {
        System.out.printf("Executing: %s\n", cmdString);
        final Process p = Runtime.getRuntime()
                .exec(cmdString, null, executionDir);

        final BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        final BufferedReader stderrReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        print("stderr", stderrReader);
        final String stdout = getString(stdoutReader);
        final int returnValue = p.waitFor();
        System.out.printf("Return value: %d\n", returnValue);
        return stdout;
    }

    private void print(final String tag, final BufferedReader stdoutReader) throws IOException {
        final BufferedReader reader = stdoutReader;
        String line = null;
        while ((line = reader.readLine()) != null) {
            System.out.printf("%s: %s\n", tag, line);
        }
    }

    private String getString(final BufferedReader stdoutReader) throws IOException {
        final BufferedReader reader = stdoutReader;
        String line = null;
        final StringBuffer sb = new StringBuffer();
        while ((line = reader.readLine()) != null) {
            sb.append(line + "\n");
        }
        return sb.toString();
    }
}
