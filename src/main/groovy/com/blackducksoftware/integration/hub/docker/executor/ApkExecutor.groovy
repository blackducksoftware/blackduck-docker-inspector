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
package com.blackducksoftware.integration.hub.docker.executor

import javax.annotation.PostConstruct

import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.docker.PackageManagerEnum

@Component
class ApkExecutor extends Executor {
    @PostConstruct
    void init() {
        initValues(PackageManagerEnum.APK, 'apk --version', 'apk info -v')
    }

    String getPackageInfoCommand(String packageName){
        "apk info ${packageName} -a"
    }
}