/**
 * hub-docker-inspector
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
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
package com.synopsys.integration.blackduck.dockerinspector.restclient.response;

import java.util.HashMap;
import java.util.Map;

public class SimpleResponse {

    private final int statusCode;
    private final Map<String, String> headers;
    private final String body;

    public SimpleResponse(final int statusCode, final Map<String, String> headers, final String body) {
        super();
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Map<String, String> getHeaders() {
        if (headers == null) {
            return new HashMap<>(0);
        }
        return headers;
    }

    public String getWarningHeaderValue() {
        final String warningHeaderValue = getHeaders().get("Warning");
        if (warningHeaderValue == null) {
            return "";
        }
        return warningHeaderValue;
    }

    public String getBody() {
        if (body == null) {
            return "";
        }
        return body;
    }

}
