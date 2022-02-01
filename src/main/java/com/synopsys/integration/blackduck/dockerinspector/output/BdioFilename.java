/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.output;

public class BdioFilename {
    private final String spdxName;

    public BdioFilename(final String spdxName) {
        this.spdxName = spdxName;
    }

    public String getBdioFilename() {
        final String bdioFilename = String.format("%s_bdio.jsonld", spdxName);
        return bdioFilename;
    }
}
