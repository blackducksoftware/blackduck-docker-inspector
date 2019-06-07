package com.synopsys.integration.blackduck.dockerinspector;

import static org.junit.Assert.assertTrue;

import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.blackduck.dockerinspector.IntegrationTestCommon.Mode;
import com.synopsys.integration.exception.IntegrationException;

@Tag("integration")
public class DockerInspectorTest {
    private static int IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE = 8080;
    private static int IMAGE_INSPECTOR_PORT_IN_CONTAINER_ALPINE = 8080;
    private static int IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS = 8081;
    private static int IMAGE_INSPECTOR_PORT_IN_CONTAINER_CENTOS = 8081;
    private static int IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU = 8082;
    private static int IMAGE_INSPECTOR_PORT_IN_CONTAINER_UBUNTU = 8082;

    private static String SHARED_DIR_PATH_IN_CONTAINER = "/opt/blackduck/shared";

    private static File dirSharedWithContainer;
    private static File containerTargetDir;
    private static File containerOutputDir;

    private static ProgramVersion programVersion;
    private static String dateTimeStamp;
    private static Random random;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        random = new Random();
        dateTimeStamp = getTimestamp();
        programVersion = new ProgramVersion();
        programVersion.init();
        printDockerVersion();
        System.out.printf("Running containers:\n%s\n", getRunningContainers(false));
        System.out.printf("All containers:\n%s\n", getAllContainers(false));
        removeDockerInspectorContainers();
        System.out.printf("All containers:\n%s\n", getAllContainers(false));
        startContainer("alpine", IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE, IMAGE_INSPECTOR_PORT_IN_CONTAINER_ALPINE);
        startContainer("centos", IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS, IMAGE_INSPECTOR_PORT_IN_CONTAINER_CENTOS);
        startContainer("ubuntu", IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU, IMAGE_INSPECTOR_PORT_IN_CONTAINER_UBUNTU);

        boolean alpineUp = false;
        boolean centosUp = false;
        boolean ubuntuUp = false;
        for (int i = 0; i < 10; i++) {
            Thread.sleep(10000L);
            if (!alpineUp) {
                alpineUp = isUp(IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE);
            }
            if (!centosUp) {
                centosUp = isUp(IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS);
            }
            if (!ubuntuUp) {
                ubuntuUp = isUp(IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU);
            }
            if (alpineUp && centosUp && ubuntuUp) {
                break;
            }
        }
        assertTrue(alpineUp && centosUp && ubuntuUp);

        final File testDir = new File(TestUtils.TEST_DIR_REL_PATH);
        dirSharedWithContainer = new File(testDir, "containerShared");
        containerTargetDir = new File(dirSharedWithContainer, "target");
        containerOutputDir = new File(dirSharedWithContainer, "output");

