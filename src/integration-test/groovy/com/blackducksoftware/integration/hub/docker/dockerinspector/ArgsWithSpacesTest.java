package com.blackducksoftware.integration.hub.docker.dockerinspector;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ArgsWithSpacesTest {

    private static final String USERNAME = "You Zer";
    private static final String PROJECT_NAME = "Pro Ject";
    private static final String PROJECT_VERSION = "Ver Sion";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        try {
            final boolean created = (new File("test")).mkdir();
            System.out.println(String.format("test dir created: %b", created));
        } catch (final Exception e) {
            System.out.println(String.format("mkdir test: %s", e.getMessage()));
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testUsernameProjectNameProjectVersionWithSpaces() throws IOException, InterruptedException {
        final String workingDirPath = "test/argsWithSpaces";
        try {
            FileUtils.deleteDirectory(new File(workingDirPath));
        } catch (final Exception e) {
            System.out.println(String.format("Unable to delete %s", workingDirPath));
        }

        final ProgramVersion pgmVerObj = new ProgramVersion();
        pgmVerObj.init();
        final String programVersion = pgmVerObj.getProgramVersion();
        final List<String> partialCmd = Arrays.asList("build/hub-docker-inspector.sh", "--dry.run=true", String.format("--hub.username=\"%s\"", USERNAME), String.format("--hub.project.name=\"%s\"", PROJECT_NAME),
                String.format("--hub.project.version=\"%s\"", PROJECT_VERSION), String.format("--jar.path=build/libs/hub-docker-inspector-%s.jar", programVersion), "--output.path=test/output", "--output.include.dockertarfile=true",
                "--output.include.containerfilesystem=true", "--hub.always.trust.cert=true");
        // Arrays.asList returns a fixed size list; need a variable sized list
        final List<String> fullCmd = new ArrayList<>();
        fullCmd.addAll(partialCmd);
        fullCmd.add("--logging.level.com.blackducksoftware=TRACE");
        fullCmd.add(String.format("--working.dir.path=%s", workingDirPath));
        fullCmd.add("alpine:latest");

        System.out.println(String.format("Running args-with-spaces test with command %s", fullCmd.toString()));
        final ProcessBuilder pb = new ProcessBuilder(fullCmd);
        final Map<String, String> env = pb.environment();
        final String oldPath = System.getenv("PATH");
        final String newPath = String.format("/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:%s", oldPath);
        System.out.println(String.format("Adjusted path: %s", newPath));
        env.put("PATH", newPath);
        final File outputFile = new File("test/argsWithSpaces_output.txt");
        outputFile.delete();
        pb.redirectErrorStream(true);
        pb.redirectOutput(outputFile);
        final Process p = pb.start();
        final int retCode = p.waitFor();
        if (retCode != 0) {
            final String log = FileUtils.readFileToString(outputFile, StandardCharsets.UTF_8);
            System.out.println(log);
        }
        assertEquals(0, retCode);

        int foundUsernameCount = 0;
        int foundProjectNameVersionCount = 0;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(outputFile)));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.endsWith(String.format("Hub username: \"%s\"", USERNAME))) {
                    foundUsernameCount++;
                }
                if (line.contains(String.format("Hub project: \"%s\", version: \"%s\";", PROJECT_NAME, PROJECT_VERSION))) {
                    foundProjectNameVersionCount++;
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        assertEquals(1, foundUsernameCount);
        assertEquals(1, foundProjectNameVersionCount);
    }
}
