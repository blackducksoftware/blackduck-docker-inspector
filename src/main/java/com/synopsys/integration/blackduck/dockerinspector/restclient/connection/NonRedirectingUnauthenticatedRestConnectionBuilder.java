/**
 * blackduck-docker-inspector
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
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
package com.synopsys.integration.blackduck.dockerinspector.restclient.connection;



public class NonRedirectingUnauthenticatedRestConnectionBuilder /* extends RestConnectionBuilder<NonRedirectingUnauthenticatedRestConnection> */ {

//    @Override
//    protected NonRedirectingUnauthenticatedRestConnection buildWithoutValidation() {
//        final Optional<URL> url = getURL();
//
//        return new NonRedirectingUnauthenticatedRestConnection(getLogger(), url.orElse(null), getTimeout(), getProxyInfo());
//    }

    // TODO remove this whole class:
//    @Override
//    public AbstractValidator createValidator() {
//        final UnauthenticatedRestConnectionValidator validator = new UnauthenticatedRestConnectionValidator();
//        validator.setBaseUrl(getBaseUrl());
//        validator.setTimeout(getTimeout());
//        validator.setProxyHost(getProxyHost());
//        validator.setProxyPort(getProxyPort());
//        validator.setProxyUsername(getProxyUsername());
//        validator.setProxyPassword(getProxyPassword());
//        validator.setProxyIgnoreHosts(getProxyIgnoreHosts());
//        validator.setProxyNtlmDomain(getProxyNtlmDomain());
//        validator.setProxyNtlmWorkstation(getProxyNtlmWorkstation());
//        validator.setLogger(getLogger());
//        validator.setCommonRequestHeaders(getCommonRequestHeaders());
//        return validator;
//    }
//
//    @Override
//    public NonRedirectingUnauthenticatedRestConnection createConnection(final ProxyInfo proxyInfo) {
//        return new NonRedirectingUnauthenticatedRestConnection(getLogger(), getBaseConnectionUrl(), getTimeout(), proxyInfo);
//    }

}
