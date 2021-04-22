/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.exception;

import com.synopsys.integration.exception.IntegrationException;

public class HelpGenerationException extends IntegrationException {

    public HelpGenerationException(final String message) {
        super(message);
    }

    public HelpGenerationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
