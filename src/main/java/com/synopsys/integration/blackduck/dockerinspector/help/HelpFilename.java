/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2019 Synopsys, Inc.
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

import javax.naming.OperationNotSupportedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;

@Component
public class HelpFilename {

    @Autowired
    private ProgramVersion programVersion;

    @Autowired
    private HelpFormatParser helpFormatParser;

    public String getDefaultFinalFilename() throws OperationNotSupportedException {
        final HelpFormat helpFormat = helpFormatParser.getHelpFormat();
        final String fileExtension;
        switch (helpFormat) {
            case HTML:
                fileExtension = "html";
                break;
            case MARKDOWN:
                fileExtension = "md";
                break;
            default:
                throw new OperationNotSupportedException(String.format("Unsupported help format: %s", helpFormat));
        }
        return String.format("%s-%s-help.%s", programVersion.getProgramId(), programVersion.getProgramVersion(), fileExtension);
    }

    public String getDefaultMarkdownFilename() throws OperationNotSupportedException {
        return String.format("%s-%s-help.%s", programVersion.getProgramId(), programVersion.getProgramVersion(), "md");
    }
}
