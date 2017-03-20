/*
 * Copyright (C) 2017 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Black Duck Software.
 */
package com.blackducksoftware.integration.hub.linux.extractor.data

import org.apache.commons.lang3.StringUtils

import groovy.transform.ToString

@ToString(includePackage = false)
class DpkgStatusFilePackage {
    String name
    String version
    String architecture
    Boolean installed

    String getExternalId(){
        "$name/$version/$architecture"
    }

    boolean isEmpty(){
        name == null && version == null && architecture == null && installed == null
    }

    boolean isComplete(){
        StringUtils.isNotBlank(name) && StringUtils.isNotBlank(version) && StringUtils.isNotBlank(architecture) && installed != null
    }
}
