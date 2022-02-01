/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.httpclient;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.httpclient.response.SimpleResponse;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.HttpMethod;
import com.synopsys.integration.rest.HttpUrl;
import com.synopsys.integration.rest.client.IntHttpClient;
import com.synopsys.integration.rest.request.Request;
import com.synopsys.integration.rest.response.Response;

@Component
public class HttpRequestor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public SimpleResponse executeGetBdioRequest(IntHttpClient httpClient, URI imageInspectorUri,
        String containerPathToTarfile,
        String givenImageRepo, String givenImageTag,
        String containerPathToContainerFileSystemFile,
        String containerFileSystemExcludedPaths,
        boolean organizeComponentsByLayer,
        boolean includeRemovedComponents,
        boolean cleanup,
        String platformTopLayerId,
        String targetLinuxDistro)
        throws IntegrationException {
        String url = new ImageInspectorUrlBuilder()
                               .imageInspectorUri(imageInspectorUri)
                               .containerPathToTarfile(containerPathToTarfile)
                               .givenImageRepo(givenImageRepo)
                               .givenImageTag(givenImageTag)
                               .containerPathToContainerFileSystemFile(containerPathToContainerFileSystemFile)
                               .containerFileSystemExcludedPaths(containerFileSystemExcludedPaths)
                               .organizeComponentsByLayer(organizeComponentsByLayer)
                               .includeRemovedComponents(includeRemovedComponents)
                               .cleanup(cleanup)
                               .platformTopLayerId(platformTopLayerId)
                               .targetLinuxDistro(targetLinuxDistro)
                               .build();
        logger.debug(String.format("Doing a getBdio request on %s", url));
        HttpUrl httpUrl = new HttpUrl(url);
        Request request = new Request.Builder(httpUrl).method(HttpMethod.GET).build();
        try (Response response = httpClient.execute(request)) {
            logger.debug(String.format("Response: HTTP status: %d", response.getStatusCode()));
            return new SimpleResponse(response.getStatusCode(), response.getHeaders(), getResponseBody(response));
        } catch (IntegrationException ie) {
            if ((ie.getCause() != null) && (ie.getCause() instanceof java.net.SocketTimeoutException)) {
                logger.error(String.format("getBdio request on %s failed: The request to the image inspector service timed out. You might need to increase the value of the service timeout property.", url));
            } else {
                logger.error(String.format("getBdio request on %s failed: %s", url, ie.getMessage()));
            }
            throw ie;
        } catch (Exception e) {
            logger.error(String.format("getBdio request on %s failed: %s", url, e.getMessage()));
            throw new IntegrationException(e);
        }
    }

    public String executeSimpleGetRequest(IntHttpClient httpClient, URI imageInspectorUri, String endpoint)
        throws IntegrationException {
        if (endpoint.startsWith("/")) {
            endpoint = endpoint.substring(1);
        }
        String url = String.format("%s/%s", imageInspectorUri.toString(), endpoint);
        logger.debug(String.format("Doing a GET on %s", url));
        HttpUrl httpUrl = new HttpUrl(url);
        Request request = new Request.Builder(httpUrl).method(HttpMethod.GET).build();
        try (Response response = httpClient.execute(request)) {
            logger.debug(String.format("Response: HTTP status: %d", response.getStatusCode()));
            return getResponseBody(response);
        } catch (Exception e) {
            logger.debug(String.format("GET on %s failed: %s", url, e.getMessage()));
            throw new IntegrationException(e);
        }
    }

    private String getResponseBody(Response response) throws IntegrationException {
        String responseBody = response.getContentString();
        logger.trace(String.format("Response: body: %s", responseBody));
        return responseBody;
    }
}
