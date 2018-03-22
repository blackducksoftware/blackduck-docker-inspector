package com.blackducksoftware.integration.hub.docker.dockerinspector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.docker.imageinspector.TestUtils;

public class DockerInspectorTest {

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
    public void testUbuntu() throws IOException, InterruptedException {
        testImage("ubuntu:17.04", "ubuntu", "17.04", "var_lib_dpkg", true);
    }

    @Test
    public void testAlpine() throws IOException, InterruptedException {
        testImage("alpine:3.6", "alpine", "3.6", "lib_apk", true);
    }

    @Test
    public void testAlpineLatest() throws IOException, InterruptedException {
        testImage("alpine", "alpine", "latest", "lib_apk", false);
    }

    @Test
    public void testCentos() throws IOException, InterruptedException {
        testImage("centos:7.3.1611", "centos", "7.3.1611", "var_lib_rpm", true);
    }

    @Test
    public void testHubWebapp() throws IOException, InterruptedException {
        testImage("blackducksoftware/hub-webapp:4.0.0", "blackducksoftware_hub-webapp", "4.0.0", "lib_apk", true);
    }

    @Test
    public void testHubZookeeper() throws IOException, InterruptedException {
        testImage("blackducksoftware/hub-zookeeper:4.0.0", "blackducksoftware_hub-zookeeper", "4.0.0", "lib_apk", true);
    }

    @Test
    public void testTomcat() throws IOException, InterruptedException {
        testImage("tomcat:6.0.53-jre7", "tomcat", "6.0.53-jre7", "var_lib_dpkg", true);
    }

    @Test
    public void testRhel() throws IOException, InterruptedException {
        testImage("dnplus/rhel:6.5", "dnplus_rhel", "6.5", "var_lib_rpm", true);
    }

    @Test
    public void testWhiteout() throws IOException, InterruptedException {
        testTar("build/images/test/whiteouttest.tar", "blackducksoftware_whiteouttest", "blackducksoftware/whiteouttest", "1.0", "1.0", "var_lib_dpkg", true, null, true);
    }

    @Test
    public void testAggregateTarfileImageOne() throws IOException, InterruptedException {
        testTar("build/images/test/aggregated.tar", "blackducksoftware_whiteouttest", "blackducksoftware/whiteouttest", "1.0", "1.0", "var_lib_dpkg", true, null, true);
    }

    @Test
    public void testAggregateTarfileImageTwo() throws IOException, InterruptedException {
        testTar("build/images/test/aggregated.tar", "blackducksoftware_centos_minus_vim_plus_bacula", "blackducksoftware/centos_minus_vim_plus_bacula", "1.0", "1.0", "var_lib_rpm", true, null, true);
    }

    @Test
    public void testAlpineLatestTarRepoTagSpecified() throws IOException, InterruptedException {
        testTar("build/images/test/alpine.tar", "alpine", "alpine", "latest", "latest", "lib_apk", false, null, true);
    }

    @Test
    public void testAlpineLatestTarRepoTagNotSpecified() throws IOException, InterruptedException {
        testTar("build/images/test/alpine.tar", "alpine", null, null, "latest", "lib_apk", false, null, true);
    }

    // TODO this requires (a) minikube installed, (b) II-ws containers already running
    @Test
    public void testAlpineExistingContainer() throws IOException, InterruptedException, IntegrationException {
        execCmd("cp build/images/test/alpine.tar /Users/billings/tmp/working", 5000L);
        final String kubeIp = execCmd("minikube ip", 2000L);
        final List<String> additionalArgs = new ArrayList<>();
        additionalArgs.add(String.format("--imageinspector.url=http://%s:8080", kubeIp));
        // TODO TEMP hard coded path
        additionalArgs.add("--working.dir.path=/Users/billings/tmp/working");
        additionalArgs.add("--working.dir.path.imageinspector=/opt/blackduck/hub-imageinspector-ws/working");
        testTar("/Users/billings/tmp/working/alpine.tar", "alpine", null, null, "latest", "lib_apk", false, additionalArgs, false);
    }

