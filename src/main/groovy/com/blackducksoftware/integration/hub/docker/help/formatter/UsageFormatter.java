package com.blackducksoftware.integration.hub.docker.help.formatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.docker.DockerEnvImageInspector;
import com.blackducksoftware.integration.hub.docker.config.Config;
import com.blackducksoftware.integration.hub.docker.config.DockerInspectorOption;

@Component
public class UsageFormatter {

    @Autowired
    private Config config;

    public List<String> getStringList() throws IllegalArgumentException, IllegalAccessException, IOException {
        final List<String> usage = new ArrayList<>();
        usage.add(String.format("Usage: %s <options>", DockerEnvImageInspector.PROGRAM_NAME));
        usage.add("options: any supported property can be set by adding to the command line");
        usage.add("an option of the form:");
        usage.add("--<property name>=<value>");
        usage.add("");
        usage.add("Alternatively, any supported property can be set by adding to a text file named");
        usage.add("application.properties (in the current directory) a line of the form:");
        usage.add("<property name>=<value>");
        usage.add("");
        usage.add("For greater security, the Hub password can be set via the environment variable BD_HUB_PASSWORD.");
        usage.add("For example:");
        usage.add("  export BD_HUB_PASSWORD=mypassword");
        usage.add("  ./hub-docker-inspector.sh --hub.url=http://hub.mydomain.com:8080/ --hub.username=myusername --docker.image=ubuntu:latest");
        usage.add("");
        usage.add(String.format("Available properties:"));
        final List<DockerInspectorOption> configOptions = config.getPublicConfigOptions();
        for (final DockerInspectorOption opt : configOptions) {
            final StringBuilder usageLine = new StringBuilder(String.format("  %s [%s]: %s", opt.getKey(), opt.getValueTypeString(), opt.getDescription()));
            if (!StringUtils.isBlank(opt.getDefaultValue())) {
                usageLine.append(String.format("; default: %s", opt.getDefaultValue()));
            }
            if (opt.isDeprecated()) {
                usageLine.append(String.format("; [DEPRECATED]"));
            }
            usage.add(usageLine.toString());
        }
        usage.add("");
        usage.add("Documentation: https://blackducksoftware.atlassian.net/wiki/spaces/INTDOCS/pages/48435867/Hub+Docker+Inspector");
        return usage;
    }
}
