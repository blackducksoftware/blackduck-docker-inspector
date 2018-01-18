package com.blackducksoftware.integration.hub.docker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.blackducksoftware.integration.hub.docker.imageinspector.TestUtils;

public class EndToEndTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
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
        testTar("whiteouttest.tar", "blackducksoftware_whiteouttest", "blackducksoftware/whiteouttest", "1.0", "1.0", "var_lib_dpkg", true);
    }

    @Test
    public void testAggregateTarfileImageOne() throws IOException, InterruptedException {
        testTar("aggregated.tar", "blackducksoftware_whiteouttest", "blackducksoftware/whiteouttest", "1.0", "1.0", "var_lib_dpkg", true);
    }

    @Test
    public void testAggregateTarfileImageTwo() throws IOException, InterruptedException {
        testTar("aggregated.tar", "blackducksoftware_centos_minus_vim_plus_bacula", "blackducksoftware/centos_minus_vim_plus_bacula", "1.0", "1.0", "var_lib_rpm", true);
    }

    @Test
    public void testAlpineLatestTarRepoTagSpecified() throws IOException, InterruptedException {
        testTar("alpine.tar", "alpine", "alpine", "latest", "latest", "lib_apk", false);
    }

    @Test
    public void testAlpineLatestTarRepoTagNotSpecified() throws IOException, InterruptedException {
        testTar("alpine.tar", "alpine", null, null, "latest", "lib_apk", false);
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
        final FilenameFilter jarFileFilter = new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                if (name.endsWith(".jar")) {
                    return true;
                } else {
                    return false;
                }
            }
        };
        return jarFileFilter;
    }

    private void testImage(final String inspectTarget, final String imageForBdioFilename, final String tagForBdioFilename, final String pkgMgrPathString, final boolean requireBdioMatch) throws IOException, InterruptedException {
        test(imageForBdioFilename, pkgMgrPathString, null, null, tagForBdioFilename, inspectTarget, requireBdioMatch);
    }

    private void testTar(final String tarFilename, final String imageForBdioFilename, final String repo, final String tag, final String tagForBdioFilename, final String pkgMgrPathString, final boolean requireBdioMatch)
            throws IOException, InterruptedException {
        final String inspectTarget = String.format(String.format("build/images/test/%s", tarFilename));
        test(imageForBdioFilename, pkgMgrPathString, repo, tag, tagForBdioFilename, inspectTarget, requireBdioMatch);
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

    private void test(final String imageForBdioFilename, final String pkgMgrPathString, final String repo, final String tag, final String tagForBdioFilename, final String inspectTarget, final boolean requireBdioMatch)
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

        final String programVersion = (new ProgramVersion()).getProgramVersion();
        final List<String> partialCmd = Arrays.asList("build/hub-docker-inspector.sh", "--dry.run=true", String.format("--jar.path=build/libs/hub-docker-inspector-%s.jar", programVersion), "--output.path=test/output",
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
        fullCmd.add(String.format("--working.dir.path=%s", workingDirPath));
        fullCmd.add(inspectTarget);

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
}
