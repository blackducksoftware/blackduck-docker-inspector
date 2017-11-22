package com.blackducksoftware.integration.hub.docker.config.formatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.docker.Application;
import com.blackducksoftware.integration.hub.docker.config.Config;
import com.blackducksoftware.integration.hub.docker.config.DockerInspectorOption;

@Component
public class UsageFormatter {

    @Autowired
    private Config config;

    public List<String> getStringList() throws IllegalArgumentException, IllegalAccessException, IOException {
        final List<String> usage = new ArrayList<>();
        usage.add(String.format("Usage: %s <options>; Available options:", Application.PROGRAM_NAME));
        final List<DockerInspectorOption> configOptions = config.getConfigOptions();
        for (final DockerInspectorOption opt : configOptions) {
            usage.add(String.format("\t--%s: type: %s; default: %s; description: %s; value: %s", opt.getKey(), opt.getValueTypeString(), opt.getDefaultValue(), opt.getDescription(), opt.getResolvedValue()));
        }
        return usage;
    }
}
