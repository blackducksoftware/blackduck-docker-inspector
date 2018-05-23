package com.blackducksoftware.integration.hub.docker.imageinspector.restclient;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.docker.dockerinspector.InspectorImages;
import com.blackducksoftware.integration.hub.docker.dockerinspector.config.Config;
import com.blackducksoftware.integration.hub.docker.dockerinspector.dockerclient.DockerClientManager;
import com.blackducksoftware.integration.hub.docker.dockerinspector.dockerclient.HubDockerClient;
import com.blackducksoftware.integration.hub.docker.dockerinspector.restclient.ContainerPathsTargetDirCopiedFromHost;
import com.blackducksoftware.integration.hub.docker.dockerinspector.restclient.ImageInspectorClientContainersStartedAsNeeded;
import com.blackducksoftware.integration.hub.docker.dockerinspector.restclient.ImageInspectorServices;
import com.blackducksoftware.integration.hub.docker.dockerinspector.restclient.RestConnectionCreator;
import com.blackducksoftware.integration.hub.docker.dockerinspector.restclient.RestRequestor;
import com.blackducksoftware.integration.hub.docker.dockerinspector.restclient.response.SimpleResponse;
import com.blackducksoftware.integration.hub.imageinspector.api.ImageInspectorOsEnum;
import com.blackducksoftware.integration.hub.imageinspector.lib.OperatingSystemEnum;
import com.blackducksoftware.integration.rest.RestConstants;
import com.blackducksoftware.integration.rest.connection.RestConnection;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;

@RunWith(SpringRunner.class)
public class IiClientContainersStartedAsNeededTest {

    @InjectMocks
    private ImageInspectorClientContainersStartedAsNeeded imageInspectorClientContainersStartedAsNeeded;

    @Mock
    private Config config;

    @Mock
    private ImageInspectorServices imageInspectorPorts;

    @Mock
    private RestConnectionCreator restConnectionCreator;

    @Mock
    private RestRequestor restRequestor;

    @Mock
    private InspectorImages inspectorImages;

    @Mock
    private DockerClientManager dockerClientManager;

    @Mock
    private HubDockerClient hubDockerClient;

    @Mock
    private ContainerPathsTargetDirCopiedFromHost containerPaths;

    @Test
    public void test() throws IntegrationException, IOException {
        Mockito.when(config.isImageInspectorServiceStart()).thenReturn(true);
        Mockito.when(imageInspectorPorts.getDefaultImageInspectorHostPort()).thenReturn(8080);
        Mockito.when(config.getCommandTimeout()).thenReturn(5000L);
        Mockito.when(config.getImageInspectorDefault()).thenReturn("ubuntu");

        final Container targetContainer = Mockito.mock(Container.class);
        Mockito.when(targetContainer.getImage()).thenReturn("target");
        Mockito.when(dockerClientManager.getRunningContainerByAppName(Mockito.any(DockerClient.class), Mockito.anyString(), Mockito.any(ImageInspectorOsEnum.class))).thenReturn(targetContainer);

        final DockerClient dockerClient = Mockito.mock(DockerClient.class);
        Mockito.when(hubDockerClient.getDockerClient()).thenReturn(dockerClient);

        final RestConnection restConnection = Mockito.mock(RestConnection.class);
        Mockito.when(restConnectionCreator.createNonRedirectingConnection(Mockito.anyString(), Mockito.anyInt())).thenReturn(restConnection);

        Mockito.when(restRequestor.executeSimpleGetRequest(Mockito.any(RestConnection.class), Mockito.anyString(), Mockito.anyString())).thenReturn("{\"status\":\"UP\"}");
        // Mockito.when(restRequestor.executeSimpleGetRequest(Mockito.any(RestConnection.class), Mockito.anyString(), Mockito.anyString())).thenReturn("testResponse");
        final SimpleResponse response = new SimpleResponse(RestConstants.OK_200, null, "testResult");
        Mockito.when(restRequestor.executeGetBdioRequest(Mockito.any(RestConnection.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean())).thenReturn(response);

        Mockito.when(containerPaths.getContainerPathToOutputDir()).thenReturn("/opt/blackduck/hub-imageinspector-ws/shared/output");
        Mockito.when(inspectorImages.getInspectorImageName(Mockito.any(OperatingSystemEnum.class))).thenReturn("blackduck/hub-imageinspector-ws");
        Mockito.when(inspectorImages.getInspectorImageTag(Mockito.any(OperatingSystemEnum.class))).thenReturn("1.1.1");

        assertEquals(true, imageInspectorClientContainersStartedAsNeeded.isApplicable());
        assertEquals("testResult", imageInspectorClientContainersStartedAsNeeded.getBdio("/tmp/t.tar", "/tmp/t.tar", "containerFileSystemFilename", true));
    }

}
