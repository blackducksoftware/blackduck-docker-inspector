/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector;

import java.lang.management.ManagementFactory;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProcessId {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final String cleanedProcessId;


  public ProcessId() {
    cleanedProcessId = atSignToUnderscore(getProcessIdOrGenerateUniqueId());
    logger.debug(String.format("Process name: %s", cleanedProcessId));
  }

  public String addProcessIdToName(final String origName) {
    final String adjustedName = String.format("%s_%s", origName, cleanedProcessId);
    logger.debug(String.format("Adjusted %s to %s", origName, adjustedName));
    return adjustedName;
  }

  private String getProcessIdOrGenerateUniqueId() {
    String processId;
    try {
      processId = ManagementFactory.getRuntimeMXBean().getName();
      return processId;
    } catch (final Exception e) {
      logger.debug(String.format("Unable to get process ID from system: %s", e.getMessage()));
      final long currentMillisecond = new Date().getTime();
      processId = Long.toString(currentMillisecond);
    }
    return processId;
  }

  private String atSignToUnderscore(final String imageName) {
    return imageName.replace("@", "_");
  }
}
