package com.synopsys.integration.blackduck.dockerinspector;

import static org.junit.Assert.assertTrue;

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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;
import com.synopsys.integration.exception.IntegrationException;

@Tag("integration")
public class DockerInspectorTest {
    private static final int IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE = 8080;
    private static final int IMAGE_INSPECTOR_PORT_IN_CONTAINER_ALPINE = 8080;
    private static final int IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS = 8081;
    private static final int IMAGE_INSPECTOR_PORT_IN_CONTAINER_CENTOS = 8081;
    private static final int IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU = 8082;
    private static final int IMAGE_INSPECTOR_PORT_IN_CONTAINER_UBUNTU = 8082;

    private static final String SHARED_DIR_PATH_IN_CONTAINER = "/opt/blackduck/shared";

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

        File testDir = new File(TestUtils.TEST_DIR_REL_PATH);
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
        int len = timestamp.length();
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

        TestConfig testConfig = new TestConfigBuilder()
                                          .setTarFilePath("build/images/test/ubuntu1404.tar")
                                          .setPortOnHost(portOnHost)
                                          .setAdditionalArgs(additionalArgs)
                                          .setRequireBdioMatch(true)
                                          .setCodelocationName("testUbuntu1404LayeredIncludeRemoved")
                                          .setTestSquashedImageGeneration(true)
                                          .build();

        testTarUsingExistingContainer(testConfig);
    }

    @Test
    public void testUbuntuStartContainer() throws IOException, InterruptedException, IntegrationException {
        TestConfig testConfig = (new TestConfigBuilder())
                                          .setInspectTargetImageRepoTag("ubuntu:17.04")
                                          .setTargetRepo("ubuntu")
                                          .setTargetTag("17.04")
                                          .setRequireBdioMatch(false)
                                          .setMode(TestConfig.Mode.DEFAULT)
                                          .setOutputBomMustContainComponentPrefix("dpkg")
                                          .setMinNumberOfComponentsExpected(10)
                                          .setCodelocationName("ubuntu_17.04_DPKG")
                                          .build();
        IntegrationTestCommon.testImage(random, programVersion, null, testConfig);
    }

    @Test
    public void testAlpineStartContainer() throws IOException, InterruptedException, IntegrationException {
        TestConfig testConfig = (new TestConfigBuilder())
                                          .setInspectTargetImageRepoTag("alpine:3.6")
                                          .setTargetRepo("alpine")
                                          .setTargetTag("3.6")
                                          .setRequireBdioMatch(false)
                                          .setMode(TestConfig.Mode.SPECIFY_II_DETAILS)
                                          .setOutputBomMustContainComponentPrefix("apk-")
                                          .setMinNumberOfComponentsExpected(5)
                                          .setCodelocationName("alpine_3.6_APK")
                                          .build();
        IntegrationTestCommon.testImage(random, programVersion, null, testConfig);
    }

    @Test
    public void testCentosStartContainer() throws IOException, InterruptedException, IntegrationException {
        TestConfig testConfig = (new TestConfigBuilder())
                                          .setInspectTargetImageRepoTag("centos:7.3.1611")
                                          .setTargetRepo("centos")
                                          .setTargetTag("7.3.1611")
                                          .setRequireBdioMatch(false)
                                          .setMode(TestConfig.Mode.SPECIFY_II_DETAILS)
                                          .setOutputBomMustContainComponentPrefix("openssl-libs")
                                          .setMinNumberOfComponentsExpected(15)
                                          .setCodelocationName("centos_7.3.1611_RPM")
                                          .build();
        IntegrationTestCommon.testImage(random, programVersion, null, testConfig);
    }

    @Test
    public void testBusybox() throws IOException, InterruptedException, IntegrationException {
        TestConfig testConfig = (new TestConfigBuilder())
                                          .setInspectTargetImageRepoTag("busybox:latest")
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE)
                                          .setRequireBdioMatch(true)
                                          .setMinNumberOfComponentsExpected(0)
                                          .setCodelocationName("busybox_latest_noPkgMgr")
                                          .build();

        testImageUsingExistingContainer(testConfig);
    }

    @Test
    public void testAlpineLatest() throws IOException, InterruptedException, IntegrationException {
        TestConfig testConfig = (new TestConfigBuilder())
                                          .setInspectTargetImageRepoTag("alpine")
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE)
                                          .setRequireBdioMatch(false)
                                          .setMinNumberOfComponentsExpected(5)
                                          .setOutputBomMustContainComponentPrefix("apk-")
                                          .setCodelocationName("alpine_latest_APK")
                                          .build();

        testImageUsingExistingContainer(testConfig);
    }

    @Test
    public void testBlackDuckWebapp() throws IOException, InterruptedException, IntegrationException {
        TestConfig testConfig = (new TestConfigBuilder())
                                          .setInspectTargetImageRepoTag("blackducksoftware/hub-webapp:4.0.0")
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE)
                                          .setRequireBdioMatch(true)
                                          .setMinNumberOfComponentsExpected(0)
                                          .setOutputBomMustContainComponentPrefix(null)
                                          .setCodelocationName("blackducksoftware_hub-webapp_4.0.0_APK")
                                          .build();

        testImageUsingExistingContainer(testConfig);
    }

    @Test
    public void testBlackDuckZookeeper() throws IOException, InterruptedException, IntegrationException {
        TestConfig testConfig = (new TestConfigBuilder())
                                          .setInspectTargetImageRepoTag("blackducksoftware/hub-zookeeper:4.0.0")
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE)
                                          .setRequireBdioMatch(true)
                                          .setMinNumberOfComponentsExpected(0)
                                          .setOutputBomMustContainComponentPrefix(null)
                                          .setCodelocationName("blackducksoftware_hub-zookeeper_4.0.0_APK")
                                          .build();

        testImageUsingExistingContainer(testConfig);
    }

    @Test
    public void testTomcat() throws IOException, InterruptedException, IntegrationException {
        TestConfig testConfig = (new TestConfigBuilder())
                                          .setInspectTargetImageRepoTag("tomcat:6.0.53-jre7")
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU)
                                          .setRequireBdioMatch(false)
                                          .setMinNumberOfComponentsExpected(5)
                                          .setOutputBomMustContainComponentPrefix("dpkg")
                                          .setCodelocationName("tomcat_6.0.53-jre7_DPKG")
                                          .build();

        testImageUsingExistingContainer(testConfig);
    }

    // TODO: This feature requires the image is already local; this test doesn't ensure that (yet)
    @Disabled
    @Test
    public void testImageById() throws IOException, InterruptedException, IntegrationException {
        TestConfig testConfig = (new TestConfigBuilder())
                                          .setInspectTargetImageId("775349758637")
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU)
                                          .build();

        testImageUsingExistingContainer(testConfig);
    }

    @Test
    public void testRhel() throws IOException, InterruptedException, IntegrationException {
        TestConfig testConfig = (new TestConfigBuilder())
                                          .setInspectTargetImageRepoTag("dnplus/rhel:6.5")
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS)
                                          .setRequireBdioMatch(false)
                                          .setMinNumberOfComponentsExpected(10)
                                          .setOutputBomMustContainComponentPrefix("rpm")
                                          .setCodelocationName("dnplus_rhel_6.5_RPM")
                                          .build();

        testImageUsingExistingContainer(testConfig);
    }

    @Test
    public void testFedora() throws IOException, InterruptedException, IntegrationException {
        TestConfig testConfig = (new TestConfigBuilder())
                                          // was fedora:latest; see IDETECT-2293
                                          .setInspectTargetImageRepoTag("fedora:32")
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS)
                                          .setRequireBdioMatch(false)
                                          .setMinNumberOfComponentsExpected(10)
                                          .setOutputBomMustContainComponentPrefix("fedora-")
                                          .setOutputBomMustContainExternalSystemTypeId("@fedora")
                                          .setCodelocationName("fedora_latest_RPM")
                                          .build();

        testImageUsingExistingContainer(testConfig);
    }

    @Test
    public void testOpenSuseForge() throws IOException, InterruptedException, IntegrationException {
        TestConfig testConfig = (new TestConfigBuilder())
                                          .setInspectTargetImageRepoTag("opensuse/portus:2.4")
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS)
                                          .setRequireBdioMatch(false)
                                          .setMinNumberOfComponentsExpected(10)
                                          .setOutputBomMustContainExternalSystemTypeId("@opensuse")
                                          .setCodelocationName("opensuse_portus_opensuse_2.4_RPM")
                                          .build();

        testImageUsingExistingContainer(testConfig);
    }

    @Test
    public void testNonLinux() throws IOException, InterruptedException, IntegrationException {
        TestConfig testConfig = new TestConfigBuilder()
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
        TestConfig testConfig = new TestConfigBuilder()
                                          .setTarFilePath("build/images/test/whiteouttest.tar")
                                          .setTargetRepo("blackducksoftware/whiteouttest")
                                          .setTargetTag("1.0")
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS)
                                          .setRequireBdioMatch(false)
                                          .setMinNumberOfComponentsExpected(10)
                                          .setOutputBomMustContainComponentPrefix("libc-bin")
                                          .setOutputBomMustNotContainComponentPrefix("curl")
                                          .setCodelocationName("blackducksoftware_whiteouttest_1.0_DPKG")
                                          .build();

        testTarUsingExistingContainer(testConfig);
    }

    @Test
    public void testAggregateTarfileImageOne() throws IOException, InterruptedException, IntegrationException {
        TestConfig testConfig = new TestConfigBuilder()
                                          .setTarFilePath("build/images/test/aggregated.tar")
                                          .setTargetRepo("blackducksoftware/whiteouttest")
                                          .setTargetTag("1.0")
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS)
                                          .setRequireBdioMatch(false)
                                          .setMinNumberOfComponentsExpected(10)
                                          .setOutputBomMustContainComponentPrefix("libc-bin")
                                          .setOutputBomMustNotContainComponentPrefix("curl")
                                          .setCodelocationName("blackducksoftware_whiteouttest_1.0_DPKG")
                                          .build();

        testTarUsingExistingContainer(testConfig);
    }

    @Test
    public void testAggregateTarfileImageTwo() throws IOException, InterruptedException, IntegrationException {
        TestConfig testConfig = new TestConfigBuilder()
                                          .setTarFilePath("build/images/test/aggregated.tar")
                                          .setTargetRepo("blackducksoftware/centos_minus_vim_plus_bacula")
                                          .setTargetTag("1.0")
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS)
                                          .setRequireBdioMatch(false)
                                          .setMinNumberOfComponentsExpected(10)
                                          .setOutputBomMustContainComponentPrefix("openssl-libs")
                                          .setOutputBomMustNotContainComponentPrefix("vim-minimal")
                                          .setCodelocationName("blackducksoftware_centos_minus_vim_plus_bacula_1.0_RPM")
                                          .build();

        testTarUsingExistingContainer(testConfig);
    }

    @Test
    public void testAlpineLatestTarRepoTagSpecified() throws IOException, InterruptedException, IntegrationException {
        TestConfig testConfig = new TestConfigBuilder()
                                          .setTarFilePath("build/images/test/alpine.tar")
                                          .setTargetRepo("alpine")
                                          .setTargetTag("latest")
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE)
                                          .setRequireBdioMatch(false)
                                          .setMinNumberOfComponentsExpected(5)
                                          .setCodelocationName("alpine_latest_APK")
                                          .build();

        testTarUsingExistingContainer(testConfig);
    }

    @Test
    public void testAlpineLatestTarRepoTagNotSpecified() throws IOException, InterruptedException, IntegrationException {
        TestConfig testConfig = new TestConfigBuilder()
                                          .setTarFilePath("build/images/test/alpine.tar")
                                          .setTargetRepo(null)
                                          .setTargetTag(null)
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE)
                                          .setRequireBdioMatch(false)
                                          .setMinNumberOfComponentsExpected(5)
                                          .setCodelocationName("alpine_latest_APK")
                                          .build();

        testTarUsingExistingContainer(testConfig);
    }

    @Test
    public void testAlpineUsingExistingAlpineContainer() throws IOException, InterruptedException, IntegrationException {
        TestConfig testConfig = new TestConfigBuilder()
                                          .setTarFilePath("build/images/test/alpine36.tar")
                                          .setTargetRepo(null)
                                          .setTargetTag(null)
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE)
                                          .setRequireBdioMatch(false)
                                          .setMinNumberOfComponentsExpected(5)
                                          .setOutputBomMustContainComponentPrefix("busybox")
                                          .setCodelocationName("null_null_APK")
                                          .build();

        testTarUsingExistingContainer(testConfig);
    }

    @Test
    public void testWhiteoutUsingExistingAlpineContainer() throws IOException, InterruptedException, IntegrationException {
        TestConfig testConfig = new TestConfigBuilder()
                                          .setTarFilePath("build/images/test//whiteouttest.tar")
                                          .setTargetRepo("blackducksoftware/whiteouttest")
                                          .setTargetTag("1.0")
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE)
                                          .setRequireBdioMatch(false)
                                          .setMinNumberOfComponentsExpected(5)
                                          .setOutputBomMustContainComponentPrefix("dpkg")
                                          .setCodelocationName("blackducksoftware_whiteouttest_1.0_DPKG")
                                          .build();

        testTarUsingExistingContainer(testConfig);
    }

    @Test
    public void testCentosUsingExistingAlpineContainer() throws IOException, InterruptedException, IntegrationException {
        TestConfig testConfig = new TestConfigBuilder()
                                          .setTarFilePath("build/images/test/centos_minus_vim_plus_bacula.tar")
                                          .setTargetRepo("blackducksoftware/centos_minus_vim_plus_bacula")
                                          .setTargetTag("1.0")
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE)
                                          .setRequireBdioMatch(false)
                                          .setMinNumberOfComponentsExpected(5)
                                          .setOutputBomMustContainComponentPrefix("openssl-libs")
                                          .setOutputBomMustNotContainComponentPrefix("vim-minimal")
                                          .setCodelocationName("blackducksoftware_centos_minus_vim_plus_bacula_1.0_RPM")
                                          .build();

        testTarUsingExistingContainer(testConfig);
    }

    @Test
    public void testLinuxDistroOverride() throws IOException, InterruptedException, IntegrationException {
        List<String> additionalArgs = new ArrayList<>();
        additionalArgs.add("--linux.distro=testdistro");
        TestConfig testConfig = new TestConfigBuilder()
                                          .setTarFilePath("build/images/test/alpine.tar")
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU)
                                          .setRequireBdioMatch(false)
                                          .setOutputBomMustContainExternalSystemTypeId("@testdistro")
                                          .setAdditionalArgs(additionalArgs)
                                          .setCodelocationName("alpine_latest_APK")
                                          .build();

        testTarUsingExistingContainer(testConfig);
    }

    @Test
    public void testUbuntuUsingExistingCentosContainer() throws IOException, InterruptedException, IntegrationException {
        TestConfig testConfig = new TestConfigBuilder()
                                          .setTarFilePath("build/images/test/ubuntu1404.tar")
                                          .setTargetRepo(null)
                                          .setTargetTag(null)
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS)
                                          .setRequireBdioMatch(false)
                                          .setOutputBomMustContainComponentPrefix("iputils-ping")
                                          .setMinNumberOfComponentsExpected(10)
                                          .setCodelocationName("null_null_DPKG")
                                          .build();

        testTarUsingExistingContainer(testConfig);
    }

    @Test
    public void testExcludePlatformComponents() throws IOException, InterruptedException, IntegrationException {
        List<String> additionalArgs = new ArrayList<>();
        additionalArgs.add("--docker.platform.top.layer.id=sha256:1bcfbfaf95f95ea8a28711c83085dbbeceefa11576e1c889304aa5bacbaa6ac2");

        TestConfig testConfig = new TestConfigBuilder()
                                          .setTarFilePath("build/images/test/aggregated.tar")
                                          .setTargetRepo("blackducksoftware/centos_minus_vim_plus_bacula")
                                          .setTargetTag("1.0")
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU)
                                          .setRequireBdioMatch(false)
                                          .setMinNumberOfComponentsExpected(10)
                                          .setOutputBomMustContainComponentPrefix("systemd")
                                          .setOutputBomMustNotContainComponentPrefix("vim-minimal")
                                          .setCodelocationName("blackducksoftware_centos_minus_vim_plus_bacula_1.0_app_RPM")
                                          .setAdditionalArgs(additionalArgs)
                                          .setAppOnlyMode(true)
                                          .build();

        testTarUsingExistingContainer(testConfig);
    }

    @Test
    public void testExcludeDirectoriesRedirect() throws IOException, InterruptedException, IntegrationException {
        List<String> additionalArgs = new ArrayList<>();
        additionalArgs.add("--output.containerfilesystem.excluded.paths=/bin,/dev,/home,/lib,/media,/mnt,/opt,/proc,/root,/run,/sbin,/srv,/sys,/tmp,/usr,/var");

        TestConfig testConfig = new TestConfigBuilder()
                                          .setTarFilePath("build/images/test/alpine.tar")
                                          .setTargetRepo(null)
                                          .setTargetTag(null)
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU)
                                          .setRequireBdioMatch(false)
                                          .setMinNumberOfComponentsExpected(5)
                                          .setCodelocationName("alpine_latest_APK")
                                          .setAdditionalArgs(additionalArgs)
                                          .setMinContainerFileSystemFileSize(100000)
                                          .setMaxContainerFileSystemFileSize(200000)
                                          .build();

        testTarUsingExistingContainer(testConfig);
    }

    @Test
    public void testExcludeDirectoriesNoRedirect() throws IOException, InterruptedException, IntegrationException {
        List<String> additionalArgs = new ArrayList<>();
        additionalArgs.add("--output.containerfilesystem.excluded.paths=/bin,/dev,/home,/lib,/media,/mnt,/opt,/proc,/root,/run,/sbin,/srv,/sys,/tmp,/usr,/var");

        TestConfig testConfig = new TestConfigBuilder()
                                          .setTarFilePath("build/images/test/alpine.tar")
                                          .setTargetRepo(null)
                                          .setTargetTag(null)
                                          .setPortOnHost(IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE)
                                          .setRequireBdioMatch(false)
                                          .setMinNumberOfComponentsExpected(5)
                                          .setCodelocationName("alpine_latest_APK")
                                          .setAdditionalArgs(additionalArgs)
                                          .setMinContainerFileSystemFileSize(100000)
                                          .setMaxContainerFileSystemFileSize(200000)
                                          .build();

        testTarUsingExistingContainer(testConfig);
    }

    @Test
    public void testExcludeDirectoriesStartContainer() throws IOException, InterruptedException, IntegrationException {
        List<String> additionalArgs = new ArrayList<>();
        additionalArgs.add("--output.containerfilesystem.excluded.paths=/bin,/dev,/home,/lib,/media,/mnt,/opt,/proc,/root,/run,/sbin,/srv,/sys,/tmp,/usr,/var");
        TestConfig testConfig = (new TestConfigBuilder())
                                          .setInspectTargetImageRepoTag("alpine:latest")
                                          .setTargetRepo(null)
                                          .setTargetTag(null)
                                          .setRequireBdioMatch(false)
                                          .setMinNumberOfComponentsExpected(5)
                                          .setCodelocationName("alpine_latest_APK")
                                          .setAdditionalArgs(additionalArgs)
                                          .setMinContainerFileSystemFileSize(100000)
                                          .setMaxContainerFileSystemFileSize(200000)
                                          .build();
        IntegrationTestCommon.testImage(random, programVersion, null, testConfig);
    }

    private void testTarUsingExistingContainer(TestConfig testConfig)
        throws IOException, InterruptedException, IntegrationException {
        File targetTar = new File(testConfig.getTarFilePath());
        String tarFileName = targetTar.getName();
        String tarFileBaseName = tarFileName.substring(0, tarFileName.length() - ".tar".length());
        File targetTarInSharedDir = new File(containerTargetDir, tarFileName);
        FileUtils.copyFile(targetTar, targetTarInSharedDir);
        targetTarInSharedDir.setReadable(true, false);
        testConfig.setTargetTarInSharedDir(targetTarInSharedDir);

        List<String> additionalArgs = testConfig.getAdditionalArgs();
        if (additionalArgs == null) {
            additionalArgs = new ArrayList<>();
            testConfig.setAdditionalArgs(additionalArgs);
        }
        additionalArgs.add(String.format("--imageinspector.service.url=http://localhost:%d", testConfig.getPortOnHost()));
        additionalArgs.add(String.format("--shared.dir.path.local=%s", dirSharedWithContainer.getAbsolutePath()));
        additionalArgs.add(String.format("--shared.dir.path.imageinspector=%s", SHARED_DIR_PATH_IN_CONTAINER));
        File outputContainerFileSystemFile;
        if (testConfig.isAppOnlyMode()) {
            outputContainerFileSystemFile = new File(String.format("%s/output/%s_app_containerfilesystem.tar.gz", TestUtils.TEST_DIR_REL_PATH, tarFileBaseName));
        } else {
            outputContainerFileSystemFile = new File(String.format("%s/output/%s_containerfilesystem.tar.gz", TestUtils.TEST_DIR_REL_PATH, tarFileBaseName));
        }
        testConfig.setOutputContainerFileSystemFile(outputContainerFileSystemFile);
        File outputSquashedImageFile = null;
        if (testConfig.isTestSquashedImageGeneration()) {
            outputSquashedImageFile = new File(String.format("%s/output/%s_squashedimage.tar.gz", TestUtils.TEST_DIR_REL_PATH, tarFileBaseName));
            additionalArgs.add("--output.include.squashedimage=true");
            testConfig.setOutputSquashedImageFile(outputSquashedImageFile);
        }
        testConfig.setMode(TestConfig.Mode.NO_SERVICE_START);

        IntegrationTestCommon.testTar(random, programVersion, null, testConfig);
    }

    private void testImageUsingExistingContainer(TestConfig testConfig)
        throws IOException, InterruptedException, IntegrationException {

        List<String> additionalArgs = new ArrayList<>();
        additionalArgs.add(String.format("--imageinspector.service.url=http://localhost:%d", testConfig.getPortOnHost()));
        additionalArgs.add(String.format("--shared.dir.path.local=%s", dirSharedWithContainer.getAbsolutePath()));
        additionalArgs.add(String.format("--shared.dir.path.imageinspector=%s", SHARED_DIR_PATH_IN_CONTAINER));

        testConfig.setMode(TestConfig.Mode.NO_SERVICE_START);
        testConfig.setAdditionalArgs(additionalArgs);
        IntegrationTestCommon.testImage(random, programVersion, null, testConfig);
    }

    private static void createWriteableDirTolerantly(File dir) {
        System.out.printf("Creating and setting a+wx permission on: %s\n", dir.getAbsolutePath());
        createDirTolerantly(dir);
        setWriteExecutePermissionsTolerantly(dir);
        logPermissions(dir);
    }

    private static void logPermissions(File dir) {
        Set<PosixFilePermission> perms;
        try {
            perms = Files.getPosixFilePermissions(dir.toPath());
            System.out.printf("* Dir %s now has perms: %s\n", dir.getAbsolutePath(), perms.toString());
        } catch (IOException e) {
            System.out.printf("Unable to read back perms for dir %s: %s\n", dir.getAbsolutePath(), e.getMessage());
        }
    }

    private static void createDirTolerantly(File dir) {
        try {
            dir.mkdirs();
        } catch (Exception e) {
            System.out.printf("Error creating directory %s: %s\n", dir.getAbsoluteFile(), e.getMessage());
        }
        if (!dir.exists()) {
            System.out.printf("ERROR: Attempted to create directory %s, but it still does not exist\n", dir.getAbsoluteFile());
        }
    }

    private static void setWriteExecutePermissionsTolerantly(File file) {
        try {
            file.setWritable(true, false);
        } catch (Exception e) {
            System.out.printf("Error making directory %s writeable: %s\n", file.getAbsolutePath(), e.getMessage());
        }
        try {
            file.setExecutable(true, false);
        } catch (Exception e) {
            System.out.printf("Error making directory %s writeable: %s\n", file.getAbsolutePath(), e.getMessage());
        }
    }

    private static boolean isUp(int port) {
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

    private static void startContainer(String imageInspectorPlatform, int portOnHost, int portInContainer) throws IOException, InterruptedException, IntegrationException {
        String containerName = getContainerName(imageInspectorPlatform);
        String cmd = String.format("docker run -d -t --name %s -p %d:%d -v \"$(pwd)\"/%s/containerShared:%s blackducksoftware/%s-%s:%s",
            containerName, portOnHost,
            portInContainer,
            TestUtils.TEST_DIR_REL_PATH,
            SHARED_DIR_PATH_IN_CONTAINER,
            programVersion.getInspectorImageFamily(), imageInspectorPlatform, programVersion.getInspectorImageVersion());
        TestUtils.execCmd(cmd, 120000L, true, null);
    }

    private static String getContainerName(String imageInspectorPlatform) {
        return String.format("dockerInspectorTestImageInspector_%s_%s", imageInspectorPlatform, dateTimeStamp);
    }

    private static void printDockerVersion() {
        try {
            TestUtils.execCmd("docker version", 20000L, true, null);
        } catch (Exception e) {
            System.out.printf("Error running docker version command: %s\n", e.getMessage());
        }
    }

    private static void stopContainer(String imageInspectorPlatform) {
        String containerName = getContainerName(imageInspectorPlatform);
        try {
            TestUtils.execCmd(String.format("docker stop %s", containerName), 120000L, true, null);
        } catch (Exception e) {
            System.out.printf("Error stopping container %s: %s\n", containerName, e.getMessage());
        }
    }

    private static void removeContainer(String imageInspectorPlatform) {
        String containerName = getContainerName(imageInspectorPlatform);
        try {
            TestUtils.execCmd(String.format("docker rm -f %s", containerName), 120000L, true, null);
        } catch (Exception e) {
            System.out.printf("Error removing container %s: %s\n", containerName, e.getMessage());
        }
    }

    private static void ensureContainerRemoved(String imageInspectorPlatform) {
        String containerName = getContainerName(imageInspectorPlatform);
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
            } catch (Exception e) {
                System.out.printf("Error stopping container %s: %s\n", containerName, e.getMessage());
            }
        }
        if (containerStillExists) {
            System.out.printf("ERROR: Failed to remove container %s\n", containerName);
        }
    }

    private static String getAllContainers(boolean logStdout) throws IOException, InterruptedException, IntegrationException {
        return TestUtils.execCmd("docker ps -a", 120000L, logStdout, null);
    }

    private static String getRunningContainers(boolean logStdout) throws IOException, InterruptedException, IntegrationException {
        return TestUtils.execCmd("docker ps", 120000L, logStdout, null);
    }

    private static void removeDockerInspectorContainers() throws IOException, InterruptedException, IntegrationException {
        System.out.println("Stopping/Removing docker inspector containers");
        String psAllOutput = TestUtils.execCmd("docker ps -a", 120000L, false, null);
        String[] lines = psAllOutput.split("\n");
        for (String line : lines) {
            System.out.printf("Line: %s\n", line);
            if (line.startsWith("CONTAINER")) {
                continue;
            }
            String[] fields = line.split("\\s+");
            String containerName = fields[fields.length - 1];
            System.out.printf("Container name: %s\n", containerName);
            if (containerName.startsWith("blackduck-imageinspector-alpine_") || containerName.startsWith("blackduck-imageinspector-centos_") || containerName.startsWith("blackduck-imageinspector-ubuntu_")) {
                TestUtils.execCmd(String.format("docker stop %s", containerName), 120000L, false, null);
                Thread.sleep(10000L);
                TestUtils.execCmd(String.format("docker rm -f %s", containerName), 120000L, false, null);
            }
        }
    }
}
