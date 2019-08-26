package com.synopsys.integration.blackduck.dockerinspector.help;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
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

    @InjectMocks
    private HelpText helpText;

    @Mock
    private Config config;

    @Mock
    private ProgramVersion programVersion;

    @Test
    public void testTextOverview() throws IllegalArgumentException, IllegalAccessException, IOException {
        final String usageString = helpText.get("overview");
        assertTrue(usageString.length() >= 100);
        assertTrue(usageString.contains("Usage: blackduck-docker-inspector.sh <Docker Inspector arguments>"));
        assertTrue(usageString.contains("Any supported property can be set by adding to the command line"));
    }

    @Test
    public void testTextProperties() throws IllegalArgumentException, IllegalAccessException, IOException {
        final SortedSet<DockerInspectorOption> configOptions = new TreeSet<>();
        configOptions.add(new DockerInspectorOption("blackduck.url", "testBlackDuckUrl", "Black Duck URL", String.class, "", "public", false));
        Mockito.when(config.getPublicConfigOptions()).thenReturn(configOptions);

        final String usageString = helpText.get("properties");
        assertTrue(usageString.contains("blackduck.url [String]: Black Duck URL"));
    }

    @Test
    public void testHtmlDeployment() throws IllegalArgumentException, IllegalAccessException, IOException {
        Mockito.when(config.getHelpOutputFormat()).thenReturn("html");
        final String deploymentHtml = helpText.get("deployment");
        assertTrue(deploymentHtml.contains("<p>The challenges involved in deploying Docker Inspector using the 'toolkit' approach are:</p>"));
    }

    @Test
    public void testHtmlProperties() throws IllegalArgumentException, IllegalAccessException, IOException {
        final SortedSet<DockerInspectorOption> configOptions = new TreeSet<>();
        configOptions.add(new DockerInspectorOption("blackduck.url", "testBlackDuckUrl", "Black Duck URL", String.class, "", "public", false));
        Mockito.when(config.getPublicConfigOptions()).thenReturn(configOptions);
        Mockito.when(config.getHelpOutputFormat()).thenReturn("HtmL");

        final String deploymentHtml = helpText.get("properties");
        assertTrue(deploymentHtml.contains("<h2>Available properties:</h2>\n"
                                               + "<ul>\n"
                                               + "<li>blackduck.url [String]: Black Duck URL</li>\n"
                                               + "</ul>\n"));
    }

    @Test
    public void testHtmlAll() throws IllegalArgumentException, IllegalAccessException, IOException {
        final SortedSet<DockerInspectorOption> configOptions = new TreeSet<>();
        configOptions.add(new DockerInspectorOption("blackduck.url", "testBlackDuckUrl", "Black Duck URL", String.class, "", "public", false));
        Mockito.when(config.getPublicConfigOptions()).thenReturn(configOptions);
        Mockito.when(config.getHelpOutputFormat()).thenReturn("HtmL");
        Mockito.when(programVersion.getProgramVersion()).thenReturn("1.2.3");

        final String deploymentHtml = helpText.get("all");
        assertTrue(deploymentHtml.contains("<h1>Black Duck Docker Inspector 1.2.3</h1>\n"
                                               + "<h2>Overview</h2>"));
        assertTrue(deploymentHtml.contains("<h2>Architecture</h2>"));
        assertTrue(deploymentHtml.contains("<h2>Running Docker Inspector</h2>"));
        assertTrue(deploymentHtml.contains("<h2>Available properties:</h2>\n"
                                               + "<ul>\n"
                                               + "<li>blackduck.url [String]: Black Duck URL</li>\n"
                                               + "</ul>\n"));
        assertTrue(deploymentHtml.contains("<h2>Advanced topics</h2>"));
        assertTrue(deploymentHtml.contains("<h2>Deploying Docker Inspector</h2>"));
        assertTrue(deploymentHtml.contains("<h2>Troubleshooting</h2>"));
        assertTrue(deploymentHtml.contains("<h2>Release notes</h2>"));
    }

}
