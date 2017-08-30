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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.hub.bdio.simple.BdioNodeFactory;
import com.blackducksoftware.integration.hub.bdio.simple.BdioPropertyHelper;
import com.blackducksoftware.integration.hub.bdio.simple.BdioWriter;
import com.blackducksoftware.integration.hub.bdio.simple.model.BdioBillOfMaterials;
import com.blackducksoftware.integration.hub.bdio.simple.model.BdioComponent;
import com.blackducksoftware.integration.hub.bdio.simple.model.BdioProject;
import com.blackducksoftware.integration.hub.bdio.simple.model.Forge;
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

    public abstract List<BdioComponent> extractComponents(ExtractionDetails extractionDetails, String[] packageList);

    void initValues(final PackageManagerEnum packageManagerEnum, final PkgMgrExecutor executor, final List<String> forges) {
        this.packageManagerEnum = packageManagerEnum;
        this.executor = executor;
        this.forges = forges;
    }

    public PackageManagerEnum getPackageManagerEnum() {
        return packageManagerEnum;
    }

    public void extract(final ImagePkgMgr imagePkgMgr, final BdioWriter bdioWriter, final ExtractionDetails extractionDetails, final String codeLocationName, final String projectName, final String version)
            throws HubIntegrationException, IOException, InterruptedException {
        final BdioBillOfMaterials bom = bdioNodeFactory.createBillOfMaterials(codeLocationName, projectName, version);
        bdioWriter.writeBdioNode(bom);

        final Forge forgeObject = new Forge(forges.get(0), "/");
        final ExternalId extId = new NameVersionExternalId(forgeObject, projectName, version);
        final String externalId = extId.createExternalId();
        final BdioProject projectNode = bdioNodeFactory.createProject(projectName, version, extId.createDataId(), extractionDetails.getOperatingSystem().getForge(), externalId);
        logger.debug(String.format("BDIO project ID: %s", projectNode.id));
        final List<BdioComponent> components = extractComponents(extractionDetails, executor.runPackageManager(imagePkgMgr));
        logger.info(String.format("Found %s potential components", components.size()));
        bdioPropertyHelper.addRelationships(projectNode, components);
        bdioWriter.writeBdioNode(projectNode);
        for (final BdioComponent component : components) {
            bdioWriter.writeBdioNode(component);
        }
    }

    public List<BdioComponent> createBdioComponent(final String name, final String version, final String externalId) {
        final List<BdioComponent> components = new ArrayList<>();
        for (final String forge : forges) {
            final BdioComponent bdioComponent = bdioNodeFactory.createComponent(name, version, getComponentBdioId(name, version), forge, externalId);
            components.add(bdioComponent);
        }
        return components;
    }

    private String getComponentBdioId(final String name, final String version) {
        return String.format("data:%s/%s", name, version);
    }
}
