package com.synopsys.integration.blackduck.dockerinspector.help;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.synopsys.integration.blackduck.dockerinspector.help.HelpText;

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
import com.synopsys.integration.blackduck.dockerinspector.help.HelpTopic;

@RunWith(SpringRunner.class)
public class HelpTextTest {

    @InjectMocks
    private HelpText helpText;

    @Mock
    private Config config;

    @Test
    public void testTextOverview() throws IllegalArgumentException, IllegalAccessException, IOException {
        final SortedSet<DockerInspectorOption> configOptions = new TreeSet<>();
        configOptions.add(new DockerInspectorOption("blackduck.url", "testBlackDuckUrl", "Black Duck URL", String.class, "", "public", false));
        Mockito.when(config.getPublicConfigOptions()).thenReturn(configOptions);

        final List<String> usageStrings = helpText.getStringList(HelpTopic.OVERVIEW);
        assertTrue(usageStrings.size() >= 16);
        assertEquals("Usage: blackduck-docker-inspector.sh <options>", usageStrings.get(0));
        assertEquals("options: any supported property can be set by adding to the command line", usageStrings.get(1));
        final String usageString = StringUtils.join(usageStrings, ";");
        assertTrue(usageString.contains("blackduck.url [String]: Black Duck URL"));
    }

    @Test
    public void testHtmlDeployment() throws IllegalArgumentException, IllegalAccessException, IOException {
//        final SortedSet<DockerInspectorOption> configOptions = new TreeSet<>();
//        configOptions.add(new DockerInspectorOption("blackduck.url", "testBlackDuckUrl", "Black Duck URL", String.class, "", "public", false));
//        Mockito.when(config.getPublicConfigOptions()).thenReturn(configOptions);

        final String deploymentHtml = helpText.getHtml(HelpTopic.DEPLOYMENT);
        System.out.printf("deploymentHtml: %s\n", deploymentHtml);

        final File outputFile = new File("test/output/deployment.html");
        FileUtils.writeStringToFile(outputFile, deploymentHtml, StandardCharsets.UTF_8);

//        assertTrue(usageStrings.size() >= 16);
//        assertEquals("Usage: blackduck-docker-inspector.sh <options>", usageStrings.get(0));
//        assertEquals("options: any supported property can be set by adding to the command line", usageStrings.get(1));
//        final String usageString = StringUtils.join(usageStrings, ";");
//        assertTrue(usageString.contains("blackduck.url [String]: Black Duck URL"));
    }

}
