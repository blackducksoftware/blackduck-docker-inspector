/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.help;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class HelpTopicParser {

    public static final String HELP_TOPIC_NAME_OVERVIEW = "overview";
    public static final String HELP_TOPIC_NAME_PROPERTIES = "properties";
    private static final String HELP_TOPIC_NAME_ALL = "all";
    private static final String ALL_HELP_TOPICS = String.format("index,%s,architecture,quickstart,running,%s,advanced,deployment,troubleshooting,releasenotes",
        HELP_TOPIC_NAME_OVERVIEW, HELP_TOPIC_NAME_PROPERTIES);

    public String translateGivenTopicNames(final String givenHelpTopics) {
        if (StringUtils.isBlank(givenHelpTopics)) {
            return HELP_TOPIC_NAME_OVERVIEW;
        }
        if (HELP_TOPIC_NAME_ALL.equalsIgnoreCase(givenHelpTopics)) {
            return ALL_HELP_TOPICS;
        }
        return givenHelpTopics;
    }

    public List<String> deriveHelpTopicList(final String helpTopicNames) {
        if (StringUtils.isBlank(helpTopicNames)) {
            return Arrays.asList("");
        }
        return Arrays.asList(helpTopicNames.split(","));
    }
}
