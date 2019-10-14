package com.synopsys.integration.blackduck.dockerinspector.help;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.exception.HelpGenerationException;

@RunWith(SpringRunner.class)
public class HelpWriterTest {

    @Mock
    private Config config;

    @Mock
    private HelpText helpText;

    @Mock
    private HelpTopicParser helpTopicParser;

    @InjectMocks
    private HelpWriter helpWriter;

    @Test
    public void test() throws HelpGenerationException {
        Mockito.when(config.getHelpOutputFilePath()).thenReturn("test/output");

        helpWriter.write("all");

        System.out.println("Done.");
    }
}
