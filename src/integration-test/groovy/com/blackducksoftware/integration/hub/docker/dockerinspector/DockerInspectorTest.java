package com.blackducksoftware.integration.hub.docker.dockerinspector;

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
    private static int IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE = 8080;
    private static int IMAGE_INSPECTOR_PORT_IN_CONTAINER_ALPINE = 8080;
    private static int IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS = 8081;
    private static int IMAGE_INSPECTOR_PORT_IN_CONTAINER_CENTOS = 8081;
    private static String IMAGE_INSPECTOR_REPO_BASE = "hub-imageinspector-ws";
    private static String IMAGE_INSPECTOR_TAG = "1.0.1-SNAPSHOT";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        try {
            final boolean created = new File("test").mkdir();
            System.out.println(String.format("test dir created: %b", created));
        } catch (final Exception e) {
            System.out.println(String.format("mkdir test: %s", e.getMessage()));
        }

        startContainer("alpine", IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE, IMAGE_INSPECTOR_PORT_IN_CONTAINER_ALPINE);
        startContainer("centos", IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS, IMAGE_INSPECTOR_PORT_IN_CONTAINER_CENTOS);

        boolean alpineUp = false;
        boolean centosUp = false;
        for (int i = 0; i < 10; i++) {
            Thread.sleep(10000L);
            if (!alpineUp) {
                alpineUp = isUp(IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE);
            }
            if (!centosUp) {
                centosUp = isUp(IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS);
            }
            if (alpineUp && centosUp) {
                break;
            }
        }
        assertTrue(alpineUp && centosUp);
    }

    private static boolean isUp(final int port) {
        String response;
        try {
            response = execCmd(String.format("curl -i http://localhost:%d/health", port), 30000L);
        } catch (IOException | InterruptedException | IntegrationException e) {
            return false;
        }
        if (response.startsWith("HTTP/1.1 200")) {
            return true;
        }
        return false;
    }

    private static void startContainer(final String imageInspectorPlatform, final int portOnHost, final int portInContainer) throws IOException, InterruptedException, IntegrationException {
        final String containerName = getContainerName(imageInspectorPlatform);
        final String cmd = String.format("docker run -d -t --name %s -p %d:%d -v \"$(pwd)\"/build/shared:/opt/blackduck/hub-imageinspector-ws/shared blackducksoftware/%s-%s:%s",
                containerName, portOnHost, portInContainer,
                IMAGE_INSPECTOR_REPO_BASE, imageInspectorPlatform, IMAGE_INSPECTOR_TAG);
        execCmd(cmd, 120000L);
    }

    private static String getContainerName(final String imageInspectorPlatform) {
        return String.format("dockerInspectorTestImageInspector_%s", imageInspectorPlatform);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        stopContainer("alpine");
        stopContainer("centos");
    }

    private static void stopContainer(final String imageInspectorPlatform) {
        final String containerName = getContainerName(imageInspectorPlatform);
        try {
            execCmd(String.format("docker stop %s", containerName), 120000L);
        } catch (final Exception e) {
        }
        try {
            execCmd(String.format("docker rm %s", containerName), 120000L);
        } catch (final Exception e) {
        }
    }

    @Test
    public void testUbuntu() throws IOException, InterruptedException, IntegrationException {
        testImage("ubuntu:17.04", "ubuntu", "17.04", "var_lib_dpkg", true);
    }

    @Test
    public void testAlpine() throws IOException, InterruptedException, IntegrationException {
        testImage("alpine:3.6", "alpine", "3.6", "lib_apk", true);
    }

    @Test
    public void testAlpineLatest() throws IOException, InterruptedException, IntegrationException {
        testImage("alpine", "alpine", "latest", "lib_apk", false);
    }

    @Test
    public void testCentos() throws IOException, InterruptedException, IntegrationException {
        testImage("centos:7.3.1611", "centos", "7.3.1611", "var_lib_rpm", true);
    }

    @Test
    public void testHubWebapp() throws IOException, InterruptedException, IntegrationException {
        testImage("blackducksoftware/hub-webapp:4.0.0", "blackducksoftware_hub-webapp", "4.0.0", "lib_apk", true);
    }

    @Test
    public void testHubZookeeper() throws IOException, InterruptedException, IntegrationException {
        testImage("blackducksoftware/hub-zookeeper:4.0.0", "blackducksoftware_hub-zookeeper", "4.0.0", "lib_apk", true);
    }

    @Test
    public void testTomcat() throws IOException, InterruptedException, IntegrationException {
        testImage("tomcat:6.0.53-jre7", "tomcat", "6.0.53-jre7", "var_lib_dpkg", true);
    }

    @Test
    public void testRhel() throws IOException, InterruptedException, IntegrationException {
        testImage("dnplus/rhel:6.5", "dnplus_rhel", "6.5", "var_lib_rpm", true);
    }

    @Test
    public void testWhiteout() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/whiteouttest";
        final String tag = "1.0";
        final File outputContainerFileSystemFile = getOutputContainerFileSystemFile(repo, tag);
        testTar("build/images/test/whiteouttest.tar", repo.replaceAll("/", "_"), repo, tag, tag, "var_lib_dpkg", true, null, true, outputContainerFileSystemFile);
    }

    @Test
    public void testAggregateTarfileImageOne() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/whiteouttest";
        final String tag = "1.0";
        final File outputContainerFileSystemFile = getOutputContainerFileSystemFile(repo, tag);
        testTar("build/images/test/aggregated.tar", repo.replaceAll("/", "_"), repo, tag, tag, "var_lib_dpkg", true, null, true, outputContainerFileSystemFile);
    }

    @Test
    public void testAggregateTarfileImageTwo() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/centos_minus_vim_plus_bacula";
        final String tag = "1.0";
        final File outputContainerFileSystemFile = getOutputContainerFileSystemFile(repo, tag);
        testTar("build/images/test/aggregated.tar", repo.replaceAll("/", "_"), repo, tag, tag, "var_lib_rpm", true, null, true, outputContainerFileSystemFile);
    }

    @Test
    public void testAlpineLatestTarRepoTagSpecified() throws IOException, InterruptedException, IntegrationException {
        final String repo = "alpine";
        final String tag = "latest";
        final File outputContainerFileSystemFile = getOutputContainerFileSystemFile(repo, tag);
        testTar("build/images/test/alpine.tar", repo.replaceAll("/", "_"), repo, tag, tag, "lib_apk", false, null, true, outputContainerFileSystemFile);
    }

    @Test
    public void testAlpineLatestTarRepoTagNotSpecified() throws IOException, InterruptedException, IntegrationException {
        final String repo = "alpine";
        final String tag = null;
        final File outputContainerFileSystemFile = getOutputContainerFileSystemFile(repo, tag);
        testTar("build/images/test/alpine.tar", repo, null, null, "latest", "lib_apk", false, null, true, outputContainerFileSystemFile);
    }

    @Test
    public void testAlpineUsingExistingAlpineContainer() throws IOException, InterruptedException, IntegrationException {
        final String targetRepo = "alpine";
        final String targetTag = "3.6";
        final String targetPkgMgrLib = "lib_apk";
        final String tarFileBaseName = "alpine36";
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE;
        final String imageInspectorPlatform = "alpine";
        testUsingExistingContainer(targetRepo, targetTag, targetPkgMgrLib, tarFileBaseName, imageInspectorPlatform, portOnHost);
    }

    @Test
    public void testCentosUsingExistingAlpineContainer() throws IOException, InterruptedException, IntegrationException {
        final String targetRepo = "blackducksoftware/centos_minus_vim_plus_bacula";
        final String targetTag = "1.0";
        final String targetPkgMgrLib = "var_lib_rpm";
        final String tarFileBaseName = "centos_minus_vim_plus_bacula";
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE;
        final String imageInspectorPlatform = "alpine";
        testUsingExistingContainer(targetRepo, targetTag, targetPkgMgrLib, tarFileBaseName, imageInspectorPlatform, portOnHost);
    }

    private void testUsingExistingContainer(final String targetRepo, final String targetTag, final String targetPkgMgrLib, final String tarFileBaseName, final String imageInspectorPlatform, final int portOnHost)
            throws IOException, InterruptedException, IntegrationException {
        final File sharedDir = new File("build/shared");
        final File targetDir = new File(sharedDir, "target");
        final File outputDir = new File(sharedDir, "output");
        targetDir.mkdirs();
        outputDir.mkdirs();
        outputDir.setWritable(true, false);
        final String tarFileName = String.format("%s.tar", tarFileBaseName);
        final File targetTar = new File(targetDir, tarFileName);
        FileUtils.copyFile(new File(String.format("build/images/test/%s", tarFileName)), targetTar);
        targetTar.setReadable(true, false);
        final List<String> additionalArgs = new ArrayList<>();
        additionalArgs.add(String.format("--imageinspector.url=http://localhost:%d", portOnHost));
        additionalArgs.add(String.format("--shared.dir.path.local=%s", sharedDir.getAbsolutePath()));
        additionalArgs.add(String.format("--shared.dir.path.imageinspector=/opt/blackduck/hub-imageinspector-ws/shared"));
        final File outputContainerFileSystemFile = new File(String.format("test/output/%s_containerfilesystem.tar.gz", tarFileBaseName));
        testTar(targetTar.getAbsolutePath(), targetRepo, null, null, targetTag, targetPkgMgrLib, true, additionalArgs, false, outputContainerFileSystemFile);
    }

    @Test
    public void testPullJar() throws IOException, InterruptedException, IntegrationException {
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
        execCmd(workingDir, String.join(" ", fullCmd), 30000L);
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

    private File getOutputContainerFileSystemFile(final String repo, final String tag) {
        final String outputContainerFileSystemFileName = String.format("test/output/%s_%s_containerfilesystem.tar.gz", repo.replaceAll("/", "_"), tag == null ? "latest" : tag);
        final File outputTarFile = new File(outputContainerFileSystemFileName);
        return outputTarFile;
    }

    private void testTar(final String inspectTargetTarfile, final String imageForBdioFilename, final String repo, final String tag, final String tagForBdioFilename, final String pkgMgrPathString, final boolean requireBdioMatch,
            final List<String> additionalArgs, final boolean needWorkingDir, final File outputContainerFileSystemFile)
            throws IOException, InterruptedException, IntegrationException {

        final String inspectTargetArg = String.format("--docker.tar=%s", inspectTargetTarfile);

        ensureFileDoesNotExist(outputContainerFileSystemFile);

        final File actualBdio = new File(String.format(String.format("test/output/%s_%s_%s_%s_bdio.jsonld", imageForBdioFilename.replaceAll("/", "_"), pkgMgrPathString, imageForBdioFilename.replaceAll("/", "_"), tagForBdioFilename)));
        ensureFileDoesNotExist(actualBdio);

        final List<String> partialCmd = Arrays.asList("build/hub-docker-inspector.sh", "--upload.bdio=false", String.format("--jar.path=build/libs/hub-docker-inspector-%s.jar", getProgramVersion()), "--output.path=test/output",
                "--output.include.containerfilesystem=true", "--hub.always.trust.cert=true");

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
            final File workingDir = new File("test/endToEnd");
            deleteDirIfExists(workingDir);
            fullCmd.add(String.format("--working.dir.path=%s", workingDir.getAbsolutePath()));
        }
        fullCmd.add(inspectTargetArg);

        if (additionalArgs != null && additionalArgs.size() > 0) {
            fullCmd.addAll(additionalArgs);
        }

        System.out.println(String.format("Running end to end test on %s with command %s", inspectTargetTarfile, fullCmd.toString()));
        execCmd(String.join(" ", fullCmd), 240000L);
        System.out.println("hub-docker-inspector done; verifying results...");
        System.out.printf("Expecting output BDIO file: %s\n", actualBdio.getAbsolutePath());
        assertTrue(actualBdio.exists());
        if (requireBdioMatch) {
            final File expectedBdio = new File(
                    String.format(String.format("src/integration-test/resources/bdio/%s_%s_%s_%s_bdio.jsonld", imageForBdioFilename.replaceAll("/", "_"), pkgMgrPathString, imageForBdioFilename.replaceAll("/", "_"), tagForBdioFilename)));
            final List<String> exceptLinesContainingThese = new ArrayList<>();
            exceptLinesContainingThese.add("\"@id\":");
            final boolean outputBdioMatches = TestUtils.contentEquals(expectedBdio, actualBdio, exceptLinesContainingThese);
            assertTrue(outputBdioMatches);
        }

        assertTrue(outputContainerFileSystemFile.exists());
    }

    private void testImage(final String inspectTargetImageRepoTag, final String repo, final String tag, final String pkgMgrPathString, final boolean requireBdioMatch)
            throws IOException, InterruptedException, IntegrationException {
        final File outputContainerFileSystemFile = getOutputContainerFileSystemFile(repo, tag);
        final String inspectTargetArg = String.format("--docker.image=%s", inspectTargetImageRepoTag);
        ensureFileDoesNotExist(outputContainerFileSystemFile);
        final File actualBdio = new File(String.format(String.format("test/output/%s_%s_%s_%s_bdio.jsonld", repo, pkgMgrPathString, repo, tag)));
        ensureFileDoesNotExist(actualBdio);

        final List<String> partialCmd = Arrays.asList("build/hub-docker-inspector.sh", "--upload.bdio=false", String.format("--jar.path=build/libs/hub-docker-inspector-%s.jar", getProgramVersion()), "--output.path=test/output",
                "--output.include.containerfilesystem=true", "--hub.always.trust.cert=true");

        final List<String> fullCmd = new ArrayList<>();
        fullCmd.addAll(partialCmd);
        if (repo != null) {
            fullCmd.add(String.format("--docker.image.repo=%s", repo));
        }
        if (tag != null) {
            fullCmd.add(String.format("--docker.image.tag=%s", tag));
        }
        fullCmd.add("--logging.level.com.blackducksoftware=DEBUG");
        final File workingDir = new File("test/endToEnd");
        deleteDirIfExists(workingDir);
        fullCmd.add(String.format("--working.dir.path=%s", workingDir.getAbsolutePath()));
        fullCmd.add(inspectTargetArg);

        System.out.println(String.format("Running end to end test on %s with command %s", inspectTargetImageRepoTag, fullCmd.toString()));
        execCmd(String.join(" ", fullCmd), 30000L);
        System.out.println("hub-docker-inspector done; verifying results...");
        System.out.printf("Expecting output BDIO file: %s\n", actualBdio.getAbsolutePath());
        assertTrue(actualBdio.exists());
        if (requireBdioMatch) {
            final File expectedBdio = new File(String.format(String.format("src/integration-test/resources/bdio/%s_%s_%s_%s_bdio.jsonld", repo, pkgMgrPathString, repo, tag)));
            final List<String> exceptLinesContainingThese = new ArrayList<>();
            exceptLinesContainingThese.add("\"@id\":");
            final boolean outputBdioMatches = TestUtils.contentEquals(expectedBdio, actualBdio, exceptLinesContainingThese);
            assertTrue(outputBdioMatches);
        }

        assertTrue(outputContainerFileSystemFile.exists());
    }

    private String getProgramVersion() throws IOException {
        final ProgramVersion pgmVerObj = new ProgramVersion();
        pgmVerObj.init();
        final String programVersion = pgmVerObj.getProgramVersion();
        return programVersion;
    }

    private void ensureFileDoesNotExist(final File outputContainerFileSystemFile) throws IOException {
        Files.deleteIfExists(outputContainerFileSystemFile.toPath());
        assertFalse(outputContainerFileSystemFile.exists());
    }

    private void deleteDirIfExists(final File workingDir) {
        try {
            FileUtils.deleteDirectory(workingDir);
        } catch (final Exception e) {
            System.out.println(String.format("Unable to delete %s", workingDir.getAbsolutePath()));
        }
    }

    private static String execCmd(final File workingDir, final String cmd, final long timeout) throws IOException, InterruptedException, IntegrationException {
        return execCmd(workingDir, cmd, timeout, null);
    }

    private static String execCmd(final String cmd, final long timeout) throws IOException, InterruptedException, IntegrationException {
        return execCmd(null, cmd, timeout, null);
    }

    private static String execCmd(final File workingDir, final String cmd, final long timeout, final Map<String, String> env) throws IOException, InterruptedException, IntegrationException {
        System.out.println(String.format("Executing: %s", cmd));
        final ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
        pb.redirectOutput(Redirect.PIPE);
        pb.redirectError(Redirect.PIPE);
        if (workingDir != null) {
            pb.directory(workingDir);
        }
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
