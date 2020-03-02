/**
 * buildSrc
 *
 * Copyright (c) 2020 Synopsys, Inc.
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
package com.synopsys.integration.blackduck.dockerinspector.artifactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

import com.google.gson.Gson;
import com.synopsys.integration.Common;
import com.synopsys.integration.executable.Executable;
import com.synopsys.integration.executable.ExecutableOutput;
import com.synopsys.integration.executable.ExecutableRunnerException;
import com.synopsys.integration.executable.ProcessBuilderRunner;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class UpdateArtifactoryPropertiesTask extends DefaultTask {
    private static final String LATEST_PROPERTY_KEY = "DOCKER_INSPECTOR_LATEST";
    private static final String LATEST_AIR_GAP_PROPERTY_KEY = "DOCKER_INSPECTOR_AIR_GAP_LATEST";

    private final IntLogger logger = new Slf4jIntLogger(getLogger());
    private final Gson gson = new Gson();
    private final Project project = this.getProject();

    @TaskAction
    public void updateArtifactoryProperties() {
        final String projectVersion = project.getVersion().toString();

        final boolean isSnapshot = StringUtils.endsWith(projectVersion, "-SNAPSHOT");

        if (isSnapshot || "true".equals(project.findProperty("qa.build"))) {
            logger.alwaysLog("For a snapshot or qa build, artifactory properties will not be updated.");
        } else {
            try {
                logger.alwaysLog("For a release build, an update of artifactory properties will be attempted.");

                final String artifactoryDeployerUsername = getExtensionProperty(Common.PROPERTY_ARTIFACTORY_DEPLOYER_USERNAME);
                final String artifactoryDeployerPassword = getExtensionProperty(Common.PROPERTY_ARTIFACTORY_DEPLOYER_PASSWORD);
                final String artifactoryDeploymentUrl = getExtensionProperty(Common.PROPERTY_DEPLOY_ARTIFACTORY_URL);
                final String artifactoryRepository = getExtensionProperty(Common.PROPERTY_ARTIFACTORY_REPO);
                final String artifactoryDownloadUrl = getExtensionProperty(Common.PROPERTY_DOWNLOAD_ARTIFACTORY_URL);

                final String artifactoryCredentials = String.format("%s:%s", artifactoryDeployerUsername, artifactoryDeployerPassword);
                final List<String> defaultCurlArgs = Arrays.asList("--silent", "--insecure", "--user", artifactoryCredentials, "--header", "Content-Type: application/json");
                final Optional<ArtifactSearchResultElement> currentArtifact = findCurrentArtifact(defaultCurlArgs, artifactoryDeploymentUrl, artifactoryRepository);

                if (currentArtifact.isPresent()) {
                    final String majorVersion = projectVersion.split("\\.")[0];
                    final String majorVersionPropertyKey = String.format("%s_%s", LATEST_PROPERTY_KEY, majorVersion);
                    final String airGapVersionPropertyKey = String.format("%s_%s", LATEST_AIR_GAP_PROPERTY_KEY, majorVersion);
                    final String downloadUri = currentArtifact.get().getDownloadUri();
                    final String updatedDownloadUri = downloadUri.replace(artifactoryDeploymentUrl, artifactoryDownloadUrl);

                    setArtifactoryProperty(defaultCurlArgs, artifactoryDeploymentUrl, artifactoryRepository, LATEST_PROPERTY_KEY, updatedDownloadUri);
                    setArtifactoryProperty(defaultCurlArgs, artifactoryDeploymentUrl, artifactoryRepository, majorVersionPropertyKey, updatedDownloadUri);
                    setArtifactoryProperty(defaultCurlArgs, artifactoryDeploymentUrl, artifactoryRepository, airGapVersionPropertyKey, updatedDownloadUri);
                } else {
                    logger.alwaysLog(String.format("Artifactory properties won't be updated since %s-%s was not found.", project.getName(), projectVersion));
                }
            } catch (final ExecutableRunnerException e) {
                logger.alwaysLog(String.format("Manual corrections to the properties for %s-%s may be necessary.", project.getName(), projectVersion));
                logger.error(String.format("Error correcting the artifactory properties: %s", e.getMessage()), e);
            }
        }
    }

    private String getExtensionProperty(final String propertyName) {
        return Optional.ofNullable(project.findProperty(propertyName))
                   .map(Object::toString)
                   .orElseThrow(() -> new IllegalArgumentException(String.format("Missing Gradle extension property '%s' which is required to set Artifactory properties.", propertyName)));
    }

    private Optional<ArtifactSearchResultElement> findCurrentArtifact(final List<String> defaultCurlArgs, final String artifactoryDeploymentUrl, final String artifactoryRepo) {
        final String projectName = project.getName();
        final String projectVersion = project.getVersion().toString();

        try {
            final String url = String.format("%s/api/search/artifact?name=%s-%s.jar&repos=%s", artifactoryDeploymentUrl, projectName, projectVersion, artifactoryRepo);
            final ArtifactSearchResult artifactSearchResult = getArtifactoryItems(url, defaultCurlArgs);

            if (artifactSearchResult.getResults() == null) {
                logger.error(String.format("Failed to get any results from %s.", url));
                return Optional.empty();
            }

            if (artifactSearchResult.getResults().size() != 1) {
                logger.error(String.format("Unexpected number of search results. Expected 1 but found %d.", artifactSearchResult.getResults().size()));
                return Optional.empty();
            }

            return artifactSearchResult.getResults().stream().findFirst();
        } catch (final ExecutableRunnerException e) {
            logger.error(String.format("Could not find the current artifact: %s", e.getMessage()), e);
        }

        return Optional.empty();
    }

    private ArtifactSearchResult getArtifactoryItems(final String url, final List<String> defaultCurlArgs) throws ExecutableRunnerException {
        final List<String> curlArgs = new ArrayList<>(Arrays.asList("--header", "X-Result-Detail: info", url));
        curlArgs.addAll(defaultCurlArgs);
        final ExecutableOutput executableOutput = curlResponse(curlArgs);

        return gson.fromJson(executableOutput.getStandardOutput(), ArtifactSearchResult.class);
    }

    // TODO: Don't use curl. Lets make an HttpClient and do it proper.
    private ExecutableOutput setArtifactoryProperty(final List<String> defaultCurlArgs, final String artifactoryDeploymentUrl, final String deploymentRepositoryKey, final String propertyKey, final String propertyValue)
        throws ExecutableRunnerException {

        final List<String> curlArgs = new ArrayList<>(defaultCurlArgs);
        final String artifactLocation = String.format("%s/api/metadata/%s/com/synopsys/integration/%s", artifactoryDeploymentUrl, deploymentRepositoryKey, project.getName());
        curlArgs.addAll(Arrays.asList(
            "--request",
            "PATCH",
            "--data",
            String.format("{\"props\":{\"%s\":\"%s\"}}", propertyKey, propertyValue),
            artifactLocation
        ));

        logger.alwaysLog(String.format("Setting %s to %s on %s.", propertyKey, propertyValue, artifactLocation));
        return curlResponse(curlArgs);
    }

    public ExecutableOutput curlResponse(final List<String> curlArgs) throws ExecutableRunnerException {
        final File workingDirectory = new File(System.getProperty("user.dir"));
        final List<String> command = new ArrayList<>(Collections.singletonList("curl"));
        command.addAll(curlArgs);
        final Executable executable = new Executable(workingDirectory, new HashMap<>(), command);

        final ProcessBuilderRunner processBuilderRunner = new ProcessBuilderRunner(logger);
        return processBuilderRunner.execute(executable);
    }
}
