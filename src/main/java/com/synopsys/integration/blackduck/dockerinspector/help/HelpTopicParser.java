/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
