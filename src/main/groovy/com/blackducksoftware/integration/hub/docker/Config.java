package com.blackducksoftware.integration.hub.docker;

import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.docker.client.ClassPathPropertiesFile;

@Component
public class Config {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    ConfigurableEnvironment configurableEnvironment;

    @PostConstruct
    public void init() throws IOException {
        final ClassPathPropertiesFile props = new ClassPathPropertiesFile("application.properties");
        final SortedSet<String> sortedPropNames = new TreeSet<>();
        for (final Object propObj : props.keySet()) {
            if (propObj instanceof String) {
                final String propName = (String) propObj;
                sortedPropNames.add(propName);
            }
        }
        for (final String propName : sortedPropNames) {
            logger.info(String.format("=== property name: %s", propName));
        }
    }
}
