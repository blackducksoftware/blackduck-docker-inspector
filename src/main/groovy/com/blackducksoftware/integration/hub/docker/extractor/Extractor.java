/**
 * Hub Docker Inspector
 *
 * Copyright (C) 2017 Black Duck Software, Inc.
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
package com.blackducksoftware.integration.hub.docker.extractor;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.hub.bdio.simple.BdioNodeFactory;
import com.blackducksoftware.integration.hub.bdio.simple.BdioPropertyHelper;
import com.blackducksoftware.integration.hub.bdio.simple.BdioWriter;
import com.blackducksoftware.integration.hub.bdio.simple.DependencyNodeBuilder;
import com.blackducksoftware.integration.hub.bdio.simple.model.BdioBillOfMaterials;
import com.blackducksoftware.integration.hub.bdio.simple.model.BdioComponent;
import com.blackducksoftware.integration.hub.bdio.simple.model.BdioProject;
import com.blackducksoftware.integration.hub.bdio.simple.model.DependencyNode;
import com.blackducksoftware.integration.hub.bdio.simple.model.Forge;
import com.blackducksoftware.integration.hub.bdio.simple.model.externalid.ArchitectureExternalId;
import com.blackducksoftware.integration.hub.bdio.simple.model.externalid.ExternalId;
import com.blackducksoftware.integration.hub.bdio.simple.model.externalid.NameVersionExternalId;
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum;
import com.blackducksoftware.integration.hub.docker.executor.PkgMgrExecutor;
import com.blackducksoftware.integration.hub.docker.tar.ImagePkgMgr;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;

public abstract class Extractor {
    private final Logger logger = LoggerFactory.getLogger(Extractor.class);
    private final BdioPropertyHelper bdioPropertyHelper = new BdioPropertyHelper();
    private final BdioNodeFactory bdioNodeFactory = new BdioNodeFactory(bdioPropertyHelper);

    private PackageManagerEnum packageManagerEnum;
    private PkgMgrExecutor executor;
    private List<String> forges;

    public abstract void init();

    public String deriveArchitecture(final File targetImageFileSystemRootDir) throws IOException {
        return null;
    }

    public abstract List<BdioComponent> extractComponents(String dockerImageRepo, String dockerImageTag, ExtractionDetails extractionDetails, String[] packageList);

    void initValues(final PackageManagerEnum packageManagerEnum, final PkgMgrExecutor executor, final List<String> forges) {
        this.packageManagerEnum = packageManagerEnum;
        this.executor = executor;
        this.forges = forges;
    }

    public PackageManagerEnum getPackageManagerEnum() {
        return packageManagerEnum;
    }

    public void extract(final String dockerImageRepo, final String dockerImageTag, final ImagePkgMgr imagePkgMgr, final BdioWriter bdioWriter, final ExtractionDetails extractionDetails, final String codeLocationName,
            final String projectName, final String version) throws HubIntegrationException, IOException, InterruptedException {
        final BdioBillOfMaterials bom = bdioNodeFactory.createBillOfMaterials(codeLocationName, projectName, version);
        bdioWriter.writeBdioNode(bom);

        final Forge forgeObject = new Forge(forges.get(0), "/");
        final ExternalId extId = new NameVersionExternalId(forgeObject, projectName, version);
        final String externalId = extId.createExternalId();
        final BdioProject projectNode = bdioNodeFactory.createProject(projectName, version, extId.createDataId(), extractionDetails.getOperatingSystem().getForge(), externalId);
        logger.debug(String.format("BDIO project ID: %s", projectNode.id));
        final List<BdioComponent> components = extractComponents(dockerImageRepo, dockerImageTag, extractionDetails, executor.runPackageManager(imagePkgMgr));
        logger.info(String.format("Found %s potential components", components.size()));
        bdioPropertyHelper.addRelationships(projectNode, components);
        bdioWriter.writeBdioNode(projectNode);
        for (final BdioComponent component : components) {
            bdioWriter.writeBdioNode(component);
        }
    }

    public void createBdioComponent(final DependencyNodeBuilder dNodeBuilder, final DependencyNode rootNode, final List<BdioComponent> components, final String name, final String version, final String externalId, final String arch) {
        for (final String forge : forges) {
            final BdioComponent bdioComponent = bdioNodeFactory.createComponent(name, version, getComponentBdioId(forge, name, version), forge, externalId);
            components.add(bdioComponent);
            final DependencyNode dNode = createDependencyNode(forge, name, version, arch);
            logger.trace(String.format("adding %s as child to dependency node tree; dataId: %s", dNode.name, dNode.externalId.createDataId()));
            dNodeBuilder.addParentNodeWithChildren(rootNode, Arrays.asList(dNode));
        }
    }

    protected DependencyNode createDependencyNode(final String forge, final String name, final String version, final String arch) {
        logger.trace(String.format("Creating dependency node with forge: %s, name: %s; version: %s, arch: %s", forge, name, version, arch));
        final Forge forgeObj = new Forge(forge, "/");
        final DependencyNode dNode = new DependencyNode(name, version, new ArchitectureExternalId(forgeObj, name, version, arch));
        logger.trace(String.format("Generated DependencyNode: %s", dNode));
        return dNode;
    }

    private String getComponentBdioId(final String forge, final String name, final String version) {
        return String.format("data:%s/%s/%s", forge, name, version);
    }
}
