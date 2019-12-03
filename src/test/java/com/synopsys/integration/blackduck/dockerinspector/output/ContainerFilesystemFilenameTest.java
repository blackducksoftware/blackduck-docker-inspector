package com.synopsys.integration.blackduck.dockerinspector.output;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;

@RunWith(SpringRunner.class)
public class ContainerFilesystemFilenameTest {

    @Mock
    private Config config;

    @InjectMocks
    private ContainerFilesystemFilename containerFilesystemFilename;

    @Test
    public void testRepoTag() {

        Mockito.when(config.getDockerImage()).thenReturn("ubuntu:latest");
        assertEquals("ubuntu_latest_containerfilesystem.tar.gz", containerFilesystemFilename.deriveContainerFilesystemFilename());

    }

    @Test
    public void testTarfile() {

        Mockito.when(config.getDockerTar()).thenReturn("test.tar");
        assertEquals("test_containerfilesystem.tar.gz", containerFilesystemFilename.deriveContainerFilesystemFilename());

    }

    // TODO need a test for: image ID is specified
}
