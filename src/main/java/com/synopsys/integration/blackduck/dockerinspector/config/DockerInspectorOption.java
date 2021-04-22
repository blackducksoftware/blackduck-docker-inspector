/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.util.Stringable;

public class DockerInspectorOption extends Stringable implements Comparable<DockerInspectorOption> {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String key;
    private final String description;
    private final String valueTypeString;
    private final String defaultValue;
    private String resolvedValue;
    private final boolean deprecated;

    public DockerInspectorOption(final String key, final String resolvedValue, final String description, final Class<?> valueType, final String defaultValue, final boolean deprecated) {
        this.key = key;
        this.description = description;
        this.defaultValue = defaultValue;
        this.resolvedValue = resolvedValue;
        this.deprecated = deprecated;

        final String[] parts = valueType.toString().split("\\.");
        logger.trace(String.format("Split %s into %d parts", valueType.toString(), parts.length));
        this.valueTypeString = parts[parts.length - 1];
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public String getValueTypeString() {
        return valueTypeString;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getResolvedValue() {
        return resolvedValue;
    }

    public void setResolvedValue(final String resolvedValue) {
        this.resolvedValue = resolvedValue;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    @Override
    public int compareTo(final DockerInspectorOption o) {
        return this.getKey().compareTo(o.getKey());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (key == null ? 0 : key.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DockerInspectorOption other = (DockerInspectorOption) obj;
        if (key == null) {
            if (other.key != null) {
                return false;
            }
        } else if (!key.equals(other.key)) {
            return false;
        }
        return true;
    }
}
