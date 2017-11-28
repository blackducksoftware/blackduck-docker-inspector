package com.blackducksoftware.integration.hub.docker.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.docker.help.ValueDescription;

@Component
public class Config {
    private final static String GROUP_PUBLIC = "public";
    private final static String GROUP_PRIVATE = "private";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    ConfigurableEnvironment configurableEnvironment;

    // Black Duck Hub connection details
    @ValueDescription(description = "Hub URL", defaultValue = "", group = Config.GROUP_PUBLIC)
    @Value("${hub.url:}")
    private String hubUrl;

    @ValueDescription(description = "Hub Timeout in seconds", defaultValue = "120", group = Config.GROUP_PUBLIC)
    @Value("${hub.timeout:120}")
    private String hubTimeout;

    @ValueDescription(description = "Hub Username", defaultValue = "", group = Config.GROUP_PUBLIC)
    @Value("${hub.username:}")
    private String hubUsername;

    @ValueDescription(description = "Hub Password", defaultValue = "", group = Config.GROUP_PUBLIC)
    @Value("${hub.password:}")
    private String hubPassword;

    // The properties in this section must be set if you must connect to the Hub through a proxy
    @ValueDescription(description = "Hub Proxy Host", defaultValue = "", group = Config.GROUP_PUBLIC)
    @Value("${hub.proxy.host:}")
    private String hubProxyHost;

    @ValueDescription(description = "Hub Proxy Port", defaultValue = "", group = Config.GROUP_PUBLIC)
    @Value("${hub.proxy.port:}")
    private String hubProxyPort;

    @ValueDescription(description = "Hub Proxy Username", defaultValue = "", group = Config.GROUP_PUBLIC)
    @Value("${hub.proxy.username:}")
    private String hubProxyUsername;

    @ValueDescription(description = "Hub Proxy Password", defaultValue = "", group = Config.GROUP_PUBLIC)
    @Value("${hub.proxy.password:}")
    private String hubProxyPassword;

    // If using an https Hub server, you can choose to always trust the server certificates
    @ValueDescription(description = "Hub Always Trust Cert?", defaultValue = "false", group = Config.GROUP_PUBLIC)
    @Value("${hub.always.trust.cert:false}")
    private final Boolean hubAlwaysTrustCert = new Boolean(false); // don't know why init is required on just this one

    // The default project name will be the Docker image name
    @ValueDescription(description = "Hub Project Name", defaultValue = "", group = Config.GROUP_PUBLIC)
    @Value("${hub.project.name:}")
    private String hubProjectName;

    // The default version name will be Docker image tag
    @ValueDescription(description = "Hub Project Version", defaultValue = "", group = Config.GROUP_PUBLIC)
    @Value("${hub.project.version:}")
    private String hubProjectVersion;

    // Working directory
    @ValueDescription(description = "Working Directory Path", defaultValue = "/tmp/hub-docker-inspector-files", group = Config.GROUP_PUBLIC)
    @Value("${working.dir.path:/tmp/hub-docker-inspector-files}")
    private String workingDirPath;

    // If false, will leave behind the files created in the working dir
    @ValueDescription(description = "Cleanup Working Dir?", defaultValue = "true", group = Config.GROUP_PUBLIC)
    @Value("${cleanup.working.dir:true}")
    private Boolean cleanupWorkingDir;

    // If Hub Docker Inspector cannot derive it automatically,
    // use linux.distro to specify the target image linux distribution
    // (ubuntu, debian, busybox, centos, fedora, redhat, alpine)
    @ValueDescription(description = "Linux Distribution Name", defaultValue = "", group = Config.GROUP_PUBLIC)
    @Value("${linux.distro:}")
    private String linuxDistro;

    // Timeout for external command execution (to pull a docker image, etc.)
    @ValueDescription(description = "Command Timeout (Milliseconds)", defaultValue = "120000", group = Config.GROUP_PUBLIC)
    @Value("${command.timeout:120000}")
    private String commandTimeout;

    // Logging level: ERROR, WARN, INFO, DEBUG, TRACE
    // TODO what about this logging level??
    @ValueDescription(description = "Logging Level (WARN, INFO, DEBUG, TRACE)", defaultValue = "INFO", group = Config.GROUP_PUBLIC)
    @Value("${logging.level.com.blackducksoftware:INFO}")
    private String loggingLevel;

    // If dry.run=true, Hub Docker Inspector won't upload results to Hub
    @ValueDescription(description = "Dry Run Mode?", defaultValue = "false", group = Config.GROUP_PUBLIC)
    @Value("${dry.run:false}")
    private Boolean dryRun;

    // Path on host of a directory into which the resulting output files will be copied
    @ValueDescription(description = "Path to directory for output files", defaultValue = "", group = Config.GROUP_PUBLIC)
    @Value("${output.path:}")
    private String outputPath;

    // Set to true to include the image tarfile in the output
    @ValueDescription(description = "Include Docker tarfile in output?", defaultValue = "false", group = Config.GROUP_PUBLIC)
    @Value("${output.include.dockertarfile:false}")
    private Boolean outputIncludeDockertarfile;

