package com.synopsys.integration.blackduck.dockerinspector.help;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.DockerInspectorOption;
import com.synopsys.integration.blackduck.dockerinspector.help.format.Converter;
import com.synopsys.integration.blackduck.dockerinspector.help.format.MarkdownToHtmlConverter;
import com.synopsys.integration.blackduck.dockerinspector.help.format.MarkdownToMarkdownConverter;
import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;
import com.synopsys.integration.exception.IntegrationException;

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

    @Mock
    private HelpReader helpReader;

    @InjectMocks
    private HelpText helpText;

    @Test
    public void testTextOverview() throws IntegrationException, IllegalAccessException, IOException {
        Mockito.when(helpTopicParser.translateGivenTopicNames("overview")).thenReturn("overview");
        Mockito.when(helpTopicParser.deriveHelpTopicList("overview")).thenReturn(Arrays.asList("overview"));
        Mockito.when(helpFormatParser.getHelpFormat()).thenReturn(HelpFormat.MARKDOWN);
        final Converter converter = new MarkdownToMarkdownConverter();
        final String actualHelpString = "help line 1\nhelp line 2";
        Mockito.when(helpReader.getStringFromHelpFile("overview")).thenReturn(actualHelpString);

        final String returnedHelpString = helpText.get(converter, "overview");

        assertTrue(returnedHelpString.contains(actualHelpString));
    }

    @Test
    public void testTextProperties() throws IntegrationException, IllegalArgumentException, IllegalAccessException {
        Mockito.when(helpTopicParser.translateGivenTopicNames("properties")).thenReturn("properties");
        Mockito.when(helpTopicParser.deriveHelpTopicList("properties")).thenReturn(Arrays.asList("properties"));
        Mockito.when(helpFormatParser.getHelpFormat()).thenReturn(HelpFormat.MARKDOWN);
        final SortedSet<DockerInspectorOption> configOptions = new TreeSet<>();
        configOptions.add(new DockerInspectorOption("blackduck.url", "testBlackDuckUrl", "Black Duck URL", String.class, "", "public", false));
        Mockito.when(config.getPublicConfigOptions()).thenReturn(configOptions);
        final Converter converter = new MarkdownToMarkdownConverter();

        final String usageString = helpText.get(converter, "properties");

        assertTrue(usageString.contains("blackduck.url [String]: Black Duck URL"));
    }

    @Test
    public void testHtmlDeployment() throws IntegrationException, IllegalArgumentException, IllegalAccessException, IOException {
        Mockito.when(helpTopicParser.translateGivenTopicNames("deployment")).thenReturn("deployment");
        Mockito.when(helpTopicParser.deriveHelpTopicList("deployment")).thenReturn(Arrays.asList("deployment"));
        Mockito.when(helpFormatParser.getHelpFormat()).thenReturn(HelpFormat.HTML);
        Mockito.when(config.getHelpOutputFormat()).thenReturn("html");
        final Converter converter = new MarkdownToHtmlConverter();
        final String deploymentHelpString = FileUtils.readFileToString(new File("src/main/resources/help/content/deployment.md"), StandardCharsets.UTF_8);
        Mockito.when(helpReader.getStringFromHelpFile("deployment")).thenReturn(deploymentHelpString);

        final String deploymentHtml = helpText.get(converter, "deployment");

        assertTrue(deploymentHtml.contains("<p>The challenges involved in deploying Docker Inspector using the 'toolkit' approach are:</p>"));
    }

    @Test
    public void testHtmlProperties() throws IntegrationException, IllegalArgumentException, IllegalAccessException {
        Mockito.when(helpTopicParser.translateGivenTopicNames("properties")).thenReturn("properties");
        Mockito.when(helpTopicParser.deriveHelpTopicList("properties")).thenReturn(Arrays.asList("properties"));
        Mockito.when(helpFormatParser.getHelpFormat()).thenReturn(HelpFormat.HTML);
        final SortedSet<DockerInspectorOption> configOptions = new TreeSet<>();
        configOptions.add(new DockerInspectorOption("blackduck.url", "testBlackDuckUrl", "Black Duck URL", String.class, "", "public", false));
        Mockito.when(config.getPublicConfigOptions()).thenReturn(configOptions);
        Mockito.when(config.getHelpOutputFormat()).thenReturn("HtmL");
        final Converter converter = new MarkdownToHtmlConverter();

        final String deploymentHtml = helpText.get(converter, "properties");

        verifyPropertiesHtml(deploymentHtml);
    }

    @Test
    public void testHtmlAll() throws IntegrationException, IllegalArgumentException, IllegalAccessException, IOException {
        Mockito.when(helpTopicParser.translateGivenTopicNames("all")).thenReturn("all");
        Mockito.when(helpTopicParser.deriveHelpTopicList("all")).thenReturn(Arrays.asList("program", "overview", "architecture", "running", "advanced", "deployment", "troubleshooting", "releasenotes", "properties"));
        Mockito.when(helpFormatParser.getHelpFormat()).thenReturn(HelpFormat.HTML);
        final SortedSet<DockerInspectorOption> configOptions = new TreeSet<>();
        configOptions.add(new DockerInspectorOption("blackduck.url", "testBlackDuckUrl", "Black Duck URL", String.class, "", "public", false));
        Mockito.when(config.getPublicConfigOptions()).thenReturn(configOptions);
        Mockito.when(config.getHelpOutputFormat()).thenReturn("HtmL");
        Mockito.when(programVersion.getProgramNamePretty()).thenReturn("Black Duck Docker Inspector");
        Mockito.when(programVersion.getProgramVersion()).thenReturn("1.2.3");
        final Converter converter = new MarkdownToHtmlConverter();

        String helpString = FileUtils.readFileToString(new File("src/main/resources/help/content/overview.md"), StandardCharsets.UTF_8);
        Mockito.when(helpReader.getStringFromHelpFile("overview")).thenReturn(helpString);
        helpString = FileUtils.readFileToString(new File("src/main/resources/help/content/architecture.md"), StandardCharsets.UTF_8);
        Mockito.when(helpReader.getStringFromHelpFile("architecture")).thenReturn(helpString);
        helpString = FileUtils.readFileToString(new File("src/main/resources/help/content/running.md"), StandardCharsets.UTF_8);
        Mockito.when(helpReader.getStringFromHelpFile("running")).thenReturn(helpString);
        helpString = FileUtils.readFileToString(new File("src/main/resources/help/content/advanced.md"), StandardCharsets.UTF_8);
        Mockito.when(helpReader.getStringFromHelpFile("advanced")).thenReturn(helpString);
        helpString = FileUtils.readFileToString(new File("src/main/resources/help/content/deployment.md"), StandardCharsets.UTF_8);
        Mockito.when(helpReader.getStringFromHelpFile("deployment")).thenReturn(helpString);
        helpString = FileUtils.readFileToString(new File("src/main/resources/help/content/troubleshooting.md"), StandardCharsets.UTF_8);
        Mockito.when(helpReader.getStringFromHelpFile("troubleshooting")).thenReturn(helpString);
        helpString = FileUtils.readFileToString(new File("src/main/resources/help/content/releasenotes.md"), StandardCharsets.UTF_8);
        Mockito.when(helpReader.getStringFromHelpFile("releasenotes")).thenReturn(helpString);

        final String deploymentHtml = helpText.get(converter, "all");

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
