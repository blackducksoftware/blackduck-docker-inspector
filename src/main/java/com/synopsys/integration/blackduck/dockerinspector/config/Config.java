/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.config;

import java.io.File;
import java.io.IOException;
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
    private static final String GROUP_PUBLIC = "public";
    private static final String GROUP_PRIVATE = "private";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // The default project name will be the Docker image name
    @ValueDescription(description = "BDIO Project Name", defaultValue = "", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${bdio.project.name:}")
    private String bdioProjectName = "";

    // The default version name will be Docker image tag
    @ValueDescription(description = "BDIO Project Version", defaultValue = "", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${bdio.project.version:}")
    private String bdioProjectVersion = "";

    // If you want to add a prefix to the code location name, specify it here
    @ValueDescription(description = "BDIO CodeLocation prefix", defaultValue = "", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${bdio.codelocation.prefix:}")
    private String bdioCodelocationPrefix = "";

    // If you want to set the code location name, specify it here
    @ValueDescription(description = "BDIO CodeLocation name", defaultValue = "", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${bdio.codelocation.name:}")
    private String bdioCodelocationName = "";

    // Working directory
    @ValueDescription(description = "Working Directory Path. If not set, a default of $HOME/blackduck/docker-inspector will be used.", defaultValue = "", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${working.dir.path:}")
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
    @ValueDescription(description = "Target image Linux distribution name. Use this if you need to override the Linux distribution name discovered by Docker Inspector.", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${linux.distro:}")
    private String targetImageLinuxDistroOverride = "";

    // Timeout for external command execution (to pull a docker image, etc.)
    @ValueDescription(description = "Command Timeout (Milliseconds)", defaultValue = "120000", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${command.timeout:120000}")
    private Long commandTimeout = 120000L;

    // Timeout for http requests to image inspector services
    @ValueDescription(description = "HTTP Service Request Timeout (Milliseconds)", defaultValue = "600000", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${service.timeout:600000}")
    private Long serviceTimeout = 600000L;

    // Logging level: ERROR, WARN, INFO, DEBUG, TRACE
    @ValueDescription(description = "Logging Level (WARN, INFO, DEBUG, TRACE)", defaultValue = "INFO", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${logging.level.com.synopsys:INFO}")
    private String loggingLevel = "";

    // Path on host of a directory into which the resulting output files will be copied
    @ValueDescription(description = "Path to directory for output files", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${output.path:}")
    private String outputPath = "";

    // Set to true to include the container file system tarfile (compressed) in the output
    @ValueDescription(description = "Include container filesystem (a large file) in output?", defaultValue = "false", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${output.include.containerfilesystem:false}")
    private Boolean outputIncludeContainerfilesystem = Boolean.FALSE;

    // Set to true to include the squashed image tarfile (compressed) in the output
    @ValueDescription(description = "Include container filesystem (a large file) in output?", defaultValue = "false", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${output.include.squashedimage:false}")
    private Boolean outputIncludeSquashedImage = Boolean.FALSE;

    // If you want dirs/files/links omitted from the container filesystem, specify the list of absolute paths here (e.g. /etc)
    @ValueDescription(description = "Comma-separated list of directories/files/links (specified as absolute paths) to exclude from the container filesystem", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${output.containerfilesystem.excluded.paths:}")
    private String containerFileSystemExcludedPaths = "";

    @ValueDescription(description = "Use platform's default DOCKER_HOST value? Set to false if you want to override DOCKER_HOST", defaultValue = "true", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${use.platform.default.docker.host:true}")
    private Boolean usePlatformDefaultDockerHost = Boolean.TRUE;

    // The following properties should not normally be set/changed by the user
    @ValueDescription(description = "Docker Image name:tag", defaultValue = "", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${docker.image:}")
    private String dockerImage = "";

    @ValueDescription(description = "The platform (shown as 'platform' field in 'docker manifest inspect {image}' output) of the target image Docker Inspector should pull. You must also provide the target image (via docker.image) when using this property.  Note: when providing a platform, you may provide either the target operating system (os), the target architecture, or os/architecture.", group = Config.GROUP_PUBLIC)
    @Value("${docker.image.platform:}")
    private String dockerImagePlatform = "";

    @ValueDescription(description = "Docker or OCI image tarfile path", defaultValue = "", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${docker.tar:}")
    private String dockerTar = "";

    @ValueDescription(description = "The ID (shown in the 'IMAGE ID' column of 'docker images' output) of the target Docker image. The target image must already be local (must appear in the output of 'docker images').", defaultValue = "", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${docker.image.id:}")
    private String dockerImageId = "";

    @ValueDescription(description = "Docker Image Repo; Use with docker.image.tag to select one image from a tarfile", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${docker.image.repo:}")
    private String dockerImageRepo = "";

    @ValueDescription(description = "To ignore components from platform layers: specify the ID (from docker inspect <image:tag>: last of RootFS.Layers) of the top layer of the platform image", defaultValue = "", group = Config.GROUP_PRIVATE, deprecated = false)
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

    @ValueDescription(description = "In generated BDIO, organize components by layer?", defaultValue = "false", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${bdio.organize.components.by.layer:false}")
    private Boolean organizeComponentsByLayer = Boolean.FALSE;

    @ValueDescription(description = "In generated BDIO, include removed components? " +
        "If false, only components present in the final container filesystem (in other words, present after the final layer is applied) will be included in the output. " +
        "If true, a component added by any layer will be included in the output even if later removed by a higher layer.",
        defaultValue = "false", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${bdio.include.removed.components:false}")
    private Boolean includeRemovedComponents = Boolean.FALSE;

    @ValueDescription(description = "The host's path to the dir shared with the imageinspector containers. Only needed if using existing imageinspector containers. If not set, $HOME/blackduck/docker-inspector/shared will be used", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${shared.dir.path.local:}")
    private String sharedDirPathLocal = "";

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

    @ValueDescription(description = "The number of lines of the image inspector service log to include in the Docker Inspector log when log level is DEBUG or higher", defaultValue = "10000", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${imageinspector.service.log.length:10000}")
    private Integer imageInspectorServiceLogLength = 10000;

    @ValueDescription(description = "Make no attempts to access network-based resources (the docker repository)", defaultValue = "false", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${offline.mode:false}")
    private Boolean offlineMode = Boolean.FALSE;

    @ValueDescription(description = "The path to a file or directory to which help output will be written in markdown format. " +
        "If not set, help will be written to stdout. If set, the directory must exist; the file will be created if it does not exist. " +
        "If the path to a directory is provided, Docker Inspector will generate the filename automatically",
        defaultValue = "", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${help.output.path:}")
    private String helpOutputFilePath = "";

    @ValueDescription(description = "The path to the directory that contains help source files in Markdown format (with .md extensions)",
        defaultValue = "", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${help.input.path:}")
    private String helpInputFilePath = "";

    private TreeSet<DockerInspectorOption> publicOptions;
    private Map<String, DockerInspectorOption> optionsByKey;
    private Map<String, DockerInspectorOption> optionsByFieldName;
    private TreeSet<String> allKeys;

    public String get(String key) {
        DockerInspectorOption opt = optionsByKey.get(key);
        if (opt == null) {
            return null;
        }
        return opt.getResolvedValue();
    }

    public SortedSet<DockerInspectorOption> getPublicConfigOptions() {
        return publicOptions;
    }

    @PostConstruct
    public void init() throws IllegalAccessException {
        publicOptions = new TreeSet<>();
        allKeys = new TreeSet<>();
        optionsByKey = new HashMap<>();
        optionsByFieldName = new HashMap<>();
        for (Field field : this.getClass().getDeclaredFields()) {
            Annotation[] declaredAnnotations = field.getDeclaredAnnotations();
            if (declaredAnnotations.length > 0) {
                for (Annotation annotation : declaredAnnotations) {
                    if (annotation instanceof ValueDescription) {
                        recordOption(field);
                    }
                }
            }
        }
    }

    private void recordOption(Field field) throws IllegalAccessException {
        logger.trace(String.format("ValueDescription annotated config object field: %s", field.getName()));
        String propMappingString = field.getAnnotation(Value.class).value();
        String propName = SpringValueUtils.springKeyFromValueAnnotation(propMappingString);
        Object fieldValueObject = field.get(this);
        if (fieldValueObject == null) {
            logger.warn(String.format("propName %s field is null", propName));
            return;
        }
        String value = fieldValueObject.toString();
        logger.trace(String.format("adding prop key %s [value: %s]", propName, value));
        allKeys.add(propName);
        ValueDescription valueDescription = field.getAnnotation(ValueDescription.class);
        DockerInspectorOption opt = new DockerInspectorOption(propName, value, valueDescription.description(), field.getType(), valueDescription.defaultValue(),
            valueDescription.deprecated(), valueDescription.deprecationMessage()
        );
        optionsByKey.put(propName, opt);
        logger.trace(String.format("adding field name %s to optionsByFieldName", field.getName()));
        optionsByFieldName.put(field.getName(), opt);
        if (!Config.GROUP_PRIVATE.equals(valueDescription.group())) {
            publicOptions.add(opt);
        } else {
            logger.trace(String.format("private prop: propName: %s, fieldName: %s, group: %s, description: %s", propName, field.getName(), valueDescription.group(), valueDescription.description()));
        }
    }

    public Integer getImageInspectorServiceLogLength() {
        return new Integer(optionsByFieldName.get("imageInspectorServiceLogLength").getResolvedValue());
    }

    public String getBdioProjectName() {
        return unEscape(optionsByFieldName.get("bdioProjectName").getResolvedValue());
    }

    public String getBdioProjectVersion() {
        return unEscape(optionsByFieldName.get("bdioProjectVersion").getResolvedValue());
    }

    public String getBdioCodelocationPrefix() {
        return optionsByFieldName.get("bdioCodelocationPrefix").getResolvedValue();
    }

    public String getBdioCodelocationName() {
        return optionsByFieldName.get("bdioCodelocationName").getResolvedValue();
    }

    public String getSharedDirPathLocal() throws IOException {
        String givenSharedDirPathLocal = optionsByFieldName.get("sharedDirPathLocal").getResolvedValue();
        if (StringUtils.isNotBlank(givenSharedDirPathLocal)) {
            File sharedDirLocal = new File(givenSharedDirPathLocal);
            return sharedDirLocal.getCanonicalPath();
        }
        File workingDir = deriveWorkingDir();
        File sharedDirLocal = new File(workingDir, "shared");
        return sharedDirLocal.getCanonicalPath();
    }

    public String getWorkingDirPath() throws IOException {
        if (StringUtils.isNotBlank(getImageInspectorUrl()) || isImageInspectorServiceStart()) {
            return getSharedDirPathLocal();
        }
        return deriveWorkingDir().getCanonicalPath();
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

    public boolean isOutputIncludeSquashedImage() {
        return optionsByFieldName.get("outputIncludeSquashedImage").getResolvedValue().equals("true");
    }

    public boolean isUsePlatformDefaultDockerHost() {
        return optionsByFieldName.get("usePlatformDefaultDockerHost").getResolvedValue().equals("true");
    }

    public String getContainerFileSystemExcludedPaths() {
        return optionsByFieldName.get("containerFileSystemExcludedPaths").getResolvedValue();
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

    public String getDockerImagePlatform() {return optionsByFieldName.get("dockerImagePlatform").getResolvedValue();}

    public String getDockerImageRepo() {
        return optionsByFieldName.get("dockerImageRepo").getResolvedValue();
    }

    public String getDockerPlatformTopLayerId() {
        return optionsByFieldName.get("dockerPlatformTopLayerId").getResolvedValue();
    }

    public String getTargetImageLinuxDistroOverride() {
        return optionsByFieldName.get("targetImageLinuxDistroOverride").getResolvedValue();
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

    public String getHelpOutputFilePath() {
        return optionsByFieldName.get("helpOutputFilePath").getResolvedValue();
    }

    public String getHelpInputFilePath() {
        return optionsByFieldName.get("helpInputFilePath").getResolvedValue();
    }

    public String getCallerVersion() {
        return optionsByFieldName.get("callerVersion").getResolvedValue();
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

    public void setDockerImageRepo(String newValue) {
        optionsByFieldName.get("dockerImageRepo").setResolvedValue(newValue);
    }

    public void setDockerImageTag(String newValue) {
        optionsByFieldName.get("dockerImageTag").setResolvedValue(newValue);
    }

    private String unEscape(String origString) {
        logger.trace(String.format("origString: %s", origString));
        String unEscapedString = origString.replace("%20", " ");
        logger.trace(String.format("unEscapedString: %s", unEscapedString));
        return unEscapedString;
    }

    private File deriveWorkingDir() {
        File workingDir;
        String givenWorkingDirPath = optionsByFieldName.get("workingDirPath").getResolvedValue();
        if (StringUtils.isNotBlank(givenWorkingDirPath)) {
            workingDir = new File(givenWorkingDirPath);
        } else {
            workingDir = deriveDefaultWorkingDir();
        }
        return workingDir;
    }

    private File deriveDefaultWorkingDir() {
        String userHomePath = System.getProperty("user.home");
        File userHomeDir = new File(userHomePath);
        File blackDuckDir = new File(userHomeDir, "blackduck");
        return new File(blackDuckDir, "docker-inspector");
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
        this.dockerImagePlatform = null;
        this.dockerImageRepo = null;
        this.dockerPlatformTopLayerId = null;
        this.dockerImageTag = null;
        this.dockerTar = null;
        this.bdioProjectName = null;
        this.bdioProjectVersion = null;
        this.bdioCodelocationPrefix = null;
        this.bdioCodelocationName = null;
        this.imageInspectorServiceLogLength = null;
        this.targetImageLinuxDistroOverride = null;
        this.loggingLevel = null;
        this.outputIncludeContainerfilesystem = null;
        this.outputIncludeSquashedImage = null;
        this.usePlatformDefaultDockerHost = null;
        this.containerFileSystemExcludedPaths = null;
        this.outputPath = null;
        this.workingDirPath = null;
        this.systemPropertiesPath = null;
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
        this.helpOutputFilePath = null;
        this.helpInputFilePath = null;
        this.offlineMode = null;
    }
}
