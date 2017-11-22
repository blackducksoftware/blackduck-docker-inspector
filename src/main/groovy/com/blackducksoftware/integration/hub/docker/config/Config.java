package com.blackducksoftware.integration.hub.docker.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

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

    @ValueDescription(description = "This is test.prop.public.string's description", defaultValue = "someDefault", group = Config.GROUP_PUBLIC)
    @Value("${test.prop.public.string:}")
    private String testPropPublicString;

    @ValueDescription(description = "This is test.prop.public.boolean's description", defaultValue = "false", group = Config.GROUP_PUBLIC)
    @Value("${test.prop.public.boolean:false}")
    private Boolean testPropPublicBoolean;

    @ValueDescription(description = "This is test.prop.private's description", defaultValue = "false", group = Config.GROUP_PRIVATE)
    @Value("${test.prop.private:}")
    private Boolean testPropPrivate;

    private List<DockerInspectorOption> publicOptions;
    private List<String> allKeys;
    private boolean initialized = false;

    public String getTestPropPublicString() {
        return testPropPublicString;
    }

    public Boolean getTestPropPublicBoolean() {
        return testPropPublicBoolean;
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
        for (final Field field : configObject.getClass().getDeclaredFields()) {
            final Annotation[] declaredAnnotations = field.getDeclaredAnnotations();
            if (declaredAnnotations.length > 0) {
                for (final Annotation annotation : declaredAnnotations) {
                    logger.debug(String.format("config object field: %s, annotation: %s", field.getName(), annotation.annotationType().getName()));
                    if (annotation.annotationType().getName().equals(ValueDescription.class.getName())) {
                        final String propMappingString = field.getAnnotation(Value.class).value();
                        final String propName = SpringValueUtils.springKeyFromValueAnnotation(propMappingString);
                        allKeys.add(propName);
                        final ValueDescription valueDescription = field.getAnnotation(ValueDescription.class);
                        if (!Config.GROUP_PRIVATE.equals(valueDescription.group())) {
                            final Object fieldValueObject = field.get(configObject);
                            if (fieldValueObject == null) {
                                logger.warn(String.format("propName %s field is null", propName));
                                continue;
                            }
                            final String value = fieldValueObject.toString();
                            final DockerInspectorOption opt = new DockerInspectorOption(propName, field.getName(), value, valueDescription.description(), field.getType(), valueDescription.defaultValue(), valueDescription.group());
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
