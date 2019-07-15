package com.synopsys.integration.blackduck.dockerinspector;

import static org.junit.Assert.assertTrue;

import com.sun.xml.internal.bind.v2.TODO;
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

        final TestConfig testConfig = new TestConfigBuilder()
            .setTarFilePath("build/images/test/ubuntu1404.tar")
            .setPortOnHost(portOnHost)
            .setAdditionalArgs(additionalArgs)
            .setRequireBdioMatch(true)
            .setCodelocationName("testUbuntu1404LayeredIncludeRemoved")
            .setTestSquashedImageGeneration(true)
            .build();

        testTarUsingExistingContainer(testConfig);
    }


    // TODO NEED TESTS THAT EXCLUDE PATHS, BOTH START CONTAINER AND EXISTING, BOTH WITH REDIRECTS

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
        final TestConfig testConfig = new TestConfigBuilder()
                                          .setTarFilePath("src/test/resources/osless.tar")
                                          .setTargetRepo("osless")
                                          .setTargetTag("1.0")
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS)
                                          .setRequireBdioMatch(true)
                                          .setCodelocationName("osless_1.0_noPkgMgr")
                                          .build();

        testTarUsingExistingContainer(testConfig);
    }

    @Test
    public void testWhiteout() throws IOException, InterruptedException, IntegrationException {
        final TestConfig testConfig = new TestConfigBuilder()
                                          .setTarFilePath("build/images/test/whiteouttest.tar")
                                          .setTargetRepo("blackducksoftware/whiteouttest")
                                          .setTargetTag("1.0")
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS)
                                          .setRequireBdioMatch(true)
                                          .setCodelocationName("blackducksoftware_whiteouttest_1.0_DPKG")
                                          .build();

        testTarUsingExistingContainer(testConfig);
    }

    @Test
    public void testAggregateTarfileImageOne() throws IOException, InterruptedException, IntegrationException {
        final TestConfig testConfig = new TestConfigBuilder()
                                          .setTarFilePath("build/images/test/aggregated.tar")
                                          .setTargetRepo("blackducksoftware/whiteouttest")
                                          .setTargetTag("1.0")
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS)
                                          .setRequireBdioMatch(true)
                                          .setCodelocationName("blackducksoftware_whiteouttest_1.0_DPKG")
                                          .build();

        testTarUsingExistingContainer(testConfig);
    }

    @Test
    public void testAggregateTarfileImageTwo() throws IOException, InterruptedException, IntegrationException {
        final TestConfig testConfig = new TestConfigBuilder()
                                          .setTarFilePath("build/images/test/aggregated.tar")
                                          .setTargetRepo("blackducksoftware/centos_minus_vim_plus_bacula")
                                          .setTargetTag("1.0")
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS)
                                          .setRequireBdioMatch(true)
                                          .setCodelocationName("blackducksoftware_centos_minus_vim_plus_bacula_1.0_RPM")
                                          .build();

        testTarUsingExistingContainer(testConfig);
    }

    @Test
    public void testAlpineLatestTarRepoTagSpecified() throws IOException, InterruptedException, IntegrationException {
        final TestConfig testConfig = new TestConfigBuilder()
            .setTarFilePath("build/images/test/alpine.tar")
            .setTargetRepo("alpine")
            .setTargetTag("latest")
            .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE)
            .setRequireBdioMatch(false)
            .setCodelocationName("alpine_latest_APK")
            .build();

        testTarUsingExistingContainer(testConfig);
    }

    @Test
    public void testAlpineLatestTarRepoTagNotSpecified() throws IOException, InterruptedException, IntegrationException {
        final TestConfig testConfig = new TestConfigBuilder()
                                          .setTarFilePath("build/images/test/alpine.tar")
                                          .setTargetRepo(null)
                                          .setTargetTag(null)
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE)
                                          .setRequireBdioMatch(false)
                                          .setCodelocationName("alpine_latest_APK")
                                          .build();

        testTarUsingExistingContainer(testConfig);
    }

    @Test
    public void testAlpineUsingExistingAlpineContainer() throws IOException, InterruptedException, IntegrationException {
        final TestConfig testConfig = new TestConfigBuilder()
                                          .setTarFilePath("build/images/test/alpine36.tar")
                                          .setTargetRepo(null)
                                          .setTargetTag(null)
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE)
                                          .setRequireBdioMatch(true)
                                          .setCodelocationName("null_null_APK")
                                          .build();

        testTarUsingExistingContainer(testConfig);
    }

    @Test
    public void testWhiteoutUsingExistingAlpineContainer() throws IOException, InterruptedException, IntegrationException {
        final TestConfig testConfig = new TestConfigBuilder()
                                          .setTarFilePath("build/images/test//whiteouttest.tar")
                                          .setTargetRepo("blackducksoftware/whiteouttest")
                                          .setTargetTag("1.0")
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE)
                                          .setRequireBdioMatch(true)
                                          .setCodelocationName("blackducksoftware_whiteouttest_1.0_DPKG")
                                          .build();

        testTarUsingExistingContainer(testConfig);
    }

    @Test
    public void testCentosUsingExistingAlpineContainer() throws IOException, InterruptedException, IntegrationException {
        final TestConfig testConfig = new TestConfigBuilder()
                                          .setTarFilePath("build/images/test/centos_minus_vim_plus_bacula.tar")
                                          .setTargetRepo("blackducksoftware/centos_minus_vim_plus_bacula")
                                          .setTargetTag("1.0")
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE)
                                          .setRequireBdioMatch(true)
                                          .setCodelocationName("blackducksoftware_centos_minus_vim_plus_bacula_1.0_RPM")
                                          .build();


        testTarUsingExistingContainer(testConfig);
    }

    @Test
    public void testUbuntuUsingExistingCentosContainer() throws IOException, InterruptedException, IntegrationException {
        final TestConfig testConfig = new TestConfigBuilder()
                                          .setTarFilePath("build/images/test/ubuntu1404.tar")
                                          .setTargetRepo(null)
                                          .setTargetTag(null)
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS)
                                          .setRequireBdioMatch(true)
                                          .setCodelocationName("null_null_DPKG")
                                          .build();

        testTarUsingExistingContainer(testConfig);
    }


    @Test
    public void testExcludePlatformComponents() throws IOException, InterruptedException, IntegrationException {
        List<String> additionalArgs = new ArrayList<>();
        additionalArgs.add("--docker.platform.top.layer.id=sha256:1bcfbfaf95f95ea8a28711c83085dbbeceefa11576e1c889304aa5bacbaa6ac2");

        final TestConfig testConfig = new TestConfigBuilder()
                                          .setTarFilePath("build/images/test/aggregated.tar")
                                          .setTargetRepo("blackducksoftware/centos_minus_vim_plus_bacula")
                                          .setTargetTag("1.0")
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU)
                                          .setRequireBdioMatch(true)
                                          .setCodelocationName("blackducksoftware_centos_minus_vim_plus_bacula_1.0_app_RPM")
                                          .setAdditionalArgs(additionalArgs)
                                          .build();

        testTarUsingExistingContainer(testConfig);
    }

    private void testTarUsingExistingContainer(final TestConfig testConfig)
            throws IOException, InterruptedException, IntegrationException {
        final File targetTar = new File(testConfig.getTarFilePath());
        final String tarFileName = targetTar.getName();
        final String tarFileBaseName = tarFileName.substring(0, tarFileName.length()-".tar".length());
        final File targetTarInSharedDir = new File(containerTargetDir, tarFileName);
        FileUtils.copyFile(targetTar, targetTarInSharedDir);
        targetTarInSharedDir.setReadable(true, false);
        List<String> additionalArgs = testConfig.getAdditionalArgs();
        if (additionalArgs == null) {
            additionalArgs = new ArrayList<>();
        }
        additionalArgs.add(String.format("--imageinspector.service.url=http://localhost:%d", testConfig.getPortOnHost()));
        additionalArgs.add(String.format("--shared.dir.path.local=%s", dirSharedWithContainer.getAbsolutePath()));
        additionalArgs.add(String.format("--shared.dir.path.imageinspector=%s", SHARED_DIR_PATH_IN_CONTAINER));
        final File outputContainerFileSystemFile = new File(String.format("%s/output/%s_containerfilesystem.tar.gz", TestUtils.TEST_DIR_REL_PATH, tarFileBaseName));
        File outputSquashedImageFile = null;
        if (testConfig.isTestSquashedImageGeneration()) {
            outputSquashedImageFile = new File(String.format("%s/output/%s_squashedimage.tar.gz", TestUtils.TEST_DIR_REL_PATH, tarFileBaseName));
            additionalArgs.add("--output.include.squashedimage=true");
        }
        IntegrationTestCommon.testTar(random, programVersion, targetTarInSharedDir.getAbsolutePath(), testConfig.getTargetRepo(), testConfig.getTargetTag(), testConfig.isRequireBdioMatch(), Mode.NO_SERVICE_START, null, additionalArgs, outputContainerFileSystemFile, outputSquashedImageFile, null, testConfig.getCodelocationName());
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

    private class TestConfigBuilder {
        private String inspectTargetImageRepoTag;
        private String tarFilePath;
        private String targetRepo; // tarfile image selector
        private String targetTag;  // tarfile image selector
        private int portOnHost;
        private boolean requireBdioMatch;
        private int minNumberOfComponentsExpected;
        private String outputBomMustContainComponentPrefix;
        private String outputBomMustContainExternalSystemTypeId;
        private String codelocationName;
        private List<String> additionalArgs;
        private boolean testSquashedImageGeneration;

        public TestConfigBuilder setInspectTargetImageRepoTag(final String inspectTargetImageRepoTag) {
            this.inspectTargetImageRepoTag = inspectTargetImageRepoTag;
            return this;
        }

        public TestConfigBuilder setTarFilePath(final String tarFilePath) {
            this.tarFilePath = tarFilePath;
            return this;
        }

        public TestConfigBuilder setTargetRepo(final String targetRepo) {
            this.targetRepo = targetRepo;
            return this;
        }

        public TestConfigBuilder setTargetTag(final String targetTag) {
            this.targetTag = targetTag;
            return this;
        }

        public TestConfigBuilder setPortOnHost(final int portOnHost) {
            this.portOnHost = portOnHost;
            return this;
        }

        public TestConfigBuilder setRequireBdioMatch(final boolean requireBdioMatch) {
            this.requireBdioMatch = requireBdioMatch;
            return this;
        }

        public TestConfigBuilder setMinNumberOfComponentsExpected(final int minNumberOfComponentsExpected) {
            this.minNumberOfComponentsExpected = minNumberOfComponentsExpected;
            return this;
        }

        public TestConfigBuilder setOutputBomMustContainComponentPrefix(final String outputBomMustContainComponentPrefix) {
            this.outputBomMustContainComponentPrefix = outputBomMustContainComponentPrefix;
            return this;
        }

        public TestConfigBuilder setOutputBomMustContainExternalSystemTypeId(final String outputBomMustContainExternalSystemTypeId) {
            this.outputBomMustContainExternalSystemTypeId = outputBomMustContainExternalSystemTypeId;
            return this;
        }

        public TestConfigBuilder setCodelocationName(final String codelocationName) {
            this.codelocationName = codelocationName;
            return this;
        }

        public TestConfigBuilder setAdditionalArgs(final List<String> additionalArgs) {
            this.additionalArgs = additionalArgs;
            return this;
        }

        public TestConfigBuilder setTestSquashedImageGeneration(final boolean testSquashedImageGeneration) {
            this.testSquashedImageGeneration = testSquashedImageGeneration;
            return this;
        }

        public TestConfig build() throws IntegrationException {
            if ((inspectTargetImageRepoTag == null) && (tarFilePath == null)) {
                throw new IntegrationException("Invalid TestConfig");
            }
            return new TestConfig(inspectTargetImageRepoTag, tarFilePath, targetRepo, targetTag, portOnHost, requireBdioMatch, minNumberOfComponentsExpected,
            outputBomMustContainComponentPrefix,
            outputBomMustContainExternalSystemTypeId, codelocationName, additionalArgs, testSquashedImageGeneration);
        }
    }

    private class TestConfig {
        private final String inspectTargetImageRepoTag;
        private final String tarFilePath;
        private final String targetRepo; // tarfile image selector
        private final String targetTag;  // tarfile image selector
        private final int portOnHost;
        private final boolean requireBdioMatch;
        private final int minNumberOfComponentsExpected;
        private final String outputBomMustContainComponentPrefix;
        private final String outputBomMustContainExternalSystemTypeId;
        private final String codelocationName;
        private final List<String> additionalArgs;
        private final boolean testSquashedImageGeneration;

        public TestConfig(final String inspectTargetImageRepoTag, final String tarFilePath, final String targetRepo, final String targetTag, final int portOnHost, final boolean requireBdioMatch, final int minNumberOfComponentsExpected,
            final String outputBomMustContainComponentPrefix,
            final String outputBomMustContainExternalSystemTypeId, final String codelocationName, final List<String> additionalArgs, final boolean testSquashedImageGeneration) {
            this.inspectTargetImageRepoTag = inspectTargetImageRepoTag;
            this.tarFilePath = tarFilePath;
            this.targetRepo = targetRepo;
            this.targetTag = targetTag;
            this.portOnHost = portOnHost;
            this.requireBdioMatch = requireBdioMatch;
            this.minNumberOfComponentsExpected = minNumberOfComponentsExpected;
            this.outputBomMustContainComponentPrefix = outputBomMustContainComponentPrefix;
            this.outputBomMustContainExternalSystemTypeId = outputBomMustContainExternalSystemTypeId;
            this.codelocationName = codelocationName;
            this.additionalArgs = additionalArgs;
            this.testSquashedImageGeneration = testSquashedImageGeneration;
        }

        public String getInspectTargetImageRepoTag() {
            return inspectTargetImageRepoTag;
        }

        public String getTarFilePath() {
            return tarFilePath;
        }

        public String getTargetRepo() {
            return targetRepo;
        }

        public String getTargetTag() {
            return targetTag;
        }

        public int getPortOnHost() {
            return portOnHost;
        }

        public boolean isRequireBdioMatch() {
            return requireBdioMatch;
        }

        public int getMinNumberOfComponentsExpected() {
            return minNumberOfComponentsExpected;
        }

        public String getOutputBomMustContainComponentPrefix() {
            return outputBomMustContainComponentPrefix;
        }

        public String getOutputBomMustContainExternalSystemTypeId() {
            return outputBomMustContainExternalSystemTypeId;
        }

        public String getCodelocationName() {
            return codelocationName;
        }

        public List<String> getAdditionalArgs() {
            return additionalArgs;
        }

        public boolean isTestSquashedImageGeneration() {
            return testSquashedImageGeneration;
        }
    }
}
