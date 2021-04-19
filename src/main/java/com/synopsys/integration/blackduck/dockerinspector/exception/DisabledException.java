/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.exception;

import com.synopsys.integration.exception.IntegrationException;

public class DisabledException extends IntegrationException {
    private static final long serialVersionUID = -8752417293450489927L;

    public DisabledException(final String message) {
        super(message);
    }

    public DisabledException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
