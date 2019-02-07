package com.synopsys.integration.blackduck.dockerinspector;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.synopsys.integration.blackduck.dockerinspector.IntegrationTestCommon.Mode;
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
        IntegrationTestCommon.testImage(random, programVersion, "ubuntu:17.04", "ubuntu", "17.04",  false, Mode.DEFAULT, null, "dpkg", 10, additionalArgsWithServiceUrl, minikubeDockerEnv, "ubuntu_17.04_DPKG");
    }

    @Test
    public void testAlpineStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(random, programVersion, "alpine:3.6", "alpine", "3.6", false, Mode.DEFAULT, null, "apk-", 5, additionalArgsWithServiceUrl, minikubeDockerEnv, "alpine_3.6_APK");
    }

    @Test
    public void testBusyboxStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(random, programVersion, "busybox:latest", "busybox", "latest",  false, Mode.DEFAULT, null, null, 0, additionalArgsWithServiceUrl, minikubeDockerEnv, "busybox_latest_noPkgMgr");
    }

    @Test
    public void testAlpineLatestStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(random, programVersion, "alpine", "alpine", "latest", false, Mode.DEFAULT, null, "apk-", 5, additionalArgsWithServiceUrl, minikubeDockerEnv, "alpine_latest_APK");
    }

    @Test
    public void testCentosStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(random, programVersion, "centos:7.3.1611", "centos", "7.3.1611", false, Mode.DEFAULT, null, "rpm", 15, additionalArgsWithServiceUrl, minikubeDockerEnv, "centos_7.3.1611_RPM");
    }

    @Test
    public void testBlackDuckWebappStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(random, programVersion, "blackducksoftware/hub-webapp:4.0.0", "blackducksoftware_hub-webapp", "4.0.0", true, Mode.DEFAULT, null, "apk-", 5,
                additionalArgsWithServiceUrl, minikubeDockerEnv, "blackducksoftware_hub-webapp_4.0.0_APK");
    }

    @Test
    public void testBlackDuckZookeeperStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(random, programVersion, "blackducksoftware/hub-zookeeper:4.0.0", "blackducksoftware_hub-zookeeper", "4.0.0", true, Mode.DEFAULT, null, "apk-", 5,
                additionalArgsWithServiceUrl, minikubeDockerEnv, "blackducksoftware_hub-zookeeper_4.0.0_APK");
    }

    @Test
    public void testTomcatStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(random, programVersion, "tomcat:6.0.53-jre7", "tomcat", "6.0.53-jre7", false, Mode.DEFAULT, null, "dpkg", 5, additionalArgsWithServiceUrl, minikubeDockerEnv, "tomcat_6.0.53-jre7_DPKG");
    }

    @Test
    public void testRhelStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(random, programVersion, "dnplus/rhel:6.5", "dnplus_rhel", "6.5", false, Mode.DEFAULT, null, "rpm", 10, additionalArgsWithServiceUrl, minikubeDockerEnv, "dnplus_rhel_6.5_RPM");
    }

    @Test
    public void testWhiteoutStartContainer() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/whiteouttest";
        final String tag = "1.0";
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("whiteouttest.tar");
        IntegrationTestCommon.testTar(random, programVersion, "build/images/test/whiteouttest.tar", repo, tag, true, Mode.DEFAULT, null, additionalArgsWithServiceUrl,
                outputContainerFileSystemFile,
                minikubeDockerEnv, "blackducksoftware_whiteouttest_1.0_DPKG");
    }

    @Test
    public void testAggregateTarfileImageOneStartContainer() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/whiteouttest";
        final String tag = "1.0";
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("aggregated.tar");
        IntegrationTestCommon.testTar(random, programVersion, "build/images/test/aggregated.tar", repo, tag, true, Mode.SPECIFY_II_DETAILS, null, additionalArgsWithServiceUrl,
                outputContainerFileSystemFile,
                minikubeDockerEnv, "blackducksoftware_whiteouttest_1.0_DPKG");
    }

    @Test
    public void testAggregateTarfileImageTwoStartContainer() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/centos_minus_vim_plus_bacula";
        final String tag = "1.0";
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("aggregated.tar");
        IntegrationTestCommon.testTar(random, programVersion, "build/images/test/aggregated.tar", repo, tag, true, Mode.DEFAULT, null, additionalArgsWithServiceUrl,
                outputContainerFileSystemFile,
                minikubeDockerEnv, "blackducksoftware_centos_minus_vim_plus_bacula_1.0_RPM");
    }

    @Test
    public void testAlpineLatestTarRepoTagSpecifiedStartContainer() throws IOException, InterruptedException, IntegrationException {
        final String repo = "alpine";
        final String tag = "latest";
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("alpine.tar");
        IntegrationTestCommon.testTar(random, programVersion, "build/images/test/alpine.tar", repo, tag, false, Mode.SPECIFY_II_DETAILS, null, additionalArgsWithServiceUrl, outputContainerFileSystemFile,
                minikubeDockerEnv, "alpine_latest_APK");
    }

    @Test
    public void testAlpineLatestTarRepoTagNotSpecifiedStartContainer() throws IOException, InterruptedException, IntegrationException {
        final String repo = "alpine";
        final String tag = "latest";
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("alpine.tar");
        IntegrationTestCommon.testTar(random, programVersion, "build/images/test/alpine.tar", repo, tag, false, Mode.DEFAULT, null, additionalArgsWithServiceUrl, outputContainerFileSystemFile,
                minikubeDockerEnv, "alpine_latest_APK");
    }
}
