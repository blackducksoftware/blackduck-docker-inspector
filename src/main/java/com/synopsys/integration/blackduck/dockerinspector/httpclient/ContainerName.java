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
package com.synopsys.integration.blackduck.dockerinspector.httpclient;

import com.synopsys.integration.blackduck.dockerinspector.ProcessId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ContainerName {

  @Autowired
  private ProcessId processId;

  public String deriveContainerNameFromImageInspectorRepo(final String imageInspectorRepo) {
    String extractorContainerName;
    final int slashIndex = imageInspectorRepo.lastIndexOf('/');
    if (slashIndex < 0) {
      extractorContainerName = imageInspectorRepo;
    } else {
      extractorContainerName = imageInspectorRepo.substring(slashIndex + 1);
    }
    return processId.addProcessIdToName(extractorContainerName);
  }

}
