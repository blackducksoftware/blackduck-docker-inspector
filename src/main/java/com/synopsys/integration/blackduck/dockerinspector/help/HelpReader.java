/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.help;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;
import com.synopsys.integration.exception.IntegrationException;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

@Component
public class HelpReader {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ProgramVersion programVersion;

    @Autowired
    private Config config;

    private Configuration freemarkerConfig = null;
    private Map<String, Object> variableData;

    public String getVariableSubstitutedTextFromHelpFile(String givenHelpTopicName) throws IntegrationException {
        String helpTopicName = ensureNotNull(givenHelpTopicName);
        try {
            init();
            Template template = createFreemarkerTemplate(helpTopicName);
            String helpFileContents = populateVariableValuesInHelpContent(template);
            return helpFileContents;
        } catch (IOException | TemplateException e) {
            String msg = String.format("Error processing help file for help topic: %s", helpTopicName);
            logger.error(msg, e);
            throw new IntegrationException(msg, e);
        }
    }

    private void init() throws IOException {
        ensureConfigInitialized();
        ensureVariableDataLoaded();
    }

    private void ensureConfigInitialized() throws IOException {
        if (freemarkerConfig == null) {
            freemarkerConfig = new Configuration(Configuration.VERSION_2_3_29);
            freemarkerConfig.setDefaultEncoding("UTF-8");
            freemarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            freemarkerConfig.setLogTemplateExceptions(false);
            freemarkerConfig.setWrapUncheckedExceptions(true);
            freemarkerConfig.setFallbackOnNullLoopVariable(false);

            if (StringUtils.isNotBlank(config.getHelpInputFilePath())) {
                File contentDir = new File(config.getHelpInputFilePath());
                freemarkerConfig.setDirectoryForTemplateLoading(contentDir);
            } else {
                freemarkerConfig.setClassForTemplateLoading(this.getClass(), "/help/content");
            }
        }
    }

    private void ensureVariableDataLoaded() throws IOException {
        if (variableData == null) {
            variableData = new HashMap<>();
            variableData.put("program_version", programVersion.getProgramVersion());
            Properties helpProperties = new Properties();
            try (InputStream propFileInputStream = this.getClass().getResourceAsStream("/help/data/help.properties")) {
                helpProperties.load(propFileInputStream);
            }
            for (String propertyName : helpProperties.stringPropertyNames()) {
                variableData.put(propertyName, helpProperties.getProperty(propertyName));
            }
        }
    }

    private String ensureNotNull(String helpTopicName) {
        if (helpTopicName == null) {
            helpTopicName = HelpTopicParser.HELP_TOPIC_NAME_OVERVIEW;
        }
        return helpTopicName;
    }

    private String populateVariableValuesInHelpContent(Template template) throws TemplateException, IOException {
        Writer out = new StringWriter();
        template.process(variableData, out);
        return out.toString();
    }

    private Template createFreemarkerTemplate(String helpTopicName) throws IOException {
        Template template;
        try {
            template = freemarkerConfig.getTemplate(String.format("/%s.md", helpTopicName.toLowerCase()));
        } catch (IOException e) {
            logger.info(String.format("Help topic %s not found; providing %s instead", helpTopicName, HelpTopicParser.HELP_TOPIC_NAME_OVERVIEW));
            template = freemarkerConfig.getTemplate(String.format("/%s.md", HelpTopicParser.HELP_TOPIC_NAME_OVERVIEW));
        }
        return template;
    }
}
