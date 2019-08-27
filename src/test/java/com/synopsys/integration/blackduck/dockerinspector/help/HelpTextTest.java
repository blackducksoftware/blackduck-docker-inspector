package com.synopsys.integration.blackduck.dockerinspector.help;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.DockerInspectorOption;
import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;

@RunWith(SpringRunner.class)
public class HelpTextTest {

    @Mock
    private Config config;

    @Mock
    private ProgramVersion programVersion;

    @Mock
    private HelpTopicParser helpTopicParser;

    @Mock
    private HelpFormatParser helpFormatParser;

    @InjectMocks
    private HelpText helpText;

    @Test
    public void testTextOverview() throws IllegalArgumentException, IllegalAccessException, IOException {
        Mockito.when(helpTopicParser.translateGivenTopicNames("overview")).thenReturn("overview");
        Mockito.when(helpTopicParser.deriveHelpTopicList("overview")).thenReturn(Arrays.asList("overview"));
        Mockito.when(helpFormatParser.getHelpFormat()).thenReturn(HelpFormat.MARKDOWN);

        final String usageString = helpText.get("overview");

        assertTrue(usageString.length() >= 100);
        assertTrue(usageString.contains("Usage: blackduck-docker-inspector.sh <Docker Inspector arguments>"));
        assertTrue(usageString.contains("Any supported property can be set by adding to the command line"));
    }

    @Test
    public void testTextProperties() throws IllegalArgumentException, IllegalAccessException, IOException {
        Mockito.when(helpTopicParser.translateGivenTopicNames("properties")).thenReturn("properties");
        Mockito.when(helpTopicParser.deriveHelpTopicList("properties")).thenReturn(Arrays.asList("properties"));
        Mockito.when(helpFormatParser.getHelpFormat()).thenReturn(HelpFormat.MARKDOWN);
        final SortedSet<DockerInspectorOption> configOptions = new TreeSet<>();
        configOptions.add(new DockerInspectorOption("blackduck.url", "testBlackDuckUrl", "Black Duck URL", String.class, "", "public", false));
        Mockito.when(config.getPublicConfigOptions()).thenReturn(configOptions);

        final String usageString = helpText.get("properties");

        assertTrue(usageString.contains("blackduck.url [String]: Black Duck URL"));
    }

    @Test
    public void testHtmlDeployment() throws IllegalArgumentException, IllegalAccessException, IOException {
        Mockito.when(helpTopicParser.translateGivenTopicNames("deployment")).thenReturn("deployment");
        Mockito.when(helpTopicParser.deriveHelpTopicList("deployment")).thenReturn(Arrays.asList("deployment"));
        Mockito.when(helpFormatParser.getHelpFormat()).thenReturn(HelpFormat.HTML);
        Mockito.when(config.getHelpOutputFormat()).thenReturn("html");

        final String deploymentHtml = helpText.get("deployment");

        assertTrue(deploymentHtml.contains("<p>The challenges involved in deploying Docker Inspector using the 'toolkit' approach are:</p>"));
    }

    @Test
    public void testHtmlProperties() throws IllegalArgumentException, IllegalAccessException, IOException {
        Mockito.when(helpTopicParser.translateGivenTopicNames("properties")).thenReturn("properties");
        Mockito.when(helpTopicParser.deriveHelpTopicList("properties")).thenReturn(Arrays.asList("properties"));
        Mockito.when(helpFormatParser.getHelpFormat()).thenReturn(HelpFormat.HTML);
        final SortedSet<DockerInspectorOption> configOptions = new TreeSet<>();
        configOptions.add(new DockerInspectorOption("blackduck.url", "testBlackDuckUrl", "Black Duck URL", String.class, "", "public", false));
        Mockito.when(config.getPublicConfigOptions()).thenReturn(configOptions);
        Mockito.when(config.getHelpOutputFormat()).thenReturn("HtmL");

        final String deploymentHtml = helpText.get("properties");

        verifyPropertiesHtml(deploymentHtml);
    }

    @Test
    public void testHtmlAll() throws IllegalArgumentException, IllegalAccessException, IOException {
        Mockito.when(helpTopicParser.translateGivenTopicNames("all")).thenReturn("all");
        Mockito.when(helpTopicParser.deriveHelpTopicList("all")).thenReturn(Arrays.asList("program", "overview", "architecture", "running", "advanced", "deployment", "troubleshooting", "releasenotes", "properties"));
        Mockito.when(helpFormatParser.getHelpFormat()).thenReturn(HelpFormat.HTML);
        final SortedSet<DockerInspectorOption> configOptions = new TreeSet<>();
        configOptions.add(new DockerInspectorOption("blackduck.url", "testBlackDuckUrl", "Black Duck URL", String.class, "", "public", false));
        Mockito.when(config.getPublicConfigOptions()).thenReturn(configOptions);
        Mockito.when(config.getHelpOutputFormat()).thenReturn("HtmL");
        Mockito.when(programVersion.getProgramNamePretty()).thenReturn("Black Duck Docker Inspector");
        Mockito.when(programVersion.getProgramVersion()).thenReturn("1.2.3");

        final String deploymentHtml = helpText.get("all");

        System.out.println("DUMPING HTML OUTPUT:");
        System.out.println(deploymentHtml);
        assertTrue(deploymentHtml.contains(">Black Duck Docker Inspector 1.2.3"));
        assertTrue(deploymentHtml.contains(">Overview"));
        assertTrue(deploymentHtml.contains(">Architecture<"));
        assertTrue(deploymentHtml.contains(">Running Docker Inspector<"));
        verifyPropertiesHtml(deploymentHtml);
        assertTrue(deploymentHtml.contains(">Advanced topics<"));
        assertTrue(deploymentHtml.contains(">Deploying Docker Inspector<"));
        assertTrue(deploymentHtml.contains(">Troubleshooting<"));
        assertTrue(deploymentHtml.contains(">Release notes<"));

        // verify TOC is present
        assertTrue(deploymentHtml.contains("<a href=\"#architecture\">Architecture</a>"));
    }

    private void verifyPropertiesHtml(final String deploymentHtml) {
        assertTrue(deploymentHtml.contains(">Available properties:<"));
        assertTrue(deploymentHtml.contains(">blackduck.url [String]: Black Duck URL<"));
    }

}
