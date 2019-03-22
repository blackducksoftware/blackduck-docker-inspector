package com.synopsys.integration.blackduck.dockerinspector.httpclient;

import static org.junit.Assert.assertEquals;

import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;
import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;

@RunWith(SpringRunner.class)
public class InspectorImagesTest {
    private static final String PROGRAM_VERSION = "1.2.3";

    @Mock
    private Config config;

    @Mock
    private ProgramVersion programVersion;

    @InjectMocks
    private InspectorImages osMapper;

    @Test
    public void testBasic() throws IOException {

        Mockito.when(config.getInspectorRepository()).thenReturn("blackducksoftware");
        Mockito.when(config.getInspectorImageFamily()).thenReturn("blackduck-imageinspector");
        Mockito.when(config.getInspectorImageVersion()).thenReturn(PROGRAM_VERSION);
        Mockito.when(programVersion.getProgramVersion()).thenReturn(PROGRAM_VERSION);
        osMapper.init();

        assertEquals("blackducksoftware/blackduck-imageinspector-centos", osMapper.getInspectorImageName(ImageInspectorOsEnum.CENTOS));
        assertEquals(PROGRAM_VERSION, osMapper.getInspectorImageTag(ImageInspectorOsEnum.CENTOS));

        assertEquals("blackducksoftware/blackduck-imageinspector-ubuntu", osMapper.getInspectorImageName(ImageInspectorOsEnum.UBUNTU));
        assertEquals(PROGRAM_VERSION, osMapper.getInspectorImageTag(ImageInspectorOsEnum.UBUNTU));

        assertEquals("blackducksoftware/blackduck-imageinspector-alpine", osMapper.getInspectorImageName(ImageInspectorOsEnum.ALPINE));
        assertEquals(PROGRAM_VERSION, osMapper.getInspectorImageTag(ImageInspectorOsEnum.ALPINE));
    }

    @Test
    public void testAlternateRepoWithoutSlash() {
        Mockito.when(config.getInspectorRepository()).thenReturn("myrepo");
        Mockito.when(config.getInspectorImageFamily()).thenReturn("blackduck-imageinspector");
        Mockito.when(config.getInspectorImageVersion()).thenReturn(PROGRAM_VERSION);
        Mockito.when(programVersion.getProgramVersion()).thenReturn(PROGRAM_VERSION);
        osMapper.init();

        assertEquals("myrepo/blackduck-imageinspector-centos", osMapper.getInspectorImageName(ImageInspectorOsEnum.CENTOS));
    }

    @Test
    public void testAlternateRepoWithSlash() {
        Mockito.when(config.getInspectorRepository()).thenReturn("myrepo/");
        Mockito.when(config.getInspectorImageFamily()).thenReturn("blackduck-imageinspector");
        Mockito.when(config.getInspectorImageVersion()).thenReturn(PROGRAM_VERSION);
        Mockito.when(programVersion.getProgramVersion()).thenReturn(PROGRAM_VERSION);
        osMapper.init();

        assertEquals("myrepo/blackduck-imageinspector-centos", osMapper.getInspectorImageName(ImageInspectorOsEnum.CENTOS));
    }

    @Test
    public void testNoRepo() {
        Mockito.when(config.getInspectorRepository()).thenReturn("");
        Mockito.when(config.getInspectorImageFamily()).thenReturn("blackduck-imageinspector");
        Mockito.when(config.getInspectorImageVersion()).thenReturn(PROGRAM_VERSION);
        Mockito.when(programVersion.getProgramVersion()).thenReturn(PROGRAM_VERSION);
        osMapper.init();

        assertEquals("blackduck-imageinspector-centos", osMapper.getInspectorImageName(ImageInspectorOsEnum.CENTOS));
    }
}
