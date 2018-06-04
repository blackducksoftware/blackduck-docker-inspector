package com.blackducksoftware.integration.hub.docker.dockerinspector;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.blackducksoftware.integration.exception.IntegrationException;

public class TestUtils {
    public final static String TEST_DIR_REL_PATH = "test";

    public static File createTempDirectory() throws IOException {
        final File temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
        if (!temp.delete()) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }
        if (!temp.mkdir()) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
        return temp;
    }

    public static void deleteDirIfExists(final File workingDir) {
        try {
            FileUtils.deleteDirectory(workingDir);
        } catch (final Exception e) {
            System.out.println(String.format("Unable to delete %s", workingDir.getAbsolutePath()));
        }
    }

    public static boolean contentEquals(final File file1, final File file2, final List<String> exceptLinesContainingThese) throws IOException {
        System.out.printf("Comparing %s %s\n", file1.getAbsolutePath(), file2.getAbsolutePath());
        int ignoredLineCount = 0;
        int matchedLineCount = 0;
        final List<String> lines1 = FileUtils.readLines(file1, StandardCharsets.UTF_8);
        final List<String> lines2 = FileUtils.readLines(file2, StandardCharsets.UTF_8);

        if (lines1.size() != lines2.size()) {
            System.out.printf("Files' line counts are different\n");
            return false;
        }
        for (int i = 0; i < lines1.size(); i++) {
            final String line1 = lines1.get(i);
            final String line2 = lines2.get(i);
            boolean skip = false;
            if (exceptLinesContainingThese != null) {
                for (final String ignoreMe : exceptLinesContainingThese) {
                    if (line1.contains(ignoreMe) || line2.contains(ignoreMe)) {
                        skip = true;
                        ignoredLineCount++;
                    }
                }
            }
            if (skip) {
                continue;
            }
            if (!line2.equals(line1)) {
                System.out.printf("File comparison: These lines do not match:\n%s\n%s\n", lines1.get(i), lines2.get(i));
                return false;
            } else {
                matchedLineCount++;
            }
        }
        System.out.printf("These files match (%d lines matched; %d lines ignored)\n", matchedLineCount, ignoredLineCount);
        return true;
    }

    public static String execCmd(final File workingDir, final String cmd, final long timeout, final boolean logStdout) throws IOException, InterruptedException, IntegrationException {
        return execCmd(workingDir, cmd, timeout, null, logStdout);
    }

    public static String execCmd(final String cmd, final long timeout, final boolean logStdout) throws IOException, InterruptedException, IntegrationException {
        return execCmd(null, cmd, timeout, null, logStdout);
    }

    private static String execCmd(final File workingDir, final String cmd, final long timeout, final Map<String, String> givenEnv, final boolean logStdout) throws IOException, InterruptedException, IntegrationException {
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
        if (logStdout) {
            System.out.println(String.format("%s: stdout: '%s'", cmd, stdoutString));
        }
        if (StringUtils.isNotBlank(stdoutString)) {
            System.out.println(String.format("%s: stderr: '%s'", cmd, stderrString));
        }
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
