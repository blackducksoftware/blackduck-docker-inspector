package com.synopsys.integration.blackduck.dockerinspector;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.exception.IntegrationException;

public class TestUtils {
    public final static String TEST_DIR_REL_PATH = "test";

    public static File createTempDirectory() throws IOException {
        File temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
        if (!temp.delete()) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }
        if (!temp.mkdir()) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
        return temp;
    }

    public static void deleteDirIfExists(File workingDir) {
        try {
            FileUtils.deleteDirectory(workingDir);
        } catch (Exception e) {
            System.out.println(String.format("Unable to delete %s", workingDir.getAbsolutePath()));
        }
    }

    public static void ensureFileDoesNotExist(File outputContainerFileSystemFile) throws IOException {
        if (outputContainerFileSystemFile == null) {
            return;
        }
        Files.deleteIfExists(outputContainerFileSystemFile.toPath());
        assertFalse(outputContainerFileSystemFile.exists());
    }

    public static boolean contentEquals(File file1, File file2, List<String> exceptLinesContainingThese) throws IOException {
        System.out.printf("Comparing %s %s\n", file1.getAbsolutePath(), file2.getAbsolutePath());

        List<String> lines1 = FileUtils.readLines(file1, StandardCharsets.UTF_8);
        List<String> lines2 = FileUtils.readLines(file2, StandardCharsets.UTF_8);

        if (lines1.size() != lines2.size()) {
            System.out.printf("Files' line counts are different\n");
            return false;
        }

        ListComparisonData list1List2 = compareListToOtherList(lines1, lines2, exceptLinesContainingThese);
        ListComparisonData list2List1 = compareListToOtherList(lines2, lines1, exceptLinesContainingThese);
        ListComparisonData aggregateComparisonData = aggregateComparisonData(list1List2, list2List1);

        if (!aggregateComparisonData.isEqualContent()) {
            return false;
        }

        System.out.printf("These files match (%d lines matched; %d lines ignored)\n", aggregateComparisonData.getMatchedLines(), aggregateComparisonData.getIgnoredLines());
        return true;
    }

    private static ListComparisonData compareListToOtherList(List<String> list1, List<String> list2, List<String> exceptLinesContainingThese) {
        ListComparisonData listComparisonData = new ListComparisonData();
        for (String line : list1) {
            if (shouldSkipLineDuringComparison(line, exceptLinesContainingThese)) {
                listComparisonData.ignoredLine();
                continue;
            }
            if (!list2.contains(line)) {
                System.out.printf("File comparison: Line not shared by both files:\n%s\n", line);
                listComparisonData.equalContent(false);
                return listComparisonData;
            }
            listComparisonData.matchedLine();
        }
        listComparisonData.equalContent(true);
        return listComparisonData;
    }

    private static boolean shouldSkipLineDuringComparison(String line, List<String> exceptLinesContainingThese) {
        if (exceptLinesContainingThese != null) {
            for (String ignoreMe : exceptLinesContainingThese) {
                if (line.contains(ignoreMe)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ListComparisonData aggregateComparisonData(ListComparisonData data1, ListComparisonData data2) {
        boolean equalContent = data1.isEqualContent() && data2.isEqualContent();
        int matches = Integer.max(data1.getMatchedLines(), data2.getMatchedLines());
        int ignored = Integer.max(data1.getIgnoredLines(), data2.getIgnoredLines());
        return new ListComparisonData(equalContent, matches, ignored);
    }

    public static String execCmd(File workingDir, String cmd, long timeout, boolean logStdout, Map<String, String> givenEnv) throws IOException, InterruptedException, IntegrationException {
        return execCmd(workingDir, cmd, timeout, givenEnv, logStdout);
    }

    public static String execCmd(String cmd, long timeout, boolean logStdout, Map<String, String> givenEnv) throws IOException, InterruptedException, IntegrationException {
        return execCmd(null, cmd, timeout, givenEnv, logStdout);
    }

    private static String execCmd(File workingDir, String cmd, long timeout, Map<String, String> givenEnv, boolean logStdout) throws IOException, InterruptedException, IntegrationException {
        System.out.println(String.format("Executing: %s", cmd));
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
        pb.redirectOutput(Redirect.PIPE);
        pb.redirectError(Redirect.PIPE);
        if (workingDir != null) {
            pb.directory(workingDir);
        }
        if (givenEnv != null) {
            pb.environment().putAll(givenEnv);
        }
        Process p = pb.start();
        String stdoutString;
        String stderrString;
        try (InputStream stdStream = p.getInputStream(); InputStream errStream = p.getErrorStream()) {
            stdoutString = toString(stdStream);
            stderrString = toString(errStream);
        }
        boolean finished = p.waitFor(timeout, TimeUnit.SECONDS);
        if (!finished) {
            throw new InterruptedException(String.format("Command '%s' timed out", cmd));
        }
        if (logStdout) {
            System.out.println(String.format("%s: stdout: '%s'", cmd, stdoutString));
        }
        if (StringUtils.isNotBlank(stdoutString)) {
            System.out.println(String.format("%s: stderr: '%s'", cmd, stderrString));
        }
        int retCode = p.exitValue();
        if (retCode != 0) {
            System.out.println(String.format("%s: retCode: %d", cmd, retCode));
            throw new IntegrationException(String.format("Command '%s' failed: %s", cmd, stderrString));
        }
        return stdoutString;
    }

    private static String toString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder builder = new StringBuilder();
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
