package com.synopsys.integration.blackduck.dockerinspector.help;

import static org.junit.Assert.assertEquals;

import javax.naming.OperationNotSupportedException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;

import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;

@RunWith(SpringRunner.class)
public class HelpFilenameTest {

    @Mock
    private ProgramVersion programVersion;

    @InjectMocks
    private HelpFilename helpFilename;

    @Test
    public void testMarkdown() throws OperationNotSupportedException {
        Mockito.when(programVersion.getProgramId()).thenReturn("blackduck-docker-inspector");
        Mockito.when(programVersion.getProgramVersion()).thenReturn("1.2.3");

        final String derivedhelpFilename = helpFilename.getDefaultFinalFilename();

        assertEquals("blackduck-docker-inspector-1.2.3-help.md", derivedhelpFilename);
    }
}
