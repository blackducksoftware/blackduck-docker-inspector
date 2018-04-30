package com.blackducksoftware.integration.hub.docker.dockerinspector;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

import com.blackducksoftware.integration.hub.docker.dockerinspector.config.Config;
import com.blackducksoftware.integration.hub.imageinspector.lib.OperatingSystemEnum;

@RunWith(SpringRunner.class)
public class InspectorImagesTest {
    private static final String PROGRAM_VERSION = "1.2.3";

    @Mock
    private Config config;

    @Mock
    private ProgramVersion programVersion;

    @InjectMocks
    InspectorImages osMapper;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testBasic() throws IOException {

        BDDMockito.given(config.getInspectorRepository()).willReturn("blackducksoftware");
        BDDMockito.given(config.getInspectorImageFamily()).willReturn("hub-docker-inspector");
        BDDMockito.given(config.getInspectorImageVersion()).willReturn(PROGRAM_VERSION);

        BDDMockito.given(programVersion.getProgramVersion()).willReturn(PROGRAM_VERSION);

        assertEquals("blackducksoftware/hub-docker-inspector-centos", osMapper.getInspectorImageName(OperatingSystemEnum.CENTOS));
        assertEquals(PROGRAM_VERSION, osMapper.getInspectorImageTag(OperatingSystemEnum.CENTOS));
        assertEquals(OperatingSystemEnum.CENTOS, osMapper.getInspectorImageOs(OperatingSystemEnum.CENTOS));

        assertEquals("blackducksoftware/hub-docker-inspector-ubuntu", osMapper.getInspectorImageName(OperatingSystemEnum.UBUNTU));
        assertEquals(PROGRAM_VERSION, osMapper.getInspectorImageTag(OperatingSystemEnum.UBUNTU));
        assertEquals(OperatingSystemEnum.UBUNTU, osMapper.getInspectorImageOs(OperatingSystemEnum.UBUNTU));

        assertEquals("blackducksoftware/hub-docker-inspector-alpine", osMapper.getInspectorImageName(OperatingSystemEnum.ALPINE));
        assertEquals(PROGRAM_VERSION, osMapper.getInspectorImageTag(OperatingSystemEnum.ALPINE));
        assertEquals(OperatingSystemEnum.ALPINE, osMapper.getInspectorImageOs(OperatingSystemEnum.ALPINE));
    }

    @Test
    public void testAlternateRepoWithoutSlash() throws IOException {
        BDDMockito.given(config.getInspectorRepository()).willReturn("myrepo");
        BDDMockito.given(config.getInspectorImageFamily()).willReturn("hub-docker-inspector");
        BDDMockito.given(config.getInspectorImageVersion()).willReturn(PROGRAM_VERSION);

        BDDMockito.given(programVersion.getProgramVersion()).willReturn(PROGRAM_VERSION);

        assertEquals("myrepo/hub-docker-inspector-centos", osMapper.getInspectorImageName(OperatingSystemEnum.CENTOS));
    }

    @Test
    public void testAlternateRepoWithSlash() throws IOException {
        BDDMockito.given(config.getInspectorRepository()).willReturn("myrepo/");
        BDDMockito.given(config.getInspectorImageFamily()).willReturn("hub-docker-inspector");
        BDDMockito.given(config.getInspectorImageVersion()).willReturn(PROGRAM_VERSION);

        BDDMockito.given(programVersion.getProgramVersion()).willReturn(PROGRAM_VERSION);

        assertEquals("myrepo/hub-docker-inspector-centos", osMapper.getInspectorImageName(OperatingSystemEnum.CENTOS));
    }

    @Test
    public void testNoRepo() throws IOException {
        BDDMockito.given(config.getInspectorRepository()).willReturn("");
        BDDMockito.given(config.getInspectorImageFamily()).willReturn("hub-docker-inspector");
        BDDMockito.given(config.getInspectorImageVersion()).willReturn(PROGRAM_VERSION);

        BDDMockito.given(programVersion.getProgramVersion()).willReturn(PROGRAM_VERSION);

        assertEquals("hub-docker-inspector-centos", osMapper.getInspectorImageName(OperatingSystemEnum.CENTOS));
    }
}
