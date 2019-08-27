package com.synopsys.integration.blackduck.dockerinspector.help;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class HelpTopicInterpreter {

    // TODO: auto generate "all"?
    public static final String HELP_TOPIC_NAME_PROGRAM_NAMEVERSION = "program";
    public static final String HELP_TOPIC_NAME_OVERVIEW = "overview";
    public static final String HELP_TOPIC_NAME_PROPERTIES = "properties";
    private static final String HELP_TOPIC_NAME_ALL = "all";
    private static final String ALL_HELP_TOPICS = String.format("%s,%s,architecture,running,%s,advanced,deployment,troubleshooting,releasenotes",
        HELP_TOPIC_NAME_PROGRAM_NAMEVERSION, HELP_TOPIC_NAME_OVERVIEW, HELP_TOPIC_NAME_PROPERTIES);

    public List<String> deriveHelpTopicList(final String helpTopicNames) {
        if (StringUtils.isBlank(helpTopicNames)) {
            return Arrays.asList("");
        }
        return Arrays.asList(helpTopicNames.split(","));
    }

    public String translateGivenTopicNames(final String givenHelpTopics) {
        if (StringUtils.isBlank(givenHelpTopics)) {
            return HELP_TOPIC_NAME_OVERVIEW;
        }
        if (HELP_TOPIC_NAME_ALL.equalsIgnoreCase(givenHelpTopics)) {
            return ALL_HELP_TOPICS;
        }
        return givenHelpTopics;
    }
}
