package com.synopsys.integration.blackduck.dockerinspector.dockerclient;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.ProgramPaths;
import com.synopsys.integration.blackduck.dockerinspector.output.ImageTarFilename;
import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import com.synopsys.integration.exception.IntegrationException;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertTrue;

///@Tag("integration")
public class DockerClientIT {

    @Test
    public void test() throws InterruptedException, IntegrationException {
        final Config config = Mockito.mock(Config.class);
        Mockito.when(config.isOfflineMode()).thenReturn(false);
        // Platform independentize these!!!!!!!!:
        Mockito.when(config.getSharedDirPathImageInspector()).thenReturn("/tmp/blackduck-docker-inspector-files/shared to container:/opt/blackduck/blackduck-imageinspector/shared");
        Mockito.when(config.getSharedDirPathLocal()).thenReturn("/tmp/blackduck-docker-inspector-files/shared");

        final ImageTarFilename imageTarFilename = new ImageTarFilename();
        final ProgramPaths programPaths = new ProgramPaths();

        DockerClientManager dockerClientManager = new DockerClientManager(config, imageTarFilename, programPaths);
        System.out.printf("docker engine version: %s", dockerClientManager.getDockerEngineVersion());
        assertTrue(StringUtils.isNotBlank(dockerClientManager.getDockerEngineVersion()));

        String imageId = dockerClientManager.pullImage("alpine", "latest");
        System.out.printf("docker pull returned imageId: %s", imageId);
        assertTrue(imageId.startsWith("sha256:"));


        final String runOnImageName = "blackducksoftware/blackduck-imageinspector-alpine";
        final String runOnTagName = "5.0.1";
        final String containerName = "blackduck-imageinspector-alpine_3146_test-windows";
        final ImageInspectorOsEnum inspectorOs = ImageInspectorOsEnum.ALPINE;
        final int containerPort = 80802;
        final int hostPort = 9000;
        final String appNameLabelValue = "imageinspector-alpine";
        final String jarPath = "/opt/blackduck//blackduck-imageinspector/blackduck-imageinspector.jar";
        final String inspectorUrlAlpine = "http://localhost:9000";
        final String inspectorUrlCentos = "http://localhost:9001";
        final String inspectorUrlUbuntu = "http://localhost:9002";
        String containerId = dockerClientManager.startContainerAsService(runOnImageName, runOnTagName,
                containerName, inspectorOs, containerPort, hostPort, appNameLabelValue,
                jarPath, inspectorUrlAlpine, inspectorUrlCentos, inspectorUrlUbuntu);

        System.out.printf("containerId: %s", containerId);
        assertTrue(StringUtils.isNotBlank(containerId));
    }
}
