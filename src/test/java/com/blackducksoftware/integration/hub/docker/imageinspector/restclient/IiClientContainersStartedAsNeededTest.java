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
import com.blackducksoftware.integration.hub.docker.dockerinspector.restclient.ImageInspectorClientContainersStartedAsNeeded;
import com.blackducksoftware.integration.hub.docker.dockerinspector.restclient.ImageInspectorServices;
import com.blackducksoftware.integration.hub.docker.dockerinspector.restclient.RestConnectionCreator;
import com.blackducksoftware.integration.hub.docker.dockerinspector.restclient.RestRequestor;
import com.blackducksoftware.integration.hub.imageinspector.lib.OperatingSystemEnum;
import com.blackducksoftware.integration.hub.rest.RestConnection;

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

    @Test
    public void test() throws IntegrationException, IOException {
        Mockito.when(config.isImageInspectorServiceStart()).thenReturn(true);
        Mockito.when(imageInspectorPorts.getDefaultImageInspectorPort()).thenReturn(8080);
        Mockito.when(config.getCommandTimeout()).thenReturn(5000L);
        Mockito.when(config.getImageInspectorDefault()).thenReturn("ubuntu");

        final RestConnection restConnection = Mockito.mock(RestConnection.class);
        Mockito.when(restConnectionCreator.createNonRedirectingConnection(Mockito.anyString(), Mockito.anyInt())).thenReturn(restConnection);

        Mockito.when(restRequestor.executeSimpleGetRequest(Mockito.any(RestConnection.class), Mockito.anyString(), Mockito.anyString())).thenReturn("{\"status\":\"UP\"}");
        // Mockito.when(restRequestor.executeSimpleGetRequest(Mockito.any(RestConnection.class), Mockito.anyString(), Mockito.anyString())).thenReturn("testResponse");
        Mockito.when(restRequestor.executeGetBdioRequest(Mockito.any(RestConnection.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean())).thenReturn("testResult");

        Mockito.when(inspectorImages.getInspectorImageName(Mockito.any(OperatingSystemEnum.class))).thenReturn("blackduck/hub-imageinspector-ws");
        Mockito.when(inspectorImages.getInspectorImageTag(Mockito.any(OperatingSystemEnum.class))).thenReturn("1.1.1");

        assertEquals(true, imageInspectorClientContainersStartedAsNeeded.isApplicable());
        assertEquals("testResult", imageInspectorClientContainersStartedAsNeeded.getBdio("/tmp/t.tar", "containerFileSystemFilename", true));
    }

}
