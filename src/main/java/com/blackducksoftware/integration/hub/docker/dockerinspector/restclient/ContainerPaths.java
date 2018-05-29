/**
 * hub-docker-inspector
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
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
package com.blackducksoftware.integration.hub.docker.dockerinspector.restclient;

import java.io.IOException;

public interface ContainerPaths {

    /*
     * Translate a local path (to a file within the dir shared with the container) to the equivalent path for the container. Find path to the given localPath RELATIVE to the local shared dir. Convert that to the container's path by
     * appending that relative path to the container's path to the shared dir
     */
    String getContainerPathToTargetFile(String localPathToTargetFile) throws IOException;

    String getContainerPathToOutputFile(String outputFilename);

    String getContainerPathToSharedDir();

    String getContainerPathToOutputDir();

    String getContainerPathToTargetDir();

}
