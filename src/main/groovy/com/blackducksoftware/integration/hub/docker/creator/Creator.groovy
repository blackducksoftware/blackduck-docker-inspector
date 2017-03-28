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
package com.blackducksoftware.integration.hub.docker.creator

import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Value

import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum

abstract class Creator {
    private final Logger logger = LoggerFactory.getLogger(getClass())

    @Value('${filename.separator}')
    String filenameSeparator

    PackageManagerEnum packageManagerEnum
    String testCommand
    String listPackagesCommand

    abstract void init()

    abstract String getPackageInfoCommand(String packageName)

    void initValues(PackageManagerEnum packageManagerEnum, String testCommand, String listPackagesCommand) {
        this.packageManagerEnum = packageManagerEnum
        this.testCommand = testCommand
        this.listPackagesCommand = listPackagesCommand
    }

    String filename(String hubProjectName, String hubProjectVersionName, OperatingSystemEnum operatingSystemEnum) {
        def pieces = [
            hubProjectName,
            hubProjectVersionName,
            operatingSystemEnum.forge,
            packageManagerEnum.filenameSuffix
        ]
        pieces.join(filenameSeparator)
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

    String[] listPackages(long timeout) {
        try {
            def standardOut = new StringBuilder()
            def standardError = new StringBuilder()
            def process = listPackagesCommand.execute()
            process.consumeProcessOutput(standardOut, standardError)
            process.waitForOrKill(timeout)

            standardOut.toString().split(System.lineSeparator())
        } catch(Exception e) {
            logger.error("Error executing command {}",listPackagesCommand,e)
        }
    }

    String[] getPackageInfo(long timeout, String packageName) {
        def infoCommand = getPackageInfoCommand(packageName)
        try {
            def standardOut = new StringBuilder()
            def standardError = new StringBuilder()
            def process = infoCommand.execute()
            process.consumeProcessOutput(standardOut, standardError)
            process.waitForOrKill(timeout)

            standardOut.toString().split(System.lineSeparator())
        } catch(Exception e) {
            logger.error("Error executing command {}",infoCommand,e)
        }
    }
}
