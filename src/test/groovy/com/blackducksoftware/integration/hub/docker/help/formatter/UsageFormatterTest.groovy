package com.blackducksoftware.integration.hub.docker.help.formatter

import static org.junit.Assert.*

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import com.blackducksoftware.integration.hub.docker.config.Config
import com.blackducksoftware.integration.hub.docker.config.DockerInspectorOption
import com.blackducksoftware.integration.hub.docker.help.formatter.UsageFormatter

class UsageFormatterTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void test() {
        final UsageFormatter helpPrinter = new UsageFormatter();
        List<DockerInspectorOption> configOptions = new ArrayList<>();
        configOptions.add(new DockerInspectorOption("hub.url", "hubUrl", "testHubUrl", "Hub URL", String.class, "", Config.GROUP_PUBLIC, false));
        Config config = [
            isOnHost: { true },
            isDryRun: { false },
            getLinuxDistro: { "" },
            getDockerTar: { "" },
            getDockerImage: { targetImageName },
            getDockerImageId: { "" },
            getTargetImageName: { "" },
            getDockerImageRepo: { targetImageName },
            getDockerImageTag : { "" },
            getHubUrl: { "test prop public string value" },
            setDockerImageRepo: {},
            setDockerImageTag: {
            },
            getHubUrl: { "testHubUrl" },
            getPublicConfigOptions: { configOptions }
        ] as Config;
        helpPrinter.config = config;
        List<String> usageStrings = helpPrinter.getStringList();
        assertTrue(usageStrings.size() >= 16);
        assertEquals("Usage: hub-docker-inspector.sh <options>", usageStrings.get(0))
        assertEquals("options: any supported property can be set by adding to the command line", usageStrings.get(1))
    }
}
