/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.config;

public class SpringValueUtils {

    private SpringValueUtils() {
    }

    public static String springKeyFromValueAnnotation(String value) {
        if (value.contains("${")) {
            value = value.substring(2);
        }
        if (value.endsWith("}")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.contains(":")) {
            value = value.split(":")[0];
        }
        return value;
    }

}
