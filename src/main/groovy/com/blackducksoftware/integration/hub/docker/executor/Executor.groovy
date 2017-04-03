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

import org.slf4j.Logger

import com.blackducksoftware.integration.hub.docker.PackageManagerEnum

abstract class Executor {
    private final Logger logger = LoggerFactory.getLogger(getClass())

    PackageManagerEnum packageManagerEnum
    String testCommand
    String listPackagesCommand
    long commandTimeout

    abstract void init()

    void initValues(PackageManagerEnum packageManagerEnum, String testCommand, String listPackagesCommand, long commandTimeout) {
        this.packageManagerEnum = packageManagerEnum
        this.testCommand = testCommand
        this.listPackagesCommand = listPackagesCommand
    }

    boolean isCommandAvailable(long timeout) {
        try {
            def proc = testCommand.execute()
            proc.waitForOrKill(timeout)

            return proc.exitValue() == 0
        } catch(Exception e) {
            logger.debug("Error executing test command {}",testCommand,e)
            return false;
        }
    }

    String[] listPackages() {
        executeCommand(listPackagesCommand)
    }

    String[] getPackageInfo(String packageName) {
        def infoCommand = getPackageInfoCommand(packageName)
        executeCommand(infoCommand)
    }

    String[] executeCommand(String commmand){
        try {
            def standardOut = new StringBuilder()
            def standardError = new StringBuilder()
            def process = commmand.execute()
            process.consumeProcessOutput(standardOut, standardError)
            process.waitForOrKill(commandTimeout)

            standardOut.toString().split(System.lineSeparator())
        } catch(Exception e) {
            logger.error("Error executing command {}",listPackagesCommand,e)
        }
    }
}
