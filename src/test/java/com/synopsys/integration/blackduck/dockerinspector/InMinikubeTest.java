package com.synopsys.integration.blackduck.dockerinspector;

import static org.junit.Assert.assertTrue;

import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.synopsys.integration.exception.IntegrationException;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.util.Random;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Disabled
@Tag("minikube")
public class InMinikubeTest {
    private static KubernetesClient client;
    private static String clusterIp;
    private static List<String> additionalArgsWithServiceUrl;
    private static ProgramVersion programVersion;
    private static Map<String, String> minikubeDockerEnv = new HashMap<>();
    private static Random random;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        random = new Random();
        programVersion = new ProgramVersion();
        programVersion.init();

        final String kubeStatusOutputJoined = TestUtils.execCmd(null, "minikube status", 15, true, null);
        System.out.println(String.format("kubeStatusOutputJoined: %s", kubeStatusOutputJoined));
        assertTrue("Minikube is not running", kubeStatusOutputJoined.contains("minikube: Running"));
        assertTrue("Minikube is not running", kubeStatusOutputJoined.contains("cluster: Running"));

        final String[] ipOutput = TestUtils.execCmd("minikube ip", 10, true, null).split("\n");
        clusterIp = ipOutput[0];
        final String serviceUrlArg = String.format("--imageinspector.service.url=http://%s:%d", clusterIp, IntegrationTestCommon.START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU);
        additionalArgsWithServiceUrl = new ArrayList<>(2);
        additionalArgsWithServiceUrl.add(serviceUrlArg);
        additionalArgsWithServiceUrl.add("--shared.dir.path.local=test/containerShared");
        client = new DefaultKubernetesClient();
        try {
            System.out.printf("API version: %s\n", client.getApiVersion());
        } catch (final Exception e) {
            e.printStackTrace();
        }

        final String[] dockerEnvOutput = TestUtils.execCmd("minikube docker-env", 5, true, null).split("\n");

        for (final String line : dockerEnvOutput) {
            if (line.startsWith("export")) {
                final String envVariableName = line.substring("export".length() + 1, line.indexOf("="));
                final String envVariableValue = line.substring(line.indexOf("=") + 2, line.length() - 1);
                System.out.println(String.format("env var assignment: %s=%s", envVariableName, envVariableValue));
                minikubeDockerEnv.put(envVariableName, envVariableValue);
            }
        }

    }

    @AfterAll
    public static void tearDownAfterClass() {
        if (client != null) {
            client.close();
        }
        System.out.println("Test has completed");
    }

    @Test
    public void testUbuntuStartContainer() throws IOException, InterruptedException, IntegrationException {
        final TestConfig testConfig = (new TestConfigBuilder())
            .setInspectTargetImageRepoTag("ubuntu:17.04")
            .setTargetRepo("ubuntu")
            .setTargetTag("17.04")
            .setRequireBdioMatch(false)
            .setMode(TestConfig.Mode.DEFAULT)
            .setOutputBomMustContainComponentPrefix("dpkg")
            .setMinNumberOfComponentsExpected(10)
            .setAdditionalArgs(additionalArgsWithServiceUrl)
            .setEnv(minikubeDockerEnv)
            .setCodelocationName("ubuntu_17.04_DPKG")
                                          .build();
        IntegrationTestCommon.testImage(random, programVersion, null, testConfig);
    }

    @Test
    public void testAlpineStartContainer() throws IOException, InterruptedException, IntegrationException {
        final TestConfig testConfig = (new TestConfigBuilder())
                                          .setInspectTargetImageRepoTag("alpine:3.6")
                                          .setTargetRepo("alpine")
                                          .setTargetTag("3.6")
                                          .setRequireBdioMatch(false)
                                          .setMode(TestConfig.Mode.DEFAULT)
                                          .setOutputBomMustContainComponentPrefix("apk-")
                                          .setMinNumberOfComponentsExpected(5)
                                          .setAdditionalArgs(additionalArgsWithServiceUrl)
                                          .setEnv(minikubeDockerEnv)
                                          .setCodelocationName("alpine_3.6_APK")
                                          .build();
        IntegrationTestCommon.testImage(random, programVersion, null, testConfig);
    }

    @Test
    public void testBusyboxStartContainer() throws IOException, InterruptedException, IntegrationException {
        final TestConfig testConfig = (new TestConfigBuilder())
                                          .setInspectTargetImageRepoTag("busybox:latest")
                                          .setTargetRepo("busybox")
                                          .setTargetTag("latest")
                                          .setRequireBdioMatch(false)
                                          .setMode(TestConfig.Mode.DEFAULT)
                                          .setOutputBomMustContainComponentPrefix(null)
                                          .setMinNumberOfComponentsExpected(0)
                                          .setAdditionalArgs(additionalArgsWithServiceUrl)
                                          .setEnv(minikubeDockerEnv)
                                          .setCodelocationName("busybox_latest_noPkgMgr")
                                          .build();
        IntegrationTestCommon.testImage(random, programVersion, null, testConfig);
    }

    @Test
    public void testAlpineLatestStartContainer() throws IOException, InterruptedException, IntegrationException {
        final TestConfig testConfig = (new TestConfigBuilder())
                                          .setInspectTargetImageRepoTag("alpine")
                                          .setTargetRepo("alpine")
                                          .setTargetTag("latest")
                                          .setRequireBdioMatch(false)
                                          .setMode(TestConfig.Mode.DEFAULT)
                                          .setOutputBomMustContainComponentPrefix("apk-")
                                          .setMinNumberOfComponentsExpected(5)
                                          .setAdditionalArgs(additionalArgsWithServiceUrl)
                                          .setEnv(minikubeDockerEnv)
                                          .setCodelocationName("alpine_latest_APK")
                                          .build();
        IntegrationTestCommon.testImage(random, programVersion, null, testConfig);
    }

    @Test
    public void testCentosStartContainer() throws IOException, InterruptedException, IntegrationException {
        final TestConfig testConfig = (new TestConfigBuilder())
                                          .setInspectTargetImageRepoTag("centos:7.3.1611")
                                          .setTargetRepo("centos")
                                          .setTargetTag("7.3.1611")
                                          .setRequireBdioMatch(false)
                                          .setMode(TestConfig.Mode.DEFAULT)
                                          .setOutputBomMustContainComponentPrefix("rpm")
                                          .setMinNumberOfComponentsExpected(15)
                                          .setAdditionalArgs(additionalArgsWithServiceUrl)
                                          .setEnv(minikubeDockerEnv)
                                          .setCodelocationName("centos_7.3.1611_RPM")
                                          .build();
        IntegrationTestCommon.testImage(random, programVersion, null, testConfig);
    }

    @Test
    public void testBlackDuckWebappStartContainer() throws IOException, InterruptedException, IntegrationException {
        final TestConfig testConfig = (new TestConfigBuilder())
                                          .setInspectTargetImageRepoTag("blackducksoftware/hub-webapp:4.0.0")
                                          .setTargetRepo("blackducksoftware_hub-webapp")
                                          .setTargetTag("4.0.0")
                                          .setRequireBdioMatch(true)
                                          .setMode(TestConfig.Mode.DEFAULT)
                                          .setOutputBomMustContainComponentPrefix("apk-")
                                          .setMinNumberOfComponentsExpected(5)
                                          .setAdditionalArgs(additionalArgsWithServiceUrl)
                                          .setEnv(minikubeDockerEnv)
                                          .setCodelocationName("blackducksoftware_hub-webapp_4.0.0_APK")
                                          .build();
        IntegrationTestCommon.testImage(random, programVersion, null, testConfig);
    }

    @Test
    public void testBlackDuckZookeeperStartContainer() throws IOException, InterruptedException, IntegrationException {
        final TestConfig testConfig = (new TestConfigBuilder())
                                          .setInspectTargetImageRepoTag("blackducksoftware/hub-zookeeper:4.0.0")
                                          .setTargetRepo("blackducksoftware_hub-zookeeper")
                                          .setTargetTag("4.0.0")
                                          .setRequireBdioMatch(true)
                                          .setMode(TestConfig.Mode.DEFAULT)
                                          .setOutputBomMustContainComponentPrefix("apk-")
                                          .setMinNumberOfComponentsExpected(5)
                                          .setAdditionalArgs(additionalArgsWithServiceUrl)
                                          .setEnv(minikubeDockerEnv)
                                          .setCodelocationName("blackducksoftware_hub-zookeeper_4.0.0_APK")
                                          .build();
        IntegrationTestCommon.testImage(random, programVersion, null, testConfig);
    }

    @Test
    public void testTomcatStartContainer() throws IOException, InterruptedException, IntegrationException {
        final TestConfig testConfig = (new TestConfigBuilder())
                                          .setInspectTargetImageRepoTag("tomcat:6.0.53-jre7")
                                          .setTargetRepo("tomcat")
                                          .setTargetTag("6.0.53-jre7")
                                          .setRequireBdioMatch(false)
                                          .setMode(TestConfig.Mode.DEFAULT)
                                          .setOutputBomMustContainComponentPrefix("dpkg")
                                          .setMinNumberOfComponentsExpected(5)
                                          .setAdditionalArgs(additionalArgsWithServiceUrl)
                                          .setEnv(minikubeDockerEnv)
                                          .setCodelocationName("tomcat_6.0.53-jre7_DPKG")
                                          .build();
        IntegrationTestCommon.testImage(random, programVersion, null, testConfig);
    }

    @Test
    public void testRhelStartContainer() throws IOException, InterruptedException, IntegrationException {
        final TestConfig testConfig = (new TestConfigBuilder())
                                          .setInspectTargetImageRepoTag("dnplus/rhel:6.5")
                                          .setTargetRepo("dnplus_rhel")
                                          .setTargetTag("6.5")
                                          .setRequireBdioMatch(false)
                                          .setMode(TestConfig.Mode.DEFAULT)
                                          .setOutputBomMustContainComponentPrefix("rpm")
                                          .setMinNumberOfComponentsExpected(10)
                                          .setAdditionalArgs(additionalArgsWithServiceUrl)
                                          .setEnv(minikubeDockerEnv)
                                          .setCodelocationName("dnplus_rhel_6.5_RPM")
                                          .build();
        IntegrationTestCommon.testImage(random, programVersion, null, testConfig);
    }

    @Test
    public void testWhiteoutStartContainer() throws IOException, InterruptedException, IntegrationException {
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("whiteouttest.tar");
        final TestConfig testConfig = (new TestConfigBuilder())
                                          .setTarFilePath("build/images/test/whiteouttest.tar")
                                          .setTargetRepo("blackducksoftware/whiteouttest")
                                          .setTargetTag("1.0")
                                          .setRequireBdioMatch(true)
                                          .setMode(TestConfig.Mode.DEFAULT)
                                          .setAdditionalArgs(additionalArgsWithServiceUrl)
                                          .setEnv(minikubeDockerEnv)
                                          .setCodelocationName("blackducksoftware_whiteouttest_1.0_DPKG")
                                          .setOutputContainerFileSystemFile(outputContainerFileSystemFile)
                                          .build();

        IntegrationTestCommon.testTar(random, programVersion, null, testConfig);
    }

    @Test
    public void testAggregateTarfileImageOneStartContainer() throws IOException, InterruptedException, IntegrationException {
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("aggregated.tar");
        final TestConfig testConfig = (new TestConfigBuilder())
                                          .setTarFilePath("build/images/test/aggregated.tar")
                                          .setTargetRepo("blackducksoftware/whiteouttest")
                                          .setTargetTag("1.0")
                                          .setRequireBdioMatch(true)
                                          .setMode(TestConfig.Mode.SPECIFY_II_DETAILS)
                                          .setAdditionalArgs(additionalArgsWithServiceUrl)
                                          .setEnv(minikubeDockerEnv)
                                          .setCodelocationName("blackducksoftware_whiteouttest_1.0_DPKG")
                                          .setOutputContainerFileSystemFile(outputContainerFileSystemFile)
                                          .build();

        IntegrationTestCommon.testTar(random, programVersion, null, testConfig);
    }

    @Test
    public void testAggregateTarfileImageTwoStartContainer() throws IOException, InterruptedException, IntegrationException {
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("aggregated.tar");
        final TestConfig testConfig = (new TestConfigBuilder())
                                          .setTarFilePath("build/images/test/aggregated.tar")
                                          .setTargetRepo("blackducksoftware/centos_minus_vim_plus_bacula")
                                          .setTargetTag("1.0")
                                          .setRequireBdioMatch(true)
                                          .setMode(TestConfig.Mode.DEFAULT)
                                          .setAdditionalArgs(additionalArgsWithServiceUrl)
                                          .setEnv(minikubeDockerEnv)
                                          .setCodelocationName("blackducksoftware_centos_minus_vim_plus_bacula_1.0_RPM")
                                          .setOutputContainerFileSystemFile(outputContainerFileSystemFile)
                                          .build();

        IntegrationTestCommon.testTar(random, programVersion, null, testConfig);
    }

    @Test
    public void testAlpineLatestTarRepoTagSpecifiedStartContainer() throws IOException, InterruptedException, IntegrationException {
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("alpine.tar");
        final TestConfig testConfig = (new TestConfigBuilder())
                                          .setTarFilePath("build/images/test/alpine.tar")
                                          .setTargetRepo("alpine")
                                          .setTargetTag("latest")
                                          .setRequireBdioMatch(false)
                                          .setMode(TestConfig.Mode.SPECIFY_II_DETAILS)
                                          .setAdditionalArgs(additionalArgsWithServiceUrl)
                                          .setEnv(minikubeDockerEnv)
                                          .setCodelocationName("alpine_latest_APK")
                                          .setOutputContainerFileSystemFile(outputContainerFileSystemFile)
                                          .build();

        IntegrationTestCommon.testTar(random, programVersion, null, testConfig);
    }

    @Test
    public void testAlpineLatestTarRepoTagNotSpecifiedStartContainer() throws IOException, InterruptedException, IntegrationException {
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("alpine.tar");
        final TestConfig testConfig = (new TestConfigBuilder())
                                          .setTarFilePath("build/images/test/alpine.tar")
                                          .setTargetRepo("alpine")
                                          .setTargetTag("latest")
                                          .setRequireBdioMatch(false)
                                          .setMode(TestConfig.Mode.DEFAULT)
                                          .setAdditionalArgs(additionalArgsWithServiceUrl)
                                          .setEnv(minikubeDockerEnv)
                                          .setCodelocationName("alpine_latest_APK")
                                          .setOutputContainerFileSystemFile(outputContainerFileSystemFile)
                                          .build();

        IntegrationTestCommon.testTar(random, programVersion, null, testConfig);
    }
}
