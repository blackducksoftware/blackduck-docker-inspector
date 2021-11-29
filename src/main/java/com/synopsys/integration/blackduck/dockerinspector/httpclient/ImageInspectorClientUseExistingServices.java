/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.httpclient;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.httpclient.response.SimpleResponse;
import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.RestConstants;
import com.synopsys.integration.rest.client.IntHttpClient;

@Component
public class ImageInspectorClientUseExistingServices extends ImageInspectorClient {

    @Autowired
    private Config config;

    @Autowired
    private HttpRequestor restRequester;

    @Autowired
    private HttpConnectionCreator httpConnectionCreator;

    @Autowired
    private ImageInspectorServices imageInspectorServices;

    @Autowired
    private ProgramVersion programVersion;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public boolean isApplicable() {
        boolean answer = !config.isImageInspectorServiceStart() && StringUtils.isNotBlank(config.getImageInspectorUrl());
        logger.debug(String.format("isApplicable() returning %b", answer));
        return answer;
    }

    @Override
    public String getBdio(String hostPathToTarfile, String containerPathToInputDockerTarfile, String givenImageRepo, String givenImageTag,
        String containerPathToOutputFileSystemFile, String containerFileSystemExcludedPaths,
        boolean organizeComponentsByLayer, boolean includeRemovedComponents,
        boolean cleanup, String platformTopLayerId,
        String targetLinuxDistro)
        throws IntegrationException, MalformedURLException {
        URI imageInspectorUri;
        try {
            imageInspectorUri = new URI(config.getImageInspectorUrl());
        } catch (URISyntaxException e) {
            throw new IntegrationException(String.format("Error constructing URI from %s: %s", config.getImageInspectorUrl(), e.getMessage()), e);
        }
        int serviceRequestTimeoutSeconds = deriveTimeoutSeconds();
        IntHttpClient httpClient = httpConnectionCreator
                                       .createRedirectingConnection(imageInspectorUri, serviceRequestTimeoutSeconds);
        checkServiceVersion(programVersion, imageInspectorServices, httpClient, imageInspectorUri);
        SimpleResponse response = restRequester.executeGetBdioRequest(httpClient, imageInspectorUri, containerPathToInputDockerTarfile,
            givenImageRepo, givenImageTag,
            containerPathToOutputFileSystemFile, containerFileSystemExcludedPaths,
            organizeComponentsByLayer, includeRemovedComponents, cleanup,
            platformTopLayerId,
            targetLinuxDistro);

        if (response.getStatusCode() >= RestConstants.BAD_REQUEST_400) {
            throw new IntegrationException(String.format("getBdio request returned status: %d: %s", response.getStatusCode(), response.getBody()));
        }

        return response.getBody();
    }

    private int deriveTimeoutSeconds() {
        return (int) (config.getServiceTimeout() / 1000L);
    }
}
