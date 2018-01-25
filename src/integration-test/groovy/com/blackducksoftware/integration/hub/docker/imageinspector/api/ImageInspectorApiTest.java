package com.blackducksoftware.integration.hub.docker.imageinspector.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.blackducksoftware.integration.hub.bdio.model.BdioProject;
import com.blackducksoftware.integration.hub.bdio.model.SimpleBdioDocument;
import com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.ImagePkgMgr;
import com.blackducksoftware.integration.hub.docker.imageinspector.lib.OperatingSystemEnum;
import com.blackducksoftware.integration.hub.docker.imageinspector.lib.PackageManagerEnum;
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.Os;
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.extractor.Extractor;
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.extractor.ExtractorManager;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { AppConfig.class })
public class ImageInspectorApiTest {

    private static final String IMAGE_TARFILE = "build/images/test/alpine.tar";

    private static final String MOCKED_PROJECT_ID = "mockedProjectId";

    @Autowired
    private ImageInspectorApi imageInspectorApi;

    @MockBean
    private Os os;

    @MockBean
    private ExtractorManager extractorManager;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testOnWrongOs() throws HubIntegrationException, IOException, InterruptedException {
        assertNotNull(imageInspectorApi);
        Mockito.when(os.deriveCurrentOs()).thenReturn(null);
        try {
            imageInspectorApi.getBdio(IMAGE_TARFILE, "SB001", "unitTest1", null);
            fail("Expected WrongInspectorOsException");
        } catch (final WrongInspectorOsException e) {
            System.out.println(String.format("Can't inspect on this OS; need to inspect on %s", e.getcorrectInspectorOs().name()));
            assertEquals(OperatingSystemEnum.ALPINE.name(), e.getcorrectInspectorOs().name());
        }
    }

    @Test
    public void testOnRightOs() throws HubIntegrationException, IOException, InterruptedException {
        assertNotNull(imageInspectorApi);
        Mockito.when(os.deriveCurrentOs()).thenReturn(OperatingSystemEnum.ALPINE);
        final List<Extractor> mockExtractors = new ArrayList<>();
        final Extractor mockExtractor = Mockito.mock(Extractor.class);
        Mockito.when(mockExtractor.deriveArchitecture(Mockito.any(File.class))).thenReturn("testArch");
        Mockito.when(mockExtractor.getPackageManagerEnum()).thenReturn(PackageManagerEnum.APK);
        final SimpleBdioDocument mockedBdioDocument = new SimpleBdioDocument();
        mockedBdioDocument.project = new BdioProject();
        mockedBdioDocument.project.id = MOCKED_PROJECT_ID;
        Mockito.when(mockExtractor.extract(Mockito.anyString(), Mockito.anyString(), Mockito.any(ImagePkgMgr.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(mockedBdioDocument);
        mockExtractors.add(mockExtractor);
        Mockito.when(extractorManager.getExtractors()).thenReturn(mockExtractors);
        final SimpleBdioDocument returnedBdioDocument = imageInspectorApi.getBdio(IMAGE_TARFILE, "SB001", "unitTest1", null);
        assertEquals(MOCKED_PROJECT_ID, returnedBdioDocument.project.id);
    }
}
