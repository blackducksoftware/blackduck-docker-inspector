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
package com.synopsys.integration.blackduck.dockerinspector.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Config {
    public static final String IMAGEINSPECTOR_WS_APPNAME = "blackduck-imageinspector";
    public static final String CONTAINER_BLACKDUCK_DIR = "/opt/blackduck/";
    private static final String INSPECTOR_OS_UBUNTU = "ubuntu";
    private final static String GROUP_PUBLIC = "public";
    private final static String GROUP_PRIVATE = "private";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // Black Duck connection details
    @ValueDescription(description = "Black Duck URL", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${blackduck.url:}")
    private String blackDuckUrl = "";

    @ValueDescription(description = "Black Duck Timeout in seconds", defaultValue = "120", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${blackduck.timeout:120}")
    private Integer blackDuckTimeout = 120;

    @ValueDescription(description = "Black Duck token", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${blackduck.api.token:}")
    private String blackDuckApiToken = "";

    @ValueDescription(description = "Black Duck Username", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${blackduck.username:}")
    private String blackDuckUsername = "";

    @ValueDescription(description = "Black Duck Password", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${blackduck.password:}")
    private String blackDuckPassword = "";

    // The properties in this section must be set if you must connect to the Black Duck through a proxy
    @ValueDescription(description = "Black Duck Proxy Host", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${blackduck.proxy.host:}")
    private String blackDuckProxyHost = "";

    @ValueDescription(description = "Black Duck Proxy Port", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${blackduck.proxy.port:}")
    private String blackDuckProxyPort = "";

    @ValueDescription(description = "Black Duck Proxy Username", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${blackduck.proxy.username:}")
    private String blackDuckProxyUsername = "";

    @ValueDescription(description = "Black Duck Proxy Password", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${blackduck.proxy.password:}")
    private String blackDuckProxyPassword = "";

    // If using an https Black Duck server, you can choose to always trust the server certificates
    @ValueDescription(description = "Black Duck Always Trust Cert?", defaultValue = "false", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${blackduck.always.trust.cert:false}")
    private Boolean blackDuckAlwaysTrustCert = Boolean.FALSE;

    // The default project name will be the Docker image name
    @ValueDescription(description = "Black Duck Project Name", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${blackduck.project.name:}")
    private String blackDuckProjectName = "";

    // The default version name will be Docker image tag
    @ValueDescription(description = "Black Duck Project Version", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${blackduck.project.version:}")
    private String blackDuckProjectVersion = "";

    // Working directory
    @ValueDescription(description = "Working Directory Path", defaultValue = "/tmp/blackduck-docker-inspector-files", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${working.dir.path:/tmp/blackduck-docker-inspector-files}")
    private String workingDirPath = "";

    // Path to additional system properties (an alternative to java -D)
    @ValueDescription(description = "Path to a properties file containing additional system properties (an alternative to java -D)", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${system.properties.path:}")
    private String systemPropertiesPath = "";

    // If false, will leave behind the files created in the working dir
    @ValueDescription(description = "Cleanup Working Dir?", defaultValue = "true", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${cleanup.working.dir:true}")
    private Boolean cleanupWorkingDir = Boolean.TRUE;

    // If Docker Inspector cannot derive it automatically,
    // use linux.distro to specify the target image linux distribution
    // (ubuntu, debian, busybox, centos, fedora, redhat, alpine)
    @ValueDescription(description = "Linux Distribution Name", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${linux.distro:}")
    private String linuxDistro = "";

    // Timeout for external command execution (to pull a docker image, etc.)
    @ValueDescription(description = "Command Timeout (Milliseconds)", defaultValue = "120000", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${command.timeout:120000}")
    private Long commandTimeout = 120000L;

    // Timeout for http requests to image inspector services
    @ValueDescription(description = "HTTP Service Request Timeout (Milliseconds)", defaultValue = "240000", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${service.timeout:240000}")
    private Long serviceTimeout = 240000L;

    // Logging level: ERROR, WARN, INFO, DEBUG, TRACE
    @ValueDescription(description = "Logging Level (WARN, INFO, DEBUG, TRACE)", defaultValue = "INFO", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${logging.level.com.synopsys:INFO}")
    private String loggingLevel = "";

    // Path on host of a directory into which the resulting output files will be copied
    @ValueDescription(description = "Path to directory for output files", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${output.path:}")
    private String outputPath = "";

    // Set to true to include the container file system tarfile in the output
    @ValueDescription(description = "Include container filesystem (a large file) in output?", defaultValue = "false", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${output.include.containerfilesystem:false}")
    private Boolean outputIncludeContainerfilesystem = Boolean.FALSE;

    // If you want to add a prefix to the code location name, specify it here
    @ValueDescription(description = "Black Duck CodeLocation prefix", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${blackduck.codelocation.prefix:}")
    private String blackDuckCodelocationPrefix = "";

    // If you want to set the code location name, specify it here
    @ValueDescription(description = "Black Duck CodeLocation name", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${blackduck.codelocation.name:}")
    private String blackDuckCodelocationName = "";

    // Path to the blackduck-docker-inspector .jar file
    // Only used by blackduck-docker-inspector.sh
    @ValueDescription(description = "Black Duck Docker Inspector .jar file path", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${jar.path:}")
    private String jarPath = "";

    // The following properties should not normally be set/changed by the user
    @ValueDescription(description = "Docker Image name:tag", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${docker.image:}")
    private String dockerImage = "";

    @ValueDescription(description = "Docker tarfile path", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${docker.tar:}")
    private String dockerTar = "";

    @ValueDescription(description = "docker.image.id", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${docker.image.id:}")
    private String dockerImageId = "";

    @ValueDescription(description = "Docker Image Repo; Use with docker.image.tag to select one image from a tarfile", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${docker.image.repo:}")
    private String dockerImageRepo = "";

    @ValueDescription(description = "To ignore components from platform layers: specify the ID (from docker inspect <image:tag>: last of RootFS.Layers) of the top layer of the platform image", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${docker.platform.top.layer.id:}")
    private String dockerPlatformTopLayerId = "";

    @ValueDescription(description = "Docker Image Tag; Use with docker.image.repo to select one image from a tarfile", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${docker.image.tag:}")
    private String dockerImageTag = "";

    @ValueDescription(description = "Caller Name", defaultValue = "", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${caller.name:}")
    private String callerName = "";

    @ValueDescription(description = "caller.version", defaultValue = "", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${caller.version:}")
    private String callerVersion = "";

    @ValueDescription(description = "Phone Home?", defaultValue = "true", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${phone.home:true}")
    private Boolean phoneHome = Boolean.TRUE;

    @ValueDescription(description = "Upload BDIO?", defaultValue = "true", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${upload.bdio:true}")
    private Boolean uploadBdio = Boolean.TRUE;

    @ValueDescription(description = "Repository name for the Docker Inspector images", defaultValue = "blackducksoftware", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${inspector.repository:blackducksoftware}")
    private String inspectorRepository = "blackducksoftware";

    @ValueDescription(description = "Docker Inspector image \"family\"", defaultValue = "", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${inspector.image.family:}")
    private String inspectorImageFamily = "";

    @ValueDescription(description = "Docker Inspector image version", defaultValue = "", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${inspector.image.version:}")
    private String inspectorImageVersion = "";

    @ValueDescription(description = "Remove target image after saving it?", defaultValue = "false", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${cleanup.target.image:false}")
    private Boolean cleanupTargetImage = Boolean.FALSE;

    @ValueDescription(description = "Stop inspector container after using it?", defaultValue = "true", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${cleanup.inspector.container:true}")
    private Boolean cleanupInspectorContainer = Boolean.TRUE;

    @ValueDescription(description = "Remove inspector image after using it?", defaultValue = "false", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${cleanup.inspector.image:false}")
    private Boolean cleanupInspectorImage = Boolean.FALSE;

    @ValueDescription(description = "In generated BDIO, organize components by layer?", defaultValue = "false", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${bdio.organize.components.by.layer:false}")
    private Boolean organizeComponentsByLayer = Boolean.FALSE;

    @ValueDescription(description = "In generated BDIO, include removed components?", defaultValue = "false", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${bdio.include.removed.components:false}")
    private Boolean includeRemovedComponents = Boolean.FALSE;

    @ValueDescription(description = "The host's path to the dir shared with the imageinspector containers. Only needed if using existing imageinspector containers", defaultValue = "/tmp/blackduck-docker-inspector-files/shared", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${shared.dir.path.local:/tmp/blackduck-docker-inspector-files/shared}")
    private String sharedDirPathLocal = "/tmp/blackduck-docker-inspector-files/shared";

    @ValueDescription(description = "The container's path to the shared directory. Only needed if using existing imageinspector containers", defaultValue = "/opt/blackduck/blackduck-imageinspector/shared", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${shared.dir.path.imageinspector:/opt/blackduck/blackduck-imageinspector/shared}")
    private String sharedDirPathImageInspector = "/opt/blackduck/blackduck-imageinspector/shared";

    @ValueDescription(description = "The URL of the (already running) imageinspector service to use", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${imageinspector.service.url:}")
    private String imageInspectorUrl = "";

    // Properties for pull/start services/containers as needed mode:

    @ValueDescription(description = "Start ImageInspector services (containers) as needed?", defaultValue = "true", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${imageinspector.service.start:true}")
    private Boolean imageInspectorServiceStart = Boolean.TRUE;

    @ValueDescription(description = "alpine image inspector container port", defaultValue = "8080", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${imageinspector.service.container.port.alpine:8080}")
    private String imageInspectorContainerPortAlpine = "8080";

    @ValueDescription(description = "centos image inspector container port", defaultValue = "8081", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${imageinspector.service.container.port.centos:8081}")
    private String imageInspectorContainerPortCentos = "8081";

    @ValueDescription(description = "ubuntu image inspector container port", defaultValue = "8082", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${imageinspector.service.container.port.ubuntu:8082}")
    private String imageInspectorContainerPortUbuntu = "8082";

    @ValueDescription(description = "alpine image inspector host port", defaultValue = "9000", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${imageinspector.service.port.alpine:9000}")
    private String imageInspectorHostPortAlpine = "9000";

    @ValueDescription(description = "centos image inspector host port", defaultValue = "9001", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${imageinspector.service.port.centos:9001}")
    private String imageInspectorHostPortCentos = "9001";

    @ValueDescription(description = "ubuntu image inspector host port", defaultValue = "9002", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${imageinspector.service.port.ubuntu:9002}")
    private String imageInspectorHostPortUbuntu = "9002";

    // In "start containers" mode, default is specified by distro; in "use existing", it's specified by URL
    @ValueDescription(description = "Default image inspector Linux distro (alpine, centos, or ubuntu)", defaultValue = "ubuntu", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${imageinspector.service.distro.default:ubuntu}")
    private String imageInspectorDefaultDistro = INSPECTOR_OS_UBUNTU;

    @ValueDescription(description = "Make no attempts to access network-based resources (the Black Duck server, docker repository)", defaultValue = "false", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${offline.mode:false}")
    private Boolean offlineMode = Boolean.FALSE;

    // Environment Variables
    @Value("${BD_HUB_PASSWORD:}")
    private String blackDuckLegacyPasswordEnvVar = "";

    @Value("${BD_HUB_TOKEN:}")
    private String blackDuckLegacyApiTokenEnvVar = "";

    @Value("${BD_PASSWORD:}")
    private String blackDuckPasswordEnvVar = "";

    @Value("${BD_TOKEN:}")
    private String blackDuckApiTokenEnvVar = "";

    @Value("${SCAN_CLI_OPTS:}")
    private String scanCliOptsEnvVar = "";

    @Value("${DOCKER_INSPECTOR_JAVA_OPTS:}")
    private String dockerInspectorJavaOptsValue = "";

    private TreeSet<DockerInspectorOption> publicOptions;
    private Map<String, DockerInspectorOption> optionsByKey;
    private Map<String, DockerInspectorOption> optionsByFieldName;
    private TreeSet<String> allKeys;

    public String get(final String key) throws IllegalArgumentException, IllegalAccessException {
        final DockerInspectorOption opt = optionsByKey.get(key);
        if (opt == null) {
            return null;
        }
        return opt.getResolvedValue();
    }

    public SortedSet<DockerInspectorOption> getPublicConfigOptions() throws IllegalArgumentException, IllegalAccessException {
        return publicOptions;
    }

    @PostConstruct
    public void init() throws IllegalArgumentException, IllegalAccessException {
        final Object configObject = this;
        publicOptions = new TreeSet<>();
        allKeys = new TreeSet<>();
        optionsByKey = new HashMap<>();
        optionsByFieldName = new HashMap<>();
        for (final Field field : configObject.getClass().getDeclaredFields()) {
            final Annotation[] declaredAnnotations = field.getDeclaredAnnotations();
            if (declaredAnnotations.length > 0) {
                for (final Annotation annotation : declaredAnnotations) {
                    if (annotation.annotationType().getName().equals(ValueDescription.class.getName())) {
                        logger.trace(String.format("ValueDescription annotated config object field: %s", field.getName()));
                        final String propMappingString = field.getAnnotation(Value.class).value();
                        final String propName = SpringValueUtils.springKeyFromValueAnnotation(propMappingString);
                        final Object fieldValueObject = field.get(configObject);
                        if (fieldValueObject == null) {
                            logger.warn(String.format("propName %s field is null", propName));
                            continue;
                        }
                        final String value = fieldValueObject.toString();
                        logger.trace(String.format("adding prop key %s [value: %s]", propName, value));
                        allKeys.add(propName);
                        final ValueDescription valueDescription = field.getAnnotation(ValueDescription.class);
                        final DockerInspectorOption opt = new DockerInspectorOption(propName, value, valueDescription.description(), field.getType(), valueDescription.defaultValue(), valueDescription.group(),
                                valueDescription.deprecated());
                        optionsByKey.put(propName, opt);
                        logger.trace(String.format("adding field name %s to optionsByFieldName", field.getName()));
                        optionsByFieldName.put(field.getName(), opt);
                        if (!Config.GROUP_PRIVATE.equals(valueDescription.group())) {
                            publicOptions.add(opt);
                        } else {
                            logger.trace(String.format("private prop: propName: %s, fieldName: %s, group: %s, description: %s", propName, field.getName(), valueDescription.group(), valueDescription.description()));
                        }
                    }
                }
            }
        }
    }

    public String getBlackDuckUrl() {
        return optionsByFieldName.get("blackDuckUrl").getResolvedValue();
    }

    public Integer getBlackDuckTimeout() {
        return new Integer(optionsByFieldName.get("blackDuckTimeout").getResolvedValue());
    }

    public String getBlackDuckApiToken() {
        return optionsByFieldName.get("blackDuckApiToken").getResolvedValue();
    }

    public String getBlackDuckUsername() {
        return unEscape(optionsByFieldName.get("blackDuckUsername").getResolvedValue());
    }

    public String getBlackDuckPassword() {
        return optionsByFieldName.get("blackDuckPassword").getResolvedValue();
    }

    public String getBlackDuckProxyHost() {
        return optionsByFieldName.get("blackDuckProxyHost").getResolvedValue();
    }

    public String getBlackDuckProxyPort() {
        return optionsByFieldName.get("blackDuckProxyPort").getResolvedValue();
    }

    public String getBlackDuckProxyUsername() {
        return optionsByFieldName.get("blackDuckProxyUsername").getResolvedValue();
    }

    public String getBlackDuckProxyPassword() {
        return optionsByFieldName.get("blackDuckProxyPassword").getResolvedValue();
    }

    public boolean isBlackDuckAlwaysTrustCert() {
        return optionsByFieldName.get("blackDuckAlwaysTrustCert").getResolvedValue().equals("true");
    }

    public String getBlackDuckProjectName() {
        return unEscape(optionsByFieldName.get("blackDuckProjectName").getResolvedValue());
    }

    public String getBlackDuckProjectVersion() {
        return unEscape(optionsByFieldName.get("blackDuckProjectVersion").getResolvedValue());
    }

    public String getWorkingDirPath() {
        if (StringUtils.isNotBlank(getImageInspectorUrl()) || isImageInspectorServiceStart()) {
            return optionsByFieldName.get("sharedDirPathLocal").getResolvedValue();
        }
        return optionsByFieldName.get("workingDirPath").getResolvedValue();
    }

    public String getSystemPropertiesPath() {
        return optionsByFieldName.get("systemPropertiesPath").getResolvedValue();
    }

    public boolean isCleanupWorkingDir() {
        return optionsByFieldName.get("cleanupWorkingDir").getResolvedValue().equals("true");
    }

    public Long getCommandTimeout() {
        return new Long(optionsByFieldName.get("commandTimeout").getResolvedValue());
    }

    public Long getServiceTimeout() {
        return new Long(optionsByFieldName.get("serviceTimeout").getResolvedValue());
    }

    public String getOutputPath() {
        return optionsByFieldName.get("outputPath").getResolvedValue();
    }

    public boolean isOutputIncludeContainerfilesystem() {
        return optionsByFieldName.get("outputIncludeContainerfilesystem").getResolvedValue().equals("true");
    }

    public String getBlackDuckCodelocationPrefix() {
        return optionsByFieldName.get("blackDuckCodelocationPrefix").getResolvedValue();
    }

    public String getBlackDuckCodelocationName() {
        return optionsByFieldName.get("blackDuckCodelocationName").getResolvedValue();
    }

    public String getDockerImage() {
        return optionsByFieldName.get("dockerImage").getResolvedValue();
    }

    public String getDockerTar() {
        return unEscape(optionsByFieldName.get("dockerTar").getResolvedValue());
    }

    public String getDockerImageId() {
        return optionsByFieldName.get("dockerImageId").getResolvedValue();
    }

    public String getDockerImageRepo() {
        return optionsByFieldName.get("dockerImageRepo").getResolvedValue();
    }

    public String getDockerPlatformTopLayerId() {
        return optionsByFieldName.get("dockerPlatformTopLayerId").getResolvedValue();
    }

    public String getDockerImageTag() {
        return optionsByFieldName.get("dockerImageTag").getResolvedValue();
    }

    public String getCallerName() {
        return optionsByFieldName.get("callerName").getResolvedValue();
    }

    public String getInspectorRepository() {
        return optionsByFieldName.get("inspectorRepository").getResolvedValue();
    }

    public String getInspectorImageFamily() {
        return optionsByFieldName.get("inspectorImageFamily").getResolvedValue();
    }

    public String getInspectorImageVersion() {
        return optionsByFieldName.get("inspectorImageVersion").getResolvedValue();
    }

    public String getSharedDirPathImageInspector() {
        return optionsByFieldName.get("sharedDirPathImageInspector").getResolvedValue();
    }

    public String getSharedDirPathLocal() {
        return optionsByFieldName.get("sharedDirPathLocal").getResolvedValue();
    }

    public String getImageInspectorUrl() {
        return optionsByFieldName.get("imageInspectorUrl").getResolvedValue();
    }

    public Integer getImageInspectorContainerPortAlpine() {
        return new Integer(optionsByFieldName.get("imageInspectorContainerPortAlpine").getResolvedValue());
    }

    public Integer getImageInspectorContainerPortCentos() {
        return new Integer(optionsByFieldName.get("imageInspectorContainerPortCentos").getResolvedValue());
    }

    public Integer getImageInspectorContainerPortUbuntu() {
        return new Integer(optionsByFieldName.get("imageInspectorContainerPortUbuntu").getResolvedValue());
    }

    public Integer getImageInspectorHostPortAlpine() {
        return new Integer(optionsByFieldName.get("imageInspectorHostPortAlpine").getResolvedValue());
    }

    public Integer getImageInspectorHostPortCentos() {
        return new Integer(optionsByFieldName.get("imageInspectorHostPortCentos").getResolvedValue());
    }

    public Integer getImageInspectorHostPortUbuntu() {
        return new Integer(optionsByFieldName.get("imageInspectorHostPortUbuntu").getResolvedValue());
    }

    public String getImageInspectorDefaultDistro() {
        return optionsByFieldName.get("imageInspectorDefaultDistro").getResolvedValue();
    }

    public String getCallerVersion() {
        return optionsByFieldName.get("callerVersion").getResolvedValue();
    }

    public boolean isPhoneHome() {
        return optionsByFieldName.get("phoneHome").getResolvedValue().equals("true");
    }

    public String getScanCliOptsEnvVar() {
        return scanCliOptsEnvVar;
    }

    public String getBlackDuckPasswordEnvVar() {
        if (StringUtils.isBlank(blackDuckPasswordEnvVar)) {
            return blackDuckLegacyPasswordEnvVar;
        }
        return blackDuckPasswordEnvVar;
    }

    public String getBlackDuckApiTokenEnvVar() {
        if (StringUtils.isBlank(blackDuckApiTokenEnvVar)) {
            return blackDuckLegacyApiTokenEnvVar;
        }
        return blackDuckApiTokenEnvVar;
    }

    public boolean isUploadBdio() {
        return optionsByFieldName.get("uploadBdio").getResolvedValue().equals("true");
    }

    public boolean isCleanupTargetImage() {
        return optionsByFieldName.get("cleanupTargetImage").getResolvedValue().equals("true");
    }

    public boolean isCleanupInspectorContainer() {
        return optionsByFieldName.get("cleanupInspectorContainer").getResolvedValue().equals("true");
    }

    public boolean isCleanupInspectorImage() {
        return optionsByFieldName.get("cleanupInspectorImage").getResolvedValue().equals("true");
    }

    public boolean isOrganizeComponentsByLayer() {
        return optionsByFieldName.get("organizeComponentsByLayer").getResolvedValue().equals("true");
    }

    public boolean isIncludeRemovedComponents() {
        return optionsByFieldName.get("includeRemovedComponents").getResolvedValue().equals("true");
    }

    public boolean isImageInspectorServiceStart() {
        return optionsByFieldName.get("imageInspectorServiceStart").getResolvedValue().equals("true");
    }

    public boolean isOfflineMode() {
        return optionsByFieldName.get("offlineMode").getResolvedValue().equals("true");
    }

    public void setDockerImageRepo(final String newValue) {
        optionsByFieldName.get("dockerImageRepo").setResolvedValue(newValue);
    }

    public void setDockerImageTag(final String newValue) {
        optionsByFieldName.get("dockerImageTag").setResolvedValue(newValue);
    }

    private String unEscape(final String origString) {
        logger.trace(String.format("origString: %s", origString));
        final String unEscapedString = origString.replaceAll("%20", " ");
        logger.trace(String.format("unEscapedString: %s", unEscapedString));
        return unEscapedString;
    }

    // This is here to prevent eclipse from making config property members final
    protected void preventFinal() {
        this.callerName = null;
        this.callerVersion = null;
        this.cleanupWorkingDir = null;
        this.commandTimeout = null;
        this.serviceTimeout = null;
        this.dockerImage = null;
        this.dockerImageId = null;
        this.dockerImageRepo = null;
        this.dockerPlatformTopLayerId = null;
        this.dockerImageTag = null;
        this.dockerInspectorJavaOptsValue = null;
        this.dockerTar = null;
        this.blackDuckAlwaysTrustCert = null;
        this.blackDuckCodelocationPrefix = null;
        this.blackDuckCodelocationName = null;
        this.blackDuckPassword = null;
        this.blackDuckPasswordEnvVar = null;
        this.blackDuckApiTokenEnvVar = null;
        this.blackDuckLegacyApiTokenEnvVar = null;
        this.blackDuckLegacyPasswordEnvVar = null;
        this.blackDuckProjectName = null;
        this.blackDuckProjectVersion = null;
        this.blackDuckProxyHost = null;
        this.blackDuckProxyPassword = null;
        this.blackDuckProxyPort = null;
        this.blackDuckProxyUsername = null;
        this.blackDuckTimeout = null;
        this.blackDuckUrl = null;
        this.blackDuckUsername = null;
        this.blackDuckApiToken = null;
        this.jarPath = null;
        this.linuxDistro = null;
        this.loggingLevel = null;
        this.outputIncludeContainerfilesystem = null;
        this.outputPath = null;
        this.phoneHome = null;
        this.scanCliOptsEnvVar = null;
        this.workingDirPath = null;
        this.systemPropertiesPath = null;
        this.uploadBdio = null;
        this.inspectorRepository = null;
        this.cleanupInspectorContainer = null;
        this.cleanupInspectorImage = null;
        this.organizeComponentsByLayer = null;
        this.includeRemovedComponents = null;
        this.cleanupTargetImage = null;
        this.inspectorImageFamily = null;
        this.inspectorImageVersion = null;
        this.sharedDirPathImageInspector = null;
        this.sharedDirPathLocal = null;
        this.imageInspectorUrl = null;
        this.imageInspectorServiceStart = null;
        this.imageInspectorContainerPortAlpine = null;
        this.imageInspectorContainerPortCentos = null;
        this.imageInspectorContainerPortUbuntu = null;
        this.imageInspectorHostPortAlpine = null;
        this.imageInspectorHostPortCentos = null;
        this.imageInspectorHostPortUbuntu = null;
        this.imageInspectorDefaultDistro = null;
        this.offlineMode = null;
    }
}
