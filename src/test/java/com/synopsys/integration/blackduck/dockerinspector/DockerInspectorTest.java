package com.synopsys.integration.blackduck.dockerinspector;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.test.annotation.IntegrationTest;

@Category(IntegrationTest.class)
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

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
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

    @AfterClass
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

    @Test // OK
    public void testUbuntuExec() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "ubuntu:17.04", "ubuntu", "17.04", "ubuntu_var_lib_dpkg_ubuntu_17.04_bdio.jsonld", false, false, false, "dpkg", 10, null, null);
    }

    @Test // OK
    public void testUbuntuStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "ubuntu:17.04", "ubuntu", "17.04", "ubuntu_17.04_DPKG_bdio.jsonld", false, true, false, "dpkg", 10, null, null);
    }

    @Test // OK
    public void testAlpineExec() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "alpine:3.6", "alpine", "3.6", "alpine_lib_apk_alpine_3.6_bdio.jsonld", false, false, false, "apk-", 5, null, null);
    }

    @Test
    public void testAlpineStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "alpine:3.6", "alpine", "3.6", "alpine_3.6_APK_bdio.jsonld", false, true, false, "apk-", 5, null, null);
    }

    @Test // OK
    public void testAlpineStartContainerForgeFromDistro() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "alpine:3.6", "alpine", "3.6", "alpine_3.6_APK_bdio.jsonld", false, true, true, "apk-", 5, null, null);
    }

    @Test
    public void testBusyboxExec() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "busybox:latest", "busybox", "latest", "busybox_noPkgMgr_busybox_latest_bdio.jsonld", false, false, false, null, 0, null, null);
    }

    @Test
    public void testBusyboxStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "busybox:latest", "busybox", "latest", "busybox_latest_noPkgMgr_bdio.jsonld", false, true, false, null, 0, null, null);
    }

    @Test
    public void testAlpineLatestExec() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "alpine", "alpine", "latest", "alpine_lib_apk_alpine_latest_bdio.jsonld", false, false, false, "apk-", 5, null, null);
    }

    @Test // OK
    public void testAlpineLatestStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "alpine", "alpine", "latest", "alpine_latest_APK_bdio.jsonld", false, true, false, "apk-", 5, null, null);
    }

    @Test
    public void testCentosExec() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "centos:7.3.1611", "centos", "7.3.1611", "centos_var_lib_rpm_centos_7.3.1611_bdio.jsonld", false, false, false, "rpm", 15, null, null);
    }

    @Test
    public void testCentosStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "centos:7.3.1611", "centos", "7.3.1611", "centos_7.3.1611_RPM_bdio.jsonld", false, true, false, "rpm", 15, null, null);
    }

    @Test
    public void testBlackDuckWebappExec() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "blackducksoftware/hub-webapp:4.0.0", "blackducksoftware_hub-webapp", "4.0.0", "blackducksoftware_hub-webapp_lib_apk_blackducksoftware_hub-webapp_4.0.0_bdio.jsonld", true, false,
                false, "apk-", 5, null, null);
    }

    @Test // OK
    public void testBlackDuckWebappStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "blackducksoftware/hub-webapp:4.0.0", "blackducksoftware_hub-webapp", "4.0.0", "blackducksoftware_hub-webapp_4.0.0_APK_bdio.jsonld", true, true, false, "apk-", 5, null, null);
    }

    @Test
    public void testBlackDuckZookeeperExec() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "blackducksoftware/hub-zookeeper:4.0.0", "blackducksoftware_hub-zookeeper", "4.0.0", "blackducksoftware_hub-zookeeper_lib_apk_blackducksoftware_hub-zookeeper_4.0.0_bdio.jsonld", true,
                false, false, "apk-", 5, null, null);
    }

    @Test
    public void testBlackDuckZookeeperStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "blackducksoftware/hub-zookeeper:4.0.0", "blackducksoftware_hub-zookeeper", "4.0.0", "blackducksoftware_hub-zookeeper_4.0.0_APK_bdio.jsonld", true, true, false, "apk-", 5, null, null);
    }

    @Test
    public void testTomcatExec() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "tomcat:6.0.53-jre7", "tomcat", "6.0.53-jre7", "tomcat_var_lib_dpkg_tomcat_6.0.53-jre7_bdio.jsonld", false, false, false, "dpkg", 5, null, null);
    }

    @Test
    public void testTomcatStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "tomcat:6.0.53-jre7", "tomcat", "6.0.53-jre7", "tomcat_6.0.53-jre7_DPKG_bdio.jsonld", false, true, false, "dpkg", 5, null, null);
    }

    @Test // OK
    public void testRhelExec() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "dnplus/rhel:6.5", "dnplus_rhel", "6.5", "dnplus_rhel_var_lib_rpm_dnplus_rhel_6.5_bdio.jsonld", false, false, false, "rpm", 10, null, null);
    }

    @Test // OK
    public void testRhelStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "dnplus/rhel:6.5", "dnplus_rhel", "6.5", "dnplus_rhel_6.5_RPM_bdio.jsonld", false, true, false, "rpm", 10, null, null);
    }

    @Test
    public void testWhiteoutExec() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/whiteouttest";
        final String tag = "1.0";
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("whiteouttest.tar");
        IntegrationTestCommon.testTar(programVersion, "build/images/test/whiteouttest.tar", "blackducksoftware_whiteouttest_var_lib_dpkg_blackducksoftware_whiteouttest_1.0_bdio.jsonld", repo, tag, true,
                false, null, true,
                outputContainerFileSystemFile, null);
    }

    @Test
    public void testWhiteoutStartContainer() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/whiteouttest";
        final String tag = "1.0";
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("whiteouttest.tar");
        IntegrationTestCommon.testTar(programVersion, "build/images/test/whiteouttest.tar", "blackducksoftware_whiteouttest_1.0_DPKG_bdio.jsonld", repo, tag, true, true, null, true,
                outputContainerFileSystemFile, null);
    }

    @Test
    public void testAggregateTarfileImageOneExec() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/whiteouttest";
        final String tag = "1.0";
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("aggregated.tar");
        IntegrationTestCommon.testTar(programVersion, "build/images/test/aggregated.tar", "blackducksoftware_whiteouttest_var_lib_dpkg_blackducksoftware_whiteouttest_1.0_bdio.jsonld", repo, tag, true, false,
                null, true, outputContainerFileSystemFile, null);
    }

    @Test
    public void testAggregateTarfileImageOneStartContainer() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/whiteouttest";
        final String tag = "1.0";
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("aggregated.tar");
        IntegrationTestCommon.testTar(programVersion, "build/images/test/aggregated.tar", "blackducksoftware_whiteouttest_1.0_DPKG_bdio.jsonld", repo, tag, true, true, null, true,
                outputContainerFileSystemFile, null);
    }

    @Test
    public void testAggregateTarfileImageTwoExec() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/centos_minus_vim_plus_bacula";
        final String tag = "1.0";
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("aggregated.tar");
        IntegrationTestCommon.testTar(programVersion, "build/images/test/aggregated.tar",
                "blackducksoftware_centos_minus_vim_plus_bacula_var_lib_rpm_blackducksoftware_centos_minus_vim_plus_bacula_1.0_bdio.jsonld", repo, tag, true, false, null, true, outputContainerFileSystemFile, null);
    }

    @Test
    public void testAggregateTarfileImageTwoStartContainer() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/centos_minus_vim_plus_bacula";
        final String tag = "1.0";
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("aggregated.tar");
        IntegrationTestCommon.testTar(programVersion, "build/images/test/aggregated.tar", "blackducksoftware_centos_minus_vim_plus_bacula_1.0_RPM_bdio.jsonld", repo, tag, true, true, null, true,
                outputContainerFileSystemFile, null);
    }

    @Test // OK
    public void testAlpineLatestTarRepoTagSpecifiedExec() throws IOException, InterruptedException, IntegrationException {
        final String repo = "alpine";
        final String tag = "latest";
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("alpine.tar");
        IntegrationTestCommon.testTar(programVersion, "build/images/test/alpine.tar", "alpine_lib_apk_alpine_latest_bdio.jsonld", repo, tag, false, false, null, true, outputContainerFileSystemFile, null);
    }

    @Test // OK
    public void testAlpineLatestTarRepoTagSpecifiedStartContainer() throws IOException, InterruptedException, IntegrationException {
        final String repo = "alpine";
        final String tag = "latest";
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("alpine.tar");
        IntegrationTestCommon.testTar(programVersion, "build/images/test/alpine.tar", "alpine_latest_APK_bdio.jsonld", repo, tag, false, true, null, true, outputContainerFileSystemFile, null);
    }

    @Test // OK
    public void testAlpineLatestTarRepoTagNotSpecifiedExec() throws IOException, InterruptedException, IntegrationException {
        final String repo = "alpine";
        final String tag = "latest";
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("alpine.tar");
        IntegrationTestCommon.testTar(programVersion, "build/images/test/alpine.tar", "alpine_lib_apk_alpine_latest_bdio.jsonld", repo, tag, false, false, null, true, outputContainerFileSystemFile, null);
    }

    @Test // OK
    public void testAlpineLatestTarRepoTagNotSpecifiedStartContainer() throws IOException, InterruptedException, IntegrationException {
        final String repo = "alpine";
        final String tag = "latest";
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("alpine.tar");
        IntegrationTestCommon.testTar(programVersion, "build/images/test/alpine.tar", "alpine_latest_APK_bdio.jsonld", repo, tag, false, true, null, true, outputContainerFileSystemFile, null);
    }

    @Test // OK
    public void testAlpineUsingExistingAlpineContainer() throws IOException, InterruptedException, IntegrationException {
        final String targetRepo = null;
        final String targetTag = null;
        final String tarFileBaseName = "alpine36";
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE;
        testUsingExistingContainer(targetRepo, targetTag, tarFileBaseName, "null_null_APK_bdio.jsonld", portOnHost);
    }

    @Test // OK
    public void testWhiteoutUsingExistingAlpineContainer() throws IOException, InterruptedException, IntegrationException {
        final String targetRepo = "blackducksoftware/whiteouttest";
        final String targetTag = "1.0";
        final String tarFileBaseName = "whiteouttest";
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE;
        testUsingExistingContainer(targetRepo, targetTag, tarFileBaseName, "blackducksoftware_whiteouttest_1.0_DPKG_bdio.jsonld", portOnHost);
    }

    @Test // OK
    public void testCentosUsingExistingAlpineContainer() throws IOException, InterruptedException, IntegrationException {
        final String targetRepo = "blackducksoftware/centos_minus_vim_plus_bacula";
        final String targetTag = "1.0";
        final String tarFileBaseName = "centos_minus_vim_plus_bacula";
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE;
        testUsingExistingContainer(targetRepo, targetTag, tarFileBaseName, "blackducksoftware_centos_minus_vim_plus_bacula_1.0_RPM_bdio.jsonld", portOnHost);
    }

    @Test // OK
    public void testUbuntuUsingExistingCentosContainer() throws IOException, InterruptedException, IntegrationException {
        final String targetRepo = null; // the image in this tarfile is not tagged
        final String targetTag = null;
        final String tarFileBaseName = "ubuntu1404";
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS;
        testUsingExistingContainer(targetRepo, targetTag, tarFileBaseName, "null_null_DPKG_bdio.jsonld", portOnHost);
    }

    private void testUsingExistingContainer(final String targetRepo, final String targetTag, final String tarFileBaseName, final String bdioFilename, final int portOnHost)
            throws IOException, InterruptedException, IntegrationException {

        final String tarFileName = String.format("%s.tar", tarFileBaseName);
        final File targetTar = new File(containerTargetDir, tarFileName);
        FileUtils.copyFile(new File(String.format("build/images/test/%s", tarFileName)), targetTar);
        targetTar.setReadable(true, false);
        final List<String> additionalArgs = new ArrayList<>();
        additionalArgs.add(String.format("--imageinspector.service.url=http://localhost:%d", portOnHost));
        additionalArgs.add(String.format("--shared.dir.path.local=%s", dirSharedWithContainer.getAbsolutePath()));
        additionalArgs.add(String.format("--shared.dir.path.imageinspector=%s", SHARED_DIR_PATH_IN_CONTAINER));
        final File outputContainerFileSystemFile = new File(String.format("%s/output/%s_containerfilesystem.tar.gz", TestUtils.TEST_DIR_REL_PATH, tarFileBaseName));
        IntegrationTestCommon.testTar(programVersion, targetTar.getAbsolutePath(), bdioFilename, targetRepo, targetTag, true, false, additionalArgs, false, outputContainerFileSystemFile, null);
    }

    private static void createWriteableDirTolerantly(final File dir) {
        System.out.printf("Creating and setting a+wx permission on: %s\n", dir.getAbsolutePath());
        createDirTolerantly(dir);
        setWriteExecutePermissionsTolerantly(dir);
        logPermissions(dir);
    }

    private static void logPermissions(final File dir) {
        Set<PosixFilePermission> perms = null;
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
