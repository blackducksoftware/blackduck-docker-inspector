/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2019 Synopsys, Inc.
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
package com.synopsys.integration.blackduck.dockerinspector.help;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.DockerInspector;
import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.DockerInspectorOption;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

@Component
public class HelpText {

    @Autowired
    private Config config;

    public List<String> getStringList(final HelpTopic helpTopic) throws IllegalArgumentException, IllegalAccessException {
        switch (helpTopic) {
            case OVERVIEW:
                return getStringListOverview();
            case DEPLOYMENT:
                return getStringListDeployment();
            default:
                throw new UnsupportedOperationException(String.format("Help topic %s has not been implemented", helpTopic.toString()));
        }
    }

    public String getHtml(final HelpTopic helpTopic) throws IllegalArgumentException, IOException {
        switch (helpTopic) {
            case DEPLOYMENT:
                return getHtmlDeployment();
            default:
                throw new UnsupportedOperationException(String.format("Help topic %s has not been implemented", helpTopic.toString()));
        }
    }

    private String getHtmlDeployment() throws IOException {
        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        final InputStream helpFileInputStream = this.getClass().getResourceAsStream("/help/deployment.md");
        final String helpFileContents = readFromInputStream(helpFileInputStream);
        Node document = parser.parse(helpFileContents);
        String html = renderer.render(document);
        return html;
    }

