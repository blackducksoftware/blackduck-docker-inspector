/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.httpclient.response;

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