    // Set to true to include the container file system tarfile in the output
    @ValueDescription(description = "Include container filesystem (a large file) in output?", defaultValue = "false", group = Config.GROUP_PUBLIC)
    @Value("${output.include.containerfilesystem:false}")
    private Boolean outputIncludeContainerfilesystem;

    // If you want to add a prefix to the code location name, specify it here
    @ValueDescription(description = "Hub CodeLocation prefix", defaultValue = "", group = Config.GROUP_PUBLIC)
    @Value("${hub.codelocation.prefix:}")
    private String hubCodelocationPrefix;

    // Path to the hub-docker-inspector .jar file
    @ValueDescription(description = "Hub Docker Inspector .jar file path", defaultValue = "", group = Config.GROUP_PUBLIC)
    @Value("${jar.path:}")
    private String jarPath;

    // The following properties should not normally be set/changed by the user
    @ValueDescription(description = "Docker Image name:tag", defaultValue = "", group = Config.GROUP_PUBLIC)
    @Value("${docker.image:}")
    private String dockerImage;

    @ValueDescription(description = "Docker tarfile path", defaultValue = "", group = Config.GROUP_PUBLIC)
    @Value("${docker.tar:}")
    private String dockerTar;

    @ValueDescription(description = "docker.image.id", defaultValue = "", group = Config.GROUP_PUBLIC)
    @Value("${docker.image.id:}")
    private String dockerImageId;

    @ValueDescription(description = "Docker Image Repo", defaultValue = "", group = Config.GROUP_PRIVATE)
    @Value("${docker.image.repo:}")
    private String dockerImageRepo;

    @ValueDescription(description = "Docker Image Tag", defaultValue = "", group = Config.GROUP_PRIVATE)
    @Value("${docker.image.tag:}")
    private String dockerImageTag;

    @ValueDescription(description = "Running on host?", defaultValue = "true", group = Config.GROUP_PRIVATE)
    @Value("${on.host:true}")
    private Boolean onHost;

    @ValueDescription(description = "Caller Name", defaultValue = "", group = Config.GROUP_PRIVATE)
    @Value("${caller.name:}")
    private String callerName;

    @ValueDescription(description = "caller.version", defaultValue = "", group = Config.GROUP_PRIVATE)
    @Value("${caller.version:}")
    private String callerVersion;

    @ValueDescription(description = "Phone Home?", defaultValue = "true", group = Config.GROUP_PRIVATE)
    @Value("${phone.home:true}")
    private Boolean phoneHome;

    private List<DockerInspectorOption> publicOptions;
    private Map<String, DockerInspectorOption> options;
    private List<String> allKeys;
    private boolean initialized = false;

    public String get(final String key) throws IllegalArgumentException, IllegalAccessException {
        init();
        final DockerInspectorOption opt = options.get(key);
        if (opt == null) {
            return null;
        }
        return opt.getResolvedValue();
    }

    public boolean isPublic(final String key) throws IllegalArgumentException, IllegalAccessException {
        init();
        final DockerInspectorOption opt = options.get(key);
        if (opt == null) {
            return false;
        }
        return (Config.GROUP_PUBLIC.equals(opt.getGroup()));
    }

    public List<DockerInspectorOption> getPublicConfigOptions() throws IllegalArgumentException, IllegalAccessException {
        init();
        return publicOptions;
    }

    public List<String> getAllKeys() throws IllegalArgumentException, IllegalAccessException {
        init();
        return allKeys;
    }

    public void init() throws IllegalArgumentException, IllegalAccessException {
        if (initialized) {
            return;
        }
        final Object configObject = this;
        publicOptions = new ArrayList<>();
        allKeys = new ArrayList<>();
        options = new HashMap<>();
        for (final Field field : configObject.getClass().getDeclaredFields()) {
            final Annotation[] declaredAnnotations = field.getDeclaredAnnotations();
            if (declaredAnnotations.length > 0) {
                for (final Annotation annotation : declaredAnnotations) {
                    if (annotation.annotationType().getName().equals(ValueDescription.class.getName())) {
                        logger.debug(String.format("ValueDescription annotated config object field: %s", field.getName()));
                        final String propMappingString = field.getAnnotation(Value.class).value();
                        final String propName = SpringValueUtils.springKeyFromValueAnnotation(propMappingString);
                        final Object fieldValueObject = field.get(configObject);
                        if (fieldValueObject == null) {
                            logger.warn(String.format("propName %s field is null", propName));
                            continue;
                        }
                        logger.trace(String.format("adding prop key %s", propName));
                        allKeys.add(propName);
                        final String value = fieldValueObject.toString();
                        final ValueDescription valueDescription = field.getAnnotation(ValueDescription.class);
                        final DockerInspectorOption opt = new DockerInspectorOption(propName, field.getName(), value, valueDescription.description(), field.getType(), valueDescription.defaultValue(), valueDescription.group());
                        options.put(propName, opt);
                        if (!Config.GROUP_PRIVATE.equals(valueDescription.group())) {
                            publicOptions.add(opt);
                        } else {
                            logger.debug(String.format("private prop: propName: %s, fieldName: %s, group: %s, description: %s", propName, field.getName(), valueDescription.group(), valueDescription.description()));
                        }
                    }
                }
            }
        }
        initialized = true;
    }

}
