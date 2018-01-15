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
package com.blackducksoftware.integration.hub.docker.imageinspector.linux.extractor;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.hub.bdio.BdioWriter;
import com.blackducksoftware.integration.hub.bdio.SimpleBdioFactory;
import com.blackducksoftware.integration.hub.bdio.graph.MutableDependencyGraph;
import com.blackducksoftware.integration.hub.bdio.model.Forge;
import com.blackducksoftware.integration.hub.bdio.model.SimpleBdioDocument;
import com.blackducksoftware.integration.hub.bdio.model.dependency.Dependency;
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalId;
import com.blackducksoftware.integration.hub.docker.imageinspector.PackageManagerEnum;
import com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.ImagePkgMgr;
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.executor.PkgMgrExecutor;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;

public abstract class Extractor {
    private final Logger logger = LoggerFactory.getLogger(Extractor.class);
    private PackageManagerEnum packageManagerEnum;
    private PkgMgrExecutor executor;
    private List<String> forges;

    public abstract void init();

    public String deriveArchitecture(final File targetImageFileSystemRootDir) throws IOException {
        return null;
    }

    public abstract void extractComponents(MutableDependencyGraph dependencies, String dockerImageRepo, String dockerImageTag, ExtractionDetails extractionDetails, String[] packageList);

    void initValues(final PackageManagerEnum packageManagerEnum, final PkgMgrExecutor executor, final List<String> forges) {
        this.packageManagerEnum = packageManagerEnum;
        this.executor = executor;
        this.forges = forges;
    }

    public PackageManagerEnum getPackageManagerEnum() {
        return packageManagerEnum;
    }

    public SimpleBdioDocument extract(final String dockerImageRepo, final String dockerImageTag, final ImagePkgMgr imagePkgMgr, final ExtractionDetails extractionDetails, final String codeLocationName, final String projectName,
            final String version) throws HubIntegrationException, IOException, InterruptedException {

        final SimpleBdioDocument bdioDocument = extractBdio(dockerImageRepo, dockerImageTag, imagePkgMgr, extractionDetails, codeLocationName, projectName, version);
        return bdioDocument;
    }

    public void writeBdio(final BdioWriter bdioWriter, final SimpleBdioDocument bdioDocument) {
        (new SimpleBdioFactory()).writeSimpleBdioDocument(bdioWriter, bdioDocument);
    }

    private SimpleBdioDocument extractBdio(final String dockerImageRepo, final String dockerImageTag, final ImagePkgMgr imagePkgMgr, final ExtractionDetails extractionDetails, final String codeLocationName, final String projectName,
            final String version) throws HubIntegrationException, IOException, InterruptedException {
        final Forge forgeObject = new Forge(forges.get(0), "/");
        final ExternalId projectExternalId = (new SimpleBdioFactory()).createNameVersionExternalId(forgeObject, projectName, version);
        final SimpleBdioDocument bdioDocument = (new SimpleBdioFactory()).createSimpleBdioDocument(codeLocationName, projectName, version, projectExternalId);
        final MutableDependencyGraph dependencies = (new SimpleBdioFactory()).createMutableDependencyGraph();

        extractComponents(dependencies, dockerImageRepo, dockerImageTag, extractionDetails, executor.runPackageManager(imagePkgMgr));
        logger.info(String.format("Found %s potential components", dependencies.getRootDependencies().size()));

        (new SimpleBdioFactory()).populateComponents(bdioDocument, projectExternalId, dependencies);
        return bdioDocument;
    }

    public void createBdioComponent(final MutableDependencyGraph dependencies, final String name, final String version, final String externalId, final String arch) {
        for (final String forge : forges) {
            final Forge forgeObj = new Forge(forge, "/");
            final ExternalId extId = (new SimpleBdioFactory()).createArchitectureExternalId(forgeObj, name, version, arch);
            final Dependency dep = (new SimpleBdioFactory()).createDependency(name, version, extId); // createDependencyNode(forge, name, version, arch);
            logger.trace(String.format("adding %s as child to dependency node tree; dataId: %s", dep.name, dep.externalId.createBdioId()));
            dependencies.addChildToRoot(dep);
        }
    }
}
