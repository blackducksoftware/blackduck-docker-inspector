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
package com.blackducksoftware.integration.hub.docker.extractor

import javax.annotation.PostConstruct

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.bdio.simple.BdioWriter
import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum
import com.sleepycat.je.Cursor
import com.sleepycat.je.Database
import com.sleepycat.je.DatabaseConfig
import com.sleepycat.je.DatabaseEntry
import com.sleepycat.je.Environment
import com.sleepycat.je.EnvironmentConfig
import com.sleepycat.je.LockMode
import com.sleepycat.je.OperationStatus

@Component
class RpmExtractor extends Extractor {
    private final Logger logger = LoggerFactory.getLogger(RpmExtractor.class)

    @PostConstruct
    void init() {
        initValues(PackageManagerEnum.RPM)
    }

    @Override
    void extractComponents(BdioWriter bdioWriter, OperatingSystemEnum operatingSystem, File databaseDirectory) {
        EnvironmentConfig envConfig = new EnvironmentConfig()
        envConfig.setTransactional(false)
        envConfig.setAllowCreate(true)
        Environment environment = new Environment(databaseDirectory, envConfig)
        environment.getDatabaseNames().each{name -> println(name) }

        DatabaseConfig databaseConfig = new DatabaseConfig()
        databaseConfig.setAllowCreate( false )
        databaseConfig.setReadOnly(true)
        Database db = environment.openDatabase( null, "__db.001", databaseConfig )
        Cursor cursor = null
        try {
            cursor = db.openCursor(null, null)
            DatabaseEntry foundKey = new DatabaseEntry()
            DatabaseEntry foundData = new DatabaseEntry()
            while (cursor.getNext(foundKey, foundData, LockMode.DEFAULT) ==
            OperationStatus.SUCCESS) {
                String keyString = new String(foundKey.getData())
                String dataString = new String(foundData.getData())
                println("Key | Data : " + keyString + " | " + dataString + "")
            }

            println("hello")
        } finally {
            cursor.close()
            db.close()
        }
    }
}