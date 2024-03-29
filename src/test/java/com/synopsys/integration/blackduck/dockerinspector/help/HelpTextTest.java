package com.synopsys.integration.blackduck.dockerinspector.help;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.DockerInspectorOption;
import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;
import com.synopsys.integration.exception.IntegrationException;

@ExtendWith(SpringExtension.class)
public class HelpTextTest {

    @Mock
    private Config config;

    @Mock
    private ProgramVersion programVersion;

    @Mock
    private HelpTopicParser helpTopicParser;

    @Mock
    private HelpReader helpReader;

    @InjectMocks
    private HelpText helpText;

    @Test
    public void testOverview() throws IntegrationException, IllegalAccessException, IOException {
        Mockito.when(helpTopicParser.translateGivenTopicNames("overview")).thenReturn("overview");
        Mockito.when(helpTopicParser.deriveHelpTopicList("overview")).thenReturn(Arrays.asList("overview"));
        final String actualHelpString = "help line 1\nhelp line 2";
        Mockito.when(helpReader.getVariableSubstitutedTextFromHelpFile("overview")).thenReturn(actualHelpString);

        String returnedHelpString = helpText.getMarkdownForTopic("overview");

        assertTrue(returnedHelpString.contains(actualHelpString));
    }

    @Test
    public void testProperties() throws IntegrationException, IllegalArgumentException, IllegalAccessException {
        Mockito.when(helpTopicParser.translateGivenTopicNames("properties")).thenReturn("properties");
        Mockito.when(helpTopicParser.deriveHelpTopicList("properties")).thenReturn(Arrays.asList("properties"));
        SortedSet<DockerInspectorOption> configOptions = new TreeSet<>();
        configOptions.add(new DockerInspectorOption("blackduck.url", "testBlackDuckUrl", "Black Duck URL", String.class, "", false, "deprecationMessage"));
        Mockito.when(config.getPublicConfigOptions()).thenReturn(configOptions);

        String usageString = helpText.getMarkdownForTopic("properties");

        assertTrue(usageString.contains("Property name | Type | Description | Default value | Deprecation Status | Deprecation Message\n"));
        assertTrue(usageString.contains("-------------\n"));
        assertTrue(usageString.contains("blackduck.url | String | Black Duck URL |  |"));
    }

    @Test
    public void testDeployment() throws IntegrationException, IllegalArgumentException, IllegalAccessException, IOException {
        Mockito.when(helpTopicParser.translateGivenTopicNames("deployment")).thenReturn("deployment");
        Mockito.when(helpTopicParser.deriveHelpTopicList("deployment")).thenReturn(Arrays.asList("deployment"));
        String deploymentHelpString = FileUtils.readFileToString(new File("src/main/resources/help/content/deployment.md"), StandardCharsets.UTF_8);
        Mockito.when(helpReader.getVariableSubstitutedTextFromHelpFile("deployment")).thenReturn(deploymentHelpString);

        String deploymentHtml = helpText.getMarkdownForTopic("deployment");

        assertTrue(deploymentHtml.contains("${solution_name} can be run in either of the following modes:"));
    }

    @Test
    public void testAll() throws IntegrationException, IllegalArgumentException, IllegalAccessException, IOException {
        Mockito.when(helpTopicParser.translateGivenTopicNames("all")).thenReturn("overview,architecture,running,properties,advanced,deployment,troubleshooting,releasenotes");
        Mockito.when(helpTopicParser.deriveHelpTopicList("overview,architecture,running,properties,advanced,deployment,troubleshooting,releasenotes"))
            .thenReturn(Arrays.asList("overview", "architecture", "running", "properties", "advanced", "deployment", "troubleshooting", "releasenotes"));
        SortedSet<DockerInspectorOption> configOptions = new TreeSet<>();
        configOptions.add(new DockerInspectorOption("blackduck.url", "testBlackDuckUrl", "Black Duck URL", String.class, "", false, "deprecationMessage"));
        Mockito.when(config.getPublicConfigOptions()).thenReturn(configOptions);
        Mockito.when(programVersion.getProgramNamePretty()).thenReturn("Black Duck Docker Inspector");
        Mockito.when(programVersion.getProgramVersion()).thenReturn("1.2.3");

        String helpString = FileUtils.readFileToString(new File("src/main/resources/help/content/overview.md"), StandardCharsets.UTF_8);
        Mockito.when(helpReader.getVariableSubstitutedTextFromHelpFile("overview")).thenReturn(helpString);
        helpString = FileUtils.readFileToString(new File("src/main/resources/help/content/architecture.md"), StandardCharsets.UTF_8);
        Mockito.when(helpReader.getVariableSubstitutedTextFromHelpFile("architecture")).thenReturn(helpString);
        helpString = FileUtils.readFileToString(new File("src/main/resources/help/content/running.md"), StandardCharsets.UTF_8);
        Mockito.when(helpReader.getVariableSubstitutedTextFromHelpFile("running")).thenReturn(helpString);
        helpString = FileUtils.readFileToString(new File("src/main/resources/help/content/advanced.md"), StandardCharsets.UTF_8);
        Mockito.when(helpReader.getVariableSubstitutedTextFromHelpFile("advanced")).thenReturn(helpString);
        helpString = FileUtils.readFileToString(new File("src/main/resources/help/content/deployment.md"), StandardCharsets.UTF_8);
        Mockito.when(helpReader.getVariableSubstitutedTextFromHelpFile("deployment")).thenReturn(helpString);
        helpString = FileUtils.readFileToString(new File("src/main/resources/help/content/troubleshooting.md"), StandardCharsets.UTF_8);
        Mockito.when(helpReader.getVariableSubstitutedTextFromHelpFile("troubleshooting")).thenReturn(helpString);
        helpString = FileUtils.readFileToString(new File("src/main/resources/help/content/releasenotes.md"), StandardCharsets.UTF_8);
        Mockito.when(helpReader.getVariableSubstitutedTextFromHelpFile("releasenotes")).thenReturn(helpString);

        String allHelpMarkdown = helpText.getMarkdownForTopics("all");

        // Due to simplified mocking, variables will not have been substituted (tested elsewhere). Testing aggregation here.
        assertTrue(allHelpMarkdown.contains("_Help version:"));
        assertTrue(allHelpMarkdown.contains("Modes of operation"));
        assertTrue(allHelpMarkdown.contains("Review the script before running it to make sure the side effects"));
        assertTrue(allHelpMarkdown.contains("Version 8.2.2"));
    }
}