        createWriteableDirTolerantly(testDir);
        createWriteableDirTolerantly(dirSharedWithContainer);
        createWriteableDirTolerantly(containerTargetDir);
        createWriteableDirTolerantly(containerOutputDir);
    }

    private static String getTimestamp() {
        String timestamp = Long.toString(new Date().getTime());
        final int len = timestamp.length();
        if (len > 8) {
            timestamp = timestamp.substring(len - 8);
        }
        return timestamp;
    }

    @AfterAll
    public static void tearDownAfterClass() throws Exception {
        cleanUpContainers();
    }

    private static void cleanUpContainers() throws InterruptedException {
        stopContainer("alpine");
        stopContainer("centos");
        stopContainer("ubuntu");
        Thread.sleep(30000L);
        removeContainer("alpine");
        removeContainer("centos");
        removeContainer("ubuntu");
        Thread.sleep(10000L);
        ensureContainerRemoved("alpine");
        ensureContainerRemoved("centos");
        ensureContainerRemoved("ubuntu");
    }
    
    @Test
    public void testUbuntu1404LayeredIncludeRemoved() throws IOException, InterruptedException, IntegrationException {
        List<String> additionalArgs = new ArrayList<>();
        additionalArgs.add("--bdio.organize.components.by.layer=true");
        additionalArgs.add("--bdio.include.removed.components=true");
        additionalArgs.add("--blackduck.codelocation.prefix=layeredIncludeRemoved");
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU;
        testTarUsingExistingContainer(null, null, "build/images/test/ubuntu1404.tar", portOnHost, additionalArgs, true, "testUbuntu1404LayeredIncludeRemoved", true);
    }

    @Test
    public void testUbuntuStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(random, programVersion, "ubuntu:17.04", "ubuntu", "17.04", false, Mode.DEFAULT, null, "dpkg", null, 10, null, null, "ubuntu_17.04_DPKG");
    }

    @Test
    public void testAlpineStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(random, programVersion, "alpine:3.6", "alpine", "3.6", false, Mode.SPECIFY_II_DETAILS, null, "apk-", null, 5, null, null, "alpine_3.6_APK");
    }

    @Test
    public void testCentosStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(random, programVersion, "centos:7.3.1611", "centos", "7.3.1611", false, Mode.SPECIFY_II_DETAILS, null, "openssl-libs", null, 15, null, null, "centos_7.3.1611_RPM");
    }

    @Test
    public void testBusybox() throws IOException, InterruptedException, IntegrationException {
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE;
        testImageUsingExistingContainer("busybox:latest", portOnHost, true, 0, null, null, "busybox_latest_noPkgMgr");
    }

    @Test
    public void testAlpineLatest() throws IOException, InterruptedException, IntegrationException {
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE;
        testImageUsingExistingContainer("alpine", portOnHost, false, 5, "apk-", null, "alpine_latest_APK");
    }

    @Test
    public void testBlackDuckWebapp() throws IOException, InterruptedException, IntegrationException {
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE;
        testImageUsingExistingContainer("blackducksoftware/hub-webapp:4.0.0", portOnHost, true, 0, null, null, "blackducksoftware_hub-webapp_4.0.0_APK");
    }

    @Test
    public void testBlackDuckZookeeper() throws IOException, InterruptedException, IntegrationException {
            final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE;
            testImageUsingExistingContainer("blackducksoftware/hub-zookeeper:4.0.0", portOnHost, true, 0, null, null, "blackducksoftware_hub-zookeeper_4.0.0_APK");
    }

    @Test
    public void testTomcat() throws IOException, InterruptedException, IntegrationException {
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU;
        testImageUsingExistingContainer("tomcat:6.0.53-jre7", portOnHost, false, 5, "dpkg", null, "tomcat_6.0.53-jre7_DPKG");
    }

    @Test
    public void testRhel() throws IOException, InterruptedException, IntegrationException {
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS;
        testImageUsingExistingContainer("dnplus/rhel:6.5", portOnHost, false, 10, "rpm", null, "dnplus_rhel_6.5_RPM");
    }

    @Test
    public void testFedora() throws IOException, InterruptedException, IntegrationException {
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS;
        testImageUsingExistingContainer("fedora:latest", portOnHost, false, 10, "fedora-", "@fedora", "fedora_latest_RPM");
    }

    @Test
    public void testOpenSuseForge() throws IOException, InterruptedException, IntegrationException {
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS;
        testImageUsingExistingContainer("opensuse/portus:2.4", portOnHost, false, 10, null, "@opensuse", "opensuse_portus_opensuse_2.4_RPM");
    }

    @Test
    public void testNonLinux() throws IOException, InterruptedException, IntegrationException {
        final String repo = "osless";
        final String tag = "1.0";
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS;
        testTarUsingExistingContainer(repo, tag, "src/test/resources/osless.tar", portOnHost, null, true, "osless_1.0_noPkgMgr", false);
    }

    @Test
    public void testWhiteout() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/whiteouttest";
        final String tag = "1.0";
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS;
        testTarUsingExistingContainer(repo, tag, "build/images/test/whiteouttest.tar", portOnHost, null, true, "blackducksoftware_whiteouttest_1.0_DPKG", false);
    }

    @Test
    public void testAggregateTarfileImageOne() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/whiteouttest";
        final String tag = "1.0";
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS;
        testTarUsingExistingContainer(repo, tag, "build/images/test/aggregated.tar", portOnHost, null, true, "blackducksoftware_whiteouttest_1.0_DPKG", false);
    }

    @Test
    public void testAggregateTarfileImageTwo() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/centos_minus_vim_plus_bacula";
        final String tag = "1.0";
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS;
        testTarUsingExistingContainer(repo, tag, "build/images/test/aggregated.tar", portOnHost, null, true, "blackducksoftware_centos_minus_vim_plus_bacula_1.0_RPM", false);
    }

    @Test
    public void testAlpineLatestTarRepoTagSpecified() throws IOException, InterruptedException, IntegrationException {
        final String repo = "alpine";
        final String tag = "latest";
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE;
        testTarUsingExistingContainer(repo, tag, "build/images/test/alpine.tar", portOnHost, null, false, "alpine_latest_APK", false);
    }

    @Test
    public void testAlpineLatestTarRepoTagNotSpecified() throws IOException, InterruptedException, IntegrationException {
        final String repo = null;
        final String tag = null;
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE;
        testTarUsingExistingContainer(repo, tag, "build/images/test/alpine.tar", portOnHost, null, false, "alpine_latest_APK", false);
    }

    @Test
    public void testAlpineUsingExistingAlpineContainer() throws IOException, InterruptedException, IntegrationException {
        final String targetRepo = null;
        final String targetTag = null;
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE;
        testTarUsingExistingContainer(targetRepo, targetTag,  "build/images/test/alpine36.tar",portOnHost, null, true, "null_null_APK", false);
    }

    @Test
    public void testWhiteoutUsingExistingAlpineContainer() throws IOException, InterruptedException, IntegrationException {
        final String targetRepo = "blackducksoftware/whiteouttest";
        final String targetTag = "1.0";
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE;
        testTarUsingExistingContainer(targetRepo, targetTag,  "build/images/test//whiteouttest.tar", portOnHost, null, true, "blackducksoftware_whiteouttest_1.0_DPKG", false);
    }

    @Test
    public void testCentosUsingExistingAlpineContainer() throws IOException, InterruptedException, IntegrationException {
        final String targetRepo = "blackducksoftware/centos_minus_vim_plus_bacula";
        final String targetTag = "1.0";
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE;
        testTarUsingExistingContainer(targetRepo, targetTag,  "build/images/test/centos_minus_vim_plus_bacula.tar",portOnHost, null, true, "blackducksoftware_centos_minus_vim_plus_bacula_1.0_RPM", false);
    }

    @Test
    public void testUbuntuUsingExistingCentosContainer() throws IOException, InterruptedException, IntegrationException {
        final String targetRepo = null; // the image in this tarfile is not tagged
        final String targetTag = null;
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS;
        testTarUsingExistingContainer(targetRepo, targetTag,  "build/images/test/ubuntu1404.tar",portOnHost, null, true, "null_null_DPKG", false);
    }


    @Test
    public void testExcludePlatformComponents() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/centos_minus_vim_plus_bacula";
        final String tag = "1.0";
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU;
        List<String> additionalArgs = new ArrayList<>();
        additionalArgs.add("--docker.platform.top.layer.id=sha256:1bcfbfaf95f95ea8a28711c83085dbbeceefa11576e1c889304aa5bacbaa6ac2");
        testTarUsingExistingContainer(repo, tag, "build/images/test/aggregated.tar", portOnHost, additionalArgs, true, "blackducksoftware_centos_minus_vim_plus_bacula_1.0_app_RPM", false);
    }

    private void testTarUsingExistingContainer(final String targetRepo, final String targetTag, final String tarFilePath, final int portOnHost,
        List<String> additionalArgs, final boolean requireBdioMatch, final String codelocationName, final boolean testSquashedImageGeneration)
            throws IOException, InterruptedException, IntegrationException {
        final File targetTar = new File(tarFilePath);
        final String tarFileName = targetTar.getName();
        final String tarFileBaseName = tarFileName.substring(0, tarFileName.length()-".tar".length());
        final File targetTarInSharedDir = new File(containerTargetDir, tarFileName);
        FileUtils.copyFile(targetTar, targetTarInSharedDir);
        targetTarInSharedDir.setReadable(true, false);
        if (additionalArgs == null) {
            additionalArgs = new ArrayList<>();
        }
        additionalArgs.add(String.format("--imageinspector.service.url=http://localhost:%d", portOnHost));
        additionalArgs.add(String.format("--shared.dir.path.local=%s", dirSharedWithContainer.getAbsolutePath()));
        additionalArgs.add(String.format("--shared.dir.path.imageinspector=%s", SHARED_DIR_PATH_IN_CONTAINER));
        final File outputContainerFileSystemFile = new File(String.format("%s/output/%s_containerfilesystem.tar.gz", TestUtils.TEST_DIR_REL_PATH, tarFileBaseName));
        File outputSquashedImageFile = null;
        if (testSquashedImageGeneration) {
            outputSquashedImageFile = new File(String.format("%s/output/%s_squashedimage.tar.gz", TestUtils.TEST_DIR_REL_PATH, tarFileBaseName));
            additionalArgs.add("--output.include.squashedimage=true");
        }
        IntegrationTestCommon.testTar(random, programVersion, targetTarInSharedDir.getAbsolutePath(), targetRepo, targetTag, requireBdioMatch, Mode.NO_SERVICE_START, null, additionalArgs, outputContainerFileSystemFile, outputSquashedImageFile, null, codelocationName);
    }

    private void testImageUsingExistingContainer(final String inspectTargetImageRepoTag, final int portOnHost, final boolean requireBdioMatch, final int minNumberOfComponentsExpected,
        final String outputBomMustContainComponentPrefix,
        final String outputBomMustContainExternalSystemTypeId,
        final String codelocationName)
        throws IOException, InterruptedException, IntegrationException {

        final List<String> additionalArgs = new ArrayList<>();
        additionalArgs.add(String.format("--imageinspector.service.url=http://localhost:%d", portOnHost));
        additionalArgs.add(String.format("--shared.dir.path.local=%s", dirSharedWithContainer.getAbsolutePath()));
        additionalArgs.add(String.format("--shared.dir.path.imageinspector=%s", SHARED_DIR_PATH_IN_CONTAINER));
        IntegrationTestCommon.testImage(random, programVersion, inspectTargetImageRepoTag, null, null, requireBdioMatch,
            Mode.NO_SERVICE_START, null, outputBomMustContainComponentPrefix, outputBomMustContainExternalSystemTypeId, minNumberOfComponentsExpected, additionalArgs, null, codelocationName);
    }

    private static void createWriteableDirTolerantly(final File dir) {
        System.out.printf("Creating and setting a+wx permission on: %s\n", dir.getAbsolutePath());
        createDirTolerantly(dir);
        setWriteExecutePermissionsTolerantly(dir);
        logPermissions(dir);
    }

    private static void logPermissions(final File dir) {
        Set<PosixFilePermission> perms;
        try {
            perms = Files.getPosixFilePermissions(dir.toPath());
            System.out.printf("* Dir %s now has perms: %s\n", dir.getAbsolutePath(), perms.toString());
        } catch (final IOException e) {
            System.out.printf("Unable to read back perms for dir %s: %s\n", dir.getAbsolutePath(), e.getMessage());
        }
    }

    private static void createDirTolerantly(final File dir) {
        try {
            dir.mkdirs();
        } catch (final Exception e) {
            System.out.printf("Error creating directory %s: %s\n", dir.getAbsoluteFile(), e.getMessage());
        }
        if (!dir.exists()) {
            System.out.printf("ERROR: Attempted to create directory %s, but it still does not exist\n", dir.getAbsoluteFile());
        }
    }

    private static void setWriteExecutePermissionsTolerantly(final File file) {
        try {
            file.setWritable(true, false);
        } catch (final Exception e) {
            System.out.printf("Error making directory %s writeable: %s\n", file.getAbsolutePath(), e.getMessage());
        }
        try {
            file.setExecutable(true, false);
        } catch (final Exception e) {
            System.out.printf("Error making directory %s writeable: %s\n", file.getAbsolutePath(), e.getMessage());
        }
    }

    private static boolean isUp(final int port) {
        String response;
        try {
            response = TestUtils.execCmd(String.format("curl -i http://localhost:%d/health", port), 30000L, true, null);
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
        final String cmd = String.format("docker run -d -t --name %s -p %d:%d -v \"$(pwd)\"/%s/containerShared:%s blackducksoftware/%s-%s:%s",
                containerName, portOnHost,
                portInContainer,
                TestUtils.TEST_DIR_REL_PATH,
                SHARED_DIR_PATH_IN_CONTAINER,
                programVersion.getInspectorImageFamily(), imageInspectorPlatform, programVersion.getInspectorImageVersion());
        TestUtils.execCmd(cmd, 120000L, true, null);
    }

    private static String getContainerName(final String imageInspectorPlatform) {
        return String.format("dockerInspectorTestImageInspector_%s_%s", imageInspectorPlatform, dateTimeStamp);
    }

    private static void printDockerVersion() {
        try {
            TestUtils.execCmd("docker version", 20000L, true, null);
        } catch (final Exception e) {
            System.out.printf("Error running docker version command: %s\n", e.getMessage());
        }
    }

    private static void stopContainer(final String imageInspectorPlatform) {
        final String containerName = getContainerName(imageInspectorPlatform);
        try {
            TestUtils.execCmd(String.format("docker stop %s", containerName), 120000L, true, null);
        } catch (final Exception e) {
            System.out.printf("Error stopping container %s: %s\n", containerName, e.getMessage());
        }
    }

    private static void removeContainer(final String imageInspectorPlatform) {
        final String containerName = getContainerName(imageInspectorPlatform);
        try {
            TestUtils.execCmd(String.format("docker rm -f %s", containerName), 120000L, true, null);
        } catch (final Exception e) {
            System.out.printf("Error removing container %s: %s\n", containerName, e.getMessage());
        }
    }

    private static void ensureContainerRemoved(final String imageInspectorPlatform) {
        final String containerName = getContainerName(imageInspectorPlatform);
        String dockerPsResponse;
        boolean containerStillExists = true;
        for (int tryCount = 0; tryCount < 20; tryCount++) {
            System.out.printf("Checking to see if container %s was removed\n", containerName);
            try {
                dockerPsResponse = getAllContainers(true);
                if (!dockerPsResponse.contains(containerName)) {
                    containerStillExists = false;
                    System.out.printf("Container %s was removed\n", containerName);
                    break;
                }
                Thread.sleep(5000L);
            } catch (final Exception e) {
                System.out.printf("Error stopping container %s: %s\n", containerName, e.getMessage());
            }
        }
        if (containerStillExists) {
            System.out.printf("ERROR: Failed to remove container %s\n", containerName);
        }
    }

    private static String getAllContainers(final boolean logStdout) throws IOException, InterruptedException, IntegrationException {
        return TestUtils.execCmd("docker ps -a", 120000L, logStdout, null);
    }

    private static String getRunningContainers(final boolean logStdout) throws IOException, InterruptedException, IntegrationException {
        return TestUtils.execCmd("docker ps", 120000L, logStdout, null);
    }

    private static void removeDockerInspectorContainers() throws IOException, InterruptedException, IntegrationException {
        System.out.println("Stopping/Removing docker inspector containers");
        final String psAllOutput = TestUtils.execCmd("docker ps -a", 120000L, false, null);
        final String[] lines = psAllOutput.split("\n");
        for (final String line : lines) {
            System.out.printf("Line: %s\n", line);
            if (line.startsWith("CONTAINER")) {
                continue;
            }
            final String[] fields = line.split("\\s+");
            final String containerName = fields[fields.length - 1];
            System.out.printf("Container name: %s\n", containerName);
            if (containerName.startsWith("blackduck-imageinspector-alpine_") || containerName.startsWith("blackduck-imageinspector-centos_") || containerName.startsWith("blackduck-imageinspector-ubuntu_")) {
                TestUtils.execCmd(String.format("docker stop %s", containerName), 120000L, false, null);
                Thread.sleep(10000L);
                TestUtils.execCmd(String.format("docker rm -f %s", containerName), 120000L, false, null);
            }
        }
    }

}