    @Test
    public void testPullJar() throws IOException, InterruptedException {
        final File workingDir = new File("test/pulljar");
        FileUtils.deleteDirectory(workingDir);
        workingDir.mkdir();
        System.out.println(String.format("workingDir: %s", workingDir.getAbsolutePath()));
        final FilenameFilter jarFileFilter = getJarFilenameFilter();
        final File[] jarFilesBefore = workingDir.listFiles(jarFileFilter);
        assertTrue(String.format("%s should be an empty directory", workingDir.getAbsolutePath()), jarFilesBefore.length == 0);

        final List<String> partialCmd = Arrays.asList("../../build/hub-docker-inspector.sh", "--pulljar");
        // Arrays.asList returns a fixed size list; need a variable sized list
        final List<String> fullCmd = new ArrayList<>();
        fullCmd.addAll(partialCmd);

        System.out.println(String.format("Running --pulljar end to end test"));
        // TODO eliminate redundancy w/ execCmd
        final ProcessBuilder pb = new ProcessBuilder(fullCmd);
        final Map<String, String> env = pb.environment();
        final String oldPath = System.getenv("PATH");
        final String newPath = String.format("/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:%s", oldPath);
        System.out.println(String.format("Adjusted path: %s", newPath));
        env.put("PATH", newPath);
        pb.redirectErrorStream(true);
        pb.redirectOutput(Redirect.INHERIT);
        pb.directory(workingDir);
        final Process p = pb.start();
        final int retCode = p.waitFor();
        assertEquals(0, retCode);
        System.out.println("hub-docker-inspector --pulljar done; verifying results...");

        final File[] jarFilesAfter = workingDir.listFiles(jarFileFilter);
        final boolean foundOne = jarFilesAfter.length == 1;
        for (final File jarFile : jarFilesAfter) {
            System.out.println(String.format("Found jar file: %s", jarFile.getName()));
            jarFile.delete();
        }
        assertTrue("Expected a single pulled jar file", foundOne);
    }

    private FilenameFilter getJarFilenameFilter() {
        final FilenameFilter jarFileFilter = (dir, name) -> {
            if (name.endsWith(".jar")) {
                return true;
            } else {
                return false;
            }
        };
        return jarFileFilter;
    }

    private void testImage(final String inspectTarget, final String imageForBdioFilename, final String tagForBdioFilename, final String pkgMgrPathString, final boolean requireBdioMatch) throws IOException, InterruptedException {
        final String inspectTargetArg = String.format(String.format("--docker.image=%s", inspectTarget));
        test(imageForBdioFilename, pkgMgrPathString, null, null, tagForBdioFilename, inspectTargetArg, requireBdioMatch, null, true);
    }

    private void testTar(final String tarPath, final String imageForBdioFilename, final String repo, final String tag, final String tagForBdioFilename, final String pkgMgrPathString, final boolean requireBdioMatch,
            final List<String> additionalArgs, final boolean needWorkingDir)
            throws IOException, InterruptedException {
        final String inspectTarget = String.format(String.format("--docker.tar=%s", tarPath));
        test(imageForBdioFilename, pkgMgrPathString, repo, tag, tagForBdioFilename, inspectTarget, requireBdioMatch, additionalArgs, needWorkingDir);
    }

    private File getOutputImageTarFile(final String inspectTarget, final String imageForBdioFilename, final String tagForBdioFilename) {
        String outputTarFileName = null;

        if (inspectTarget.endsWith(".tar")) {
            final String inspectTargetFileName = inspectTarget.substring(18);

            outputTarFileName = String.format("test/output/%s", inspectTargetFileName);
        } else {
            final String inspectTargetFileName = String.format("%s_%s.tar", imageForBdioFilename, tagForBdioFilename);
            outputTarFileName = String.format("test/output/%s", inspectTargetFileName);
        }
        final File outputTarFile = new File(outputTarFileName);
        return outputTarFile;
    }

    private File getOutputContainerFileSystemFile(final String repo, final String tag) {
        final String outputContainerFileSystemFileName = String.format("test/output/%s_%s_containerfilesystem.tar.gz", repo, tag);
        final File outputTarFile = new File(outputContainerFileSystemFileName);
        return outputTarFile;
    }