    private String readFromInputStream(InputStream inputStream) throws IOException {
        final StringBuilder resultStringBuilder = new StringBuilder();
        try (final BufferedReader br
                 = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }

    private List<String> getStringListOverview() throws IllegalArgumentException, IllegalAccessException {
        final List<String> usage = new ArrayList<>();
        usage.add(String.format("Usage: %s <options>", DockerInspector.PROGRAM_NAME));
        usage.add("options: any supported property can be set by adding to the command line");
        usage.add("an option of the form:");
        usage.add("\t--<property name>=<value>");
        usage.add("");
        usage.add("Alternatively, any supported property can be set by adding to a text file named");
        usage.add("application.properties (in the current directory) a line of the form:");
        usage.add("<property name>=<value>");
        usage.add("");
        usage.add("For greater security, the Black Duck password can be set via the environment variable BD_PASSWORD.");
        usage.add("For example:");
        usage.add("  export BD_PASSWORD=mypassword");
        usage.add("  ./blackduck-docker-inspector.sh --blackduck.url=http://blackduck.mydomain.com:8080/ --blackduck.username=myusername --docker.image=ubuntu:latest");
        usage.add("");
        usage.add(String.format("Available properties:"));
        final SortedSet<DockerInspectorOption> configOptions = config.getPublicConfigOptions();
        for (final DockerInspectorOption opt : configOptions) {
            final StringBuilder usageLine = new StringBuilder(String.format("  %s [%s]: %s", opt.getKey(), opt.getValueTypeString(), opt.getDescription()));
            if (!StringUtils.isBlank(opt.getDefaultValue())) {
                usageLine.append(String.format("; default: %s", opt.getDefaultValue()));
            }
            if (opt.isDeprecated()) {
                usageLine.append(String.format("; [DEPRECATED]"));
            }
            usage.add(usageLine.toString());
        }
        usage.add("");
        usage.add("For more detailed help on deploying Docker Inspector, use:");
        usage.add("\t-h deployment");
        usage.add("");
        usage.add("");
        usage.add("Documentation is under Package Managers > Black Duck Docker Inspector at: https://blackducksoftware.atlassian.net/wiki/spaces/INTDOCS");
        return usage;
    }

    public List<String> getStringListDeployment() throws IllegalArgumentException, IllegalAccessException {
        final List<String> usage = new ArrayList<>();
        usage.add("Deploying Docker Inspector");
        usage.add("");
        usage.add("Black Duck Docker Inspector can be run in either of the following environments:\n\n" +
                      "1. A Linux machine (or Linux VM) with Docker.\n\n" +
                      "In this scenario, Docker Inspector is a command line utility that automatically pulls/runs and uses container-based services \n" +
                      "(and cleans them up when it's done). The Docker client (CLI) can be very useful for troubleshooting, but is not actually\n" +
                      "required or used by Docker Inspector.\n\n" +
                      "In this mode Docker Inspector does require access to a Docker Engine (very similar to the way the Docker client requires\n" +
                      "access to a Docker Engine) so it can pull and run Docker images (it uses the https://github.com/docker-java/docker-java\n" +
                      "library to perform Docker operations via the Docker Engine).\n\n" +
                      "In this mode, Docker Inspector automatically pulls, runs, stops, and removes the container-based image inspector services\n" +
                      "on which it depends. It accesses the services they provide via HTTP GET operations.\n\n" +
                      "This is the default mode, and the simplest to use.\n\n" +
                      "The documentation under Package Managers > Black Duck Docker Inspector at: https://synopsys.atlassian.net/wiki/spaces/INTDOCS\n" +
                      "provides all the information that is normally required to run Docker Inspector in this mode.\n\n" +
                      "2. A container orchestration platform such as Kubernetes, OpenShift, etc.\n\n" +
                      "In this scenario, Docker Inspector is a toolkit consisting of a command line utility (that you will run in one container), plus\n" +
                      "three container-based services (which you must start). These four containers must:\n" +
                      "(a) share a mounted volume (either persistent or temporary) that they will use to pass large files between containers, and\n" +
                      "(b) be able to reach each other via HTTP GET operations using base URLs that you will provide.");
        usage.add("");
        usage.add("Image Inspector Services\n\n" +
                      "Docker Inspector consists of a command line utility (provided in a Java .jar, but sometimes invoked via a bash script)\n" +
                      "and three image inspector services.\n\n" +
                      "The required Docker operations (if any) are performed by the command line utility, while the image inspector services\n" +
                     "perform the work of unpacking the target Docker image, extracting the Linux package manager database,\n" +
                     "and running the Linux package manager against that database in order to extract installed packages\n" +
                     "and translate them to components (actually externalIds) for Black Duck. If the image inspector service\n" +
                     "finds in the target image a package manager database that is incompatible with its own package manager utility\n" +
                     "(this happens when, for example, you run Docker Inspector on an Alpine image, but the request goes to the\n" +
                     "Ubuntu image inspector service), the image inspector service will redirect the request to the appropriate\n" +
                     "image inspector service. You can change the default image inspector service to reduce the likelihood\n" +
                      "of redirects (resulting in shorter execution times). For example, if most of your target images are Alpine\n" +
                      "you can set imageinspector.service.distro.default to alpine.\n\n" +
                     "The image inspector service containers are downloaded from Docker Hub (blackducksoftware/blackduck-imageinspector-*).");
        usage.add("");
        usage.add("Deployment samples for commonly-used environments:");
        usage.add("");
        usage.add("Your deployment approach will be the same whether you are invoking Docker Inspector directly, or invoking it via Detect.\n" +
                     "Most of the sample deployments use Detect simply because that is the most common use case.");
        usage.add("");
        usage.add("Each sample deployment follows one of the two approaches described above, and are labelled accordingly below:\n" +
                     "1. Utility (#1 above) (= Command Line Utility)\n" +
                     "2. Toolkit (#2 above)");
        usage.add("");
        usage.add("The challenges involved in deploying Docker Inspector using the 'toolkit' approach are:\n" +
                     "1. Starting the four containers (one for Detect / Docker Inspector, plus three image inspector containers) such that they all share a common mounted volume\n" +
                     "2. Ensuring that the containers can reach each other via HTTP GET operations using base URLs that your provide.");
        usage.add("");
        usage.add("These deployment samples are intended to show how these challenges could be met. They are not intended to be used as-is in production.\n" +
                      "You should understand the code before you use it. They do not represent the only way to deploy in each environment.\n");
        usage.add("");
        usage.add("Environment\t\t\tApproach\tDeployment Notes\t\t\t\tSample Deployment (curl this URL)");
        usage.add("");
        usage.add("Kubernetes\t\t\tToolkit\t\tSeparate pods; hostPath volume; service URLs\thttps://raw.githubusercontent.com/blackducksoftware/blackduck-docker-inspector/master/deployment/kubernetes/setup.txt");
        usage.add("OpenShift\t\t\tToolkit\t\tSingle pod; emptyDir volume; localhost URLs\thttps://raw.githubusercontent.com/blackducksoftware/blackduck-docker-inspector/master/deployment/openshift/setup.txt");
        usage.add("Travis CI\t\t\tToolkit\t\tdocker service; localhost URLs\t\t\thttps://raw.githubusercontent.com/blackducksoftware/blackduck-docker-inspector/master/deployment/travisci/travis.yml");
        usage.add("GitLab CI\t\t\tToolkit\t\tshell executoer; localhost URLs\t\t\thttps://raw.githubusercontent.com/blackducksoftware/blackduck-docker-inspector/master/deployment/gitlabci/setup.sh");
        usage.add("Circle CI\t\t\tUtility\t\t\t\t\t\t\t\thttps://raw.githubusercontent.com/blackducksoftware/blackduck-docker-inspector/master/deployment/circleci/config.yml");
        usage.add("Docker (Detect in container)\tToolkit\t\tlocalhost URLs\t\t\t\t\thttps://raw.githubusercontent.com/blackducksoftware/blackduck-docker-inspector/master/deployment/docker/runDetectInContainer/setup.sh");
        usage.add("");
        return usage;
    }
}
