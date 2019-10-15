package com.synopsys.integration.blackduck.dockerinspector.help;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.exception.IntegrationException;

@RunWith(SpringRunner.class)
public class HelpWriterTest {

    public static final String TEST_OVERVIEW_CONTENT = "test overview content";
    public static final String TEST_ADVANCED_CONTENT = "test advanced content";
    @Mock
    private Config config;

    @Mock
    private HelpText helpText;

    @Mock
    private HelpTopicParser helpTopicParser;

    @InjectMocks
    private HelpWriter helpWriter;

    @Test
    public void test() throws IntegrationException, UnsupportedEncodingException, IllegalAccessException {
        Mockito.when(config.getHelpOutputFilePath()).thenReturn("test/output");
        Mockito.when(helpTopicParser.translateGivenTopicNames("all")).thenReturn("overview,architecture,running,properties,advanced,deployment,troubleshooting,releasenotes");
        Mockito.when(helpTopicParser.deriveHelpTopicList("overview,architecture,running,properties,advanced,deployment,troubleshooting,releasenotes")).thenReturn(
            Arrays.asList("overview", "architecture", "running", "properties", "advanced", "deployment", "troubleshooting", "releasenotes"));
        Mockito.when(helpText.getMarkdownForTopic("overview")).thenReturn(TEST_OVERVIEW_CONTENT);
        Mockito.when(helpText.getMarkdownForTopic("advanced")).thenReturn(TEST_ADVANCED_CONTENT);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8.name())) {
            helpWriter.concatinateContentToPrintStream(ps, "all");
        }

        final String helpContent = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(helpContent.contains(TEST_OVERVIEW_CONTENT));
        assertTrue(helpContent.contains(TEST_ADVANCED_CONTENT));
    }
}