    // TODO wow, this is ugly
    private void test(final String imageForBdioFilename, final String pkgMgrPathString, final String repo, final String tag, final String tagForBdioFilename, final String inspectTarget, final boolean requireBdioMatch,
            final List<String> additionalArgs, final boolean needWorkingDir)
            throws IOException, InterruptedException {
        final String workingDirPath = "test/endToEnd";
        try {
            FileUtils.deleteDirectory(new File(workingDirPath));
        } catch (final Exception e) {
            System.out.println(String.format("Unable to delete %s", workingDirPath));
        }

        final File expectedBdio = new File(String.format(String.format("src/integration-test/resources/bdio/%s_%s_%s_%s_bdio.jsonld", imageForBdioFilename, pkgMgrPathString, imageForBdioFilename, tagForBdioFilename)));
        if (requireBdioMatch) {
            assertTrue(expectedBdio.exists());
        }

        final File outputImageTarFile = getOutputImageTarFile(inspectTarget, imageForBdioFilename, tagForBdioFilename);
        Files.deleteIfExists(outputImageTarFile.toPath());
        assertFalse(outputImageTarFile.exists());

        final File outputContainerFileSystemFile = getOutputContainerFileSystemFile(imageForBdioFilename, tagForBdioFilename);
        Files.deleteIfExists(outputContainerFileSystemFile.toPath());
        assertFalse(outputContainerFileSystemFile.exists());

        final File actualBdio = new File(String.format(String.format("test/output/%s_%s_%s_%s_bdio.jsonld", imageForBdioFilename, pkgMgrPathString, imageForBdioFilename, tagForBdioFilename)));
        Files.deleteIfExists(actualBdio.toPath());
        assertFalse(actualBdio.exists());

        final ProgramVersion pgmVerObj = new ProgramVersion();
        pgmVerObj.init();
        final String programVersion = pgmVerObj.getProgramVersion();
        final List<String> partialCmd = Arrays.asList("build/hub-docker-inspector.sh", "--upload.bdio=false", String.format("--jar.path=build/libs/hub-docker-inspector-%s.jar", programVersion), "--output.path=test/output",
                "--output.include.dockertarfile=true", "--output.include.containerfilesystem=true", "--hub.always.trust.cert=true");
        // Arrays.asList returns a fixed size list; need a variable sized list
        final List<String> fullCmd = new ArrayList<>();
        fullCmd.addAll(partialCmd);
        if (repo != null) {
            fullCmd.add(String.format("--docker.image.repo=%s", repo));
        }
        if (tag != null) {
            fullCmd.add(String.format("--docker.image.tag=%s", tag));
        }
        fullCmd.add("--logging.level.com.blackducksoftware=DEBUG");
        if (needWorkingDir) {
            fullCmd.add(String.format("--working.dir.path=%s", workingDirPath));
        }
        fullCmd.add(inspectTarget);

        if (additionalArgs != null && additionalArgs.size() > 0) {
            fullCmd.addAll(additionalArgs);
        }

        System.out.println(String.format("Running end to end test on %s with command %s", inspectTarget, fullCmd.toString()));
        final ProcessBuilder pb = new ProcessBuilder(fullCmd);
        final Map<String, String> env = pb.environment();
        final String oldPath = System.getenv("PATH");
        final String newPath = String.format("/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:%s", oldPath);
        System.out.println(String.format("Adjusted path: %s", newPath));
        env.put("PATH", newPath);
        pb.redirectErrorStream(true);
        pb.redirectOutput(Redirect.INHERIT);
        final Process p = pb.start();
        final int retCode = p.waitFor();
        assertEquals(0, retCode);
        System.out.println("hub-docker-inspector done; verifying results...");
        assertTrue(actualBdio.exists());
        if (requireBdioMatch) {
            final List<String> exceptLinesContainingThese = new ArrayList<>();
            exceptLinesContainingThese.add("\"@id\":");

            final boolean outputBdioMatches = TestUtils.contentEquals(expectedBdio, actualBdio, exceptLinesContainingThese);
            assertTrue(outputBdioMatches);
        }

        assertTrue(outputImageTarFile.exists());
        assertTrue(outputContainerFileSystemFile.exists());
    }

    private static String execCmd(final String cmd, final long timeout) throws IOException, InterruptedException, IntegrationException {
        return execCmd(cmd, timeout, null);
    }

    private static String execCmd(final String cmd, final long timeout, final Map<String, String> env) throws IOException, InterruptedException, IntegrationException {
        System.out.println(String.format("Executing: %s", cmd));
        final ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
        pb.redirectOutput(Redirect.PIPE);
        pb.redirectError(Redirect.PIPE);
        pb.environment().put("PATH", "/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin");
        if (env != null) {
            pb.environment().putAll(env);
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
