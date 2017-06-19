/**
 * Hub Docker Inspector
 *
 * Copyright (C) 2017 Black Duck Software, Inc.
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
	String[] listPackages() {
		executeCommand(listPackagesCommand)
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