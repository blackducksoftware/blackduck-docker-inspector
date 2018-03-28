package com.blackducksoftware.integration.hub.docker.dockerinspector;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.blackducksoftware.integration.exception.IntegrationException;

public class TempTest {

    private static File containerSharedDir;
    private static File containerTargetDir;
    private static File containerOutputDir;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        try {
            final boolean created = new File("test").mkdir();
            System.out.println(String.format("test dir created: %b", created));
        } catch (final Exception e) {
            System.out.println(String.format("mkdir test: %s", e.getMessage()));
        }
        // TODO code below is redundant w/ code above
        System.out.printf("=========================================== Creating directories: test, test/containerShared, test/containerShared/target, test/containerShared/output\n");
        final File testDir = new File("test");
        try {
            testDir.mkdir();
        } catch (final Exception e) {
            System.out.printf("Error creating directory: test: %s\n", e.getMessage());
        }
        try {
            testDir.setWritable(true, false);
        } catch (final Exception e) {
            System.out.printf("Error making directory writeable: test: %s\n", e.getMessage());
        }
        containerSharedDir = new File("test/containerShared");
        containerTargetDir = new File(containerSharedDir, "target");
        containerOutputDir = new File(containerSharedDir, "output");
        try {
            containerTargetDir.mkdir();
        } catch (final Exception e) {
            System.out.printf("Error creating directory: test/target: %s\n", e.getMessage());
        }
        try {
            containerTargetDir.setWritable(true, false);
        } catch (final Exception e) {
            System.out.printf("Error making directory writeable: test/target: %s\n", e.getMessage());
        }
        try {
            containerOutputDir.mkdir();
        } catch (final Exception e) {
            System.out.printf("Error creating directory: test/output: %s\n", e.getMessage());
        }
        try {
            containerOutputDir.setWritable(true, false);
        } catch (final Exception e) {
            System.out.printf("Error making directory writeable: test/output: %s\n", e.getMessage());
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {

    }

    @Test
    public void testUbuntu() throws IOException, InterruptedException, IntegrationException {
        execCmd("ls -lR test", 10000L);
    }

    private static String execCmd(final File workingDir, final String cmd, final long timeout) throws IOException, InterruptedException, IntegrationException {
        return execCmd(workingDir, cmd, timeout, null);
    }

    private static String execCmd(final String cmd, final long timeout) throws IOException, InterruptedException, IntegrationException {
        return execCmd(null, cmd, timeout, null);
    }

    private static String execCmd(final File workingDir, final String cmd, final long timeout, final Map<String, String> givenEnv) throws IOException, InterruptedException, IntegrationException {
        System.out.println(String.format("Executing: %s", cmd));
        final ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
        pb.redirectOutput(Redirect.PIPE);
        pb.redirectError(Redirect.PIPE);
        if (workingDir != null) {
            pb.directory(workingDir);
        }
        final Map<String, String> processEnv = pb.environment();
        final String oldPath = System.getenv("PATH");
        final String newPath = String.format("/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:%s", oldPath);
        System.out.println(String.format("Adjusted path: %s", newPath));
        processEnv.put("PATH", newPath);
        if (givenEnv != null) {
            pb.environment().putAll(givenEnv);
        }
        final Process p = pb.start();
        final String stdoutString = toString(p.getInputStream());
        final String stderrString = toString(p.getErrorStream());
        final boolean finished = p.waitFor(timeout, TimeUnit.SECONDS);
        if (!finished) {
            throw new InterruptedException(String.format("Command '%s' timed out", cmd));
        }

        System.out.println(String.format("%s: stdout: %s", cmd, stdoutString));
        System.out.println(String.format("%s: stderr: %s", cmd, stderrString));
        final int retCode = p.exitValue();
        if (retCode != 0) {
            System.out.println(String.format("%s: retCode: %d", cmd, retCode));
            throw new IntegrationException(String.format("Command '%s' failed: %s", cmd, stderrString));
        }
        return stdoutString;
    }

    private static String toString(final InputStream is) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        final StringBuilder builder = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(line);
        }
        return builder.toString();
    }
}
