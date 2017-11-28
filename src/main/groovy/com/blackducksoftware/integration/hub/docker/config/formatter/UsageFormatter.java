package com.blackducksoftware.integration.hub.docker.config.formatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
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
        final List<DockerInspectorOption> configOptions = config.getPublicConfigOptions();
        for (final DockerInspectorOption opt : configOptions) {
            if (!StringUtils.isBlank(opt.getDefaultValue())) {
                usage.add(String.format("  --%s: [%s]: %s; default: %s", opt.getKey(), opt.getValueTypeString(), opt.getDescription(), opt.getDefaultValue()));
            } else {
                usage.add(String.format("  --%s: [%s]: %s", opt.getKey(), opt.getValueTypeString(), opt.getDescription()));
            }
        }
        return usage;
    }
}
