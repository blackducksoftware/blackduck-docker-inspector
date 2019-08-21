package com.synopsys.integration.blackduck.dockerinspector.help;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.DockerInspectorOption;

@RunWith(SpringRunner.class)
public class HelpTextTest {

    @InjectMocks
    private HelpText helpText;

    @Mock
    private Config config;

    @Test
    public void testTextOverview() throws IllegalArgumentException, IllegalAccessException, IOException {
        final String usageString = helpText.get("overview");
        assertTrue(usageString.length() >= 100);
        assertTrue(usageString.contains("Usage: blackduck-docker-inspector.sh <options>"));
        assertTrue(usageString.contains("options: any supported property can be set by adding to the command line"));
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
        final String deploymentHtml = helpText.get("deployment.html");
        assertTrue(deploymentHtml.contains("<p>The challenges involved in deploying Docker Inspector using the 'toolkit' approach are:</p>"));
    }

    @Test
    public void testHtmlProperties() throws IllegalArgumentException, IllegalAccessException, IOException {
        final SortedSet<DockerInspectorOption> configOptions = new TreeSet<>();
        configOptions.add(new DockerInspectorOption("blackduck.url", "testBlackDuckUrl", "Black Duck URL", String.class, "", "public", false));
        Mockito.when(config.getPublicConfigOptions()).thenReturn(configOptions);

        final String deploymentHtml = helpText.get("properties.html");
        assertTrue(deploymentHtml.contains("<h1>Available properties:</h1>\n"
                                               + "<ul>\n"
                                               + "<li>blackduck.url [String]: Black Duck URL</li>\n"
                                               + "</ul>\n"));
    }

}
