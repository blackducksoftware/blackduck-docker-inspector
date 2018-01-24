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
package com.blackducksoftware.integration.hub.docker.dockerinspector.imageinspector.linux.executor


import com.blackducksoftware.integration.hub.docker.dockerinspector.imageinspector.imageformat.docker.ImagePkgMgr
import com.blackducksoftware.integration.hub.docker.dockerinspector.imageinspector.linux.executor.PkgMgrExecutor
import com.blackducksoftware.integration.hub.exception.HubIntegrationException

class ExecutorMock extends PkgMgrExecutor {

    File resourceFile

    ExecutorMock(File resourceFile){
        this.resourceFile = resourceFile
    }

    public String[] runPackageManager(final ImagePkgMgr imagePkgMgr) throws HubIntegrationException, IOException, InterruptedException {
        final String[] packages = listPackages();
        return packages;
    }

    String[] listPackages(){
        resourceFile as String[]
    }

    @Override
    public void init() {
    }
}