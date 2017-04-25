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
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value

import com.blackducksoftware.integration.hub.docker.PackageManagerEnum
import com.blackducksoftware.integration.hub.exception.HubIntegrationException

abstract class Executor {
    private final Logger logger = LoggerFactory.getLogger(getClass())

    PackageManagerEnum packageManagerEnum
    String testCommand
    String listPackagesCommand

    @Value('${command.timeout}')
    long commandTimeout

    abstract void init()

    void initValues(PackageManagerEnum packageManagerEnum, String testCommand, String listPackagesCommand) {
        this.packageManagerEnum = packageManagerEnum
        this.testCommand = testCommand
        this.listPackagesCommand = listPackagesCommand
    }

    boolean isCommandAvailable() {
        try {
            def proc = testCommand.execute()
            proc.waitForOrKill(commandTimeout)

            return proc.exitValue() == 0
        } catch(Exception e) {
            logger.debug("Error executing test command {}",testCommand,e)
            return false
        }
    }

    String[] listPackages() {
        executeCommand(listPackagesCommand)
    }

    String[] getPackageInfo(String packageName) {
        def infoCommand = getPackageInfoCommand(packageName)
        executeCommand(infoCommand)
    }

    String[] executeCommand(String command){
        try {
            def standardOut = new StringBuilder()
            def standardError = new StringBuilder()
            def process = command.execute()
            process.consumeProcessOutput(standardOut, standardError)
            process.waitForOrKill(commandTimeout)

            if(process.exitValue() !=0){
                logger.error(standardError.toString())
                throw new HubIntegrationException("Failed to run command ${command}")
            }

            def output =  standardOut.toString()
            logger.trace(output)
            output.split(System.lineSeparator())
        } catch(Exception e) {
            logger.error("Error executing command {}",listPackagesCommand,e)
        }
    }
}
