package com.synopsys.integration.blackduck.dockerinspector;

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

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
public class ArgsWithSpacesTest {

    private static final String USERNAME = "You Zer";
    private static final String PROJECT_NAME = "Pro Ject";
    private static final String PROJECT_VERSION = "Ver Sion";

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        try {
            final boolean created = new File(TestUtils.TEST_DIR_REL_PATH).mkdirs();
            System.out.println(String.format("test dir created: %b", created));
        } catch (final Exception e) {
            System.out.println(String.format("mkdir test: %s", e.getMessage()));
        }
    }

    @AfterAll
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testUsernameProjectNameProjectVersionWithSpaces() throws IOException, InterruptedException {
        final String workingDirPath = String.format("%s/argsWithSpaces", TestUtils.TEST_DIR_REL_PATH);
        try {
            FileUtils.deleteDirectory(new File(workingDirPath));
        } catch (final Exception e) {
            System.out.println(String.format("Unable to delete %s", workingDirPath));
        }

        final ProgramVersion pgmVerObj = new ProgramVersion();
        pgmVerObj.init();
        final String programVersion = pgmVerObj.getProgramVersion();
        final List<String> partialCmd = Arrays.asList("build/blackduck-docker-inspector.sh", "--upload.bdio=false", String.format("--blackduck.username=%s", USERNAME), String.format("--blackduck.project.name=%s", PROJECT_NAME),
                String.format("--blackduck.project.version=%s", PROJECT_VERSION), String.format("--jar.path=build/libs/blackduck-docker-inspector-%s.jar", programVersion),
                String.format("--output.path=%s/output", TestUtils.TEST_DIR_REL_PATH),
                "--output.include.dockertarfile=true",
                "--output.include.containerfilesystem=true", "--blackduck.always.trust.cert=true");
        // Arrays.asList returns a fixed size list; need a variable sized list
        final List<String> fullCmd = new ArrayList<>();
        fullCmd.addAll(partialCmd);
        fullCmd.add("--logging.level.com.synopsys=TRACE");
        fullCmd.add(String.format("--working.dir.path=%s", workingDirPath));
        fullCmd.add("--docker.image=alpine:latest");

        System.out.println(String.format("Running args-with-spaces test with command %s", fullCmd.toString()));
        final ProcessBuilder pb = new ProcessBuilder(fullCmd);
        final File outputFile = new File(String.format("%s/argsWithSpaces_output.txt", TestUtils.TEST_DIR_REL_PATH));
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
                if (line.contains("Black Duck username:") || line.contains("Black Duck project:")) {
                    System.out.println(line);
                }
                if (line.endsWith(String.format("Black Duck username: %s", USERNAME))) {
                    foundUsernameCount++;
                }
                if (line.contains(String.format("Black Duck project: %s, version: %s;", PROJECT_NAME, PROJECT_VERSION))) {
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
