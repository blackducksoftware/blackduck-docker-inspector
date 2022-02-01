/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.config;

import com.synopsys.integration.exception.IntegrationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DockerInspectorSystemProperties {
  private  final Logger logger = LoggerFactory.getLogger(this.getClass());

  public void augmentSystemProperties(final String additionalSystemPropertiesPath) throws IntegrationException {
    if (StringUtils.isNotBlank(additionalSystemPropertiesPath)) {
      logger.debug(String.format("Reading user-provided additional System properties: %s", additionalSystemPropertiesPath));
      File additionalSystemPropertiesFile = new File(additionalSystemPropertiesPath);
      Properties additionalSystemProperties = new Properties();
      InputStream additionalSystemPropertiesInputStream = null;
      try {
        additionalSystemPropertiesInputStream = new FileInputStream(additionalSystemPropertiesFile);
        additionalSystemProperties.load(additionalSystemPropertiesInputStream);
        for (Object key : additionalSystemProperties.keySet()) {
          String keyString = (String) key;
          String value = additionalSystemProperties.getProperty(keyString);
          logger.trace(String.format("additional system property: %s", keyString));
          System.setProperty(keyString, value);
        }
      } catch (IOException e) {
        final String msg = String.format("Error loading additional system properties from %s: %s", additionalSystemPropertiesPath, e.getMessage());
        throw new IntegrationException(msg);
      }
    }
  }
}
