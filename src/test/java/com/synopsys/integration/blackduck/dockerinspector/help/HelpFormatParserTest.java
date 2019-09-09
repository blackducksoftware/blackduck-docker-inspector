package com.synopsys.integration.blackduck.dockerinspector.help;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;

@RunWith(SpringRunner.class)
public class HelpFormatParserTest {

    @Mock
    private Config config;

    @InjectMocks
    private HelpFormatParser helpFormatParser;

    @Test
    public void testHtml() {
        Mockito.when(config.getHelpOutputFormat()).thenReturn("html");

        final HelpFormat derivedHelpFormat = helpFormatParser.getHelpFormat();

        assertEquals(HelpFormat.HTML, derivedHelpFormat);
    }

    @Test
    public void testMarkdown() {
        Mockito.when(config.getHelpOutputFormat()).thenReturn("markdown");

        final HelpFormat derivedHelpFormat = helpFormatParser.getHelpFormat();

        assertEquals(HelpFormat.MARKDOWN, derivedHelpFormat);
    }
}
