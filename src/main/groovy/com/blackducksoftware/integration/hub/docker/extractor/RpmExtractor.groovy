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
import com.sleepycat.bind.tuple.StringBinding
import com.sleepycat.collections.StoredSortedMap
import com.sleepycat.je.Database
import com.sleepycat.je.DatabaseConfig
import com.sleepycat.je.Environment
import com.sleepycat.je.EnvironmentConfig

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
        DatabaseConfig databaseConfig = new DatabaseConfig();
        databaseConfig.setAllowCreate( true );
        Database db = environment.openDatabase( null, "Packages", databaseConfig );
        try {
            StoredSortedMap<String, String> ssm = new StoredSortedMap<String, String>(db, new StringBinding(), new StringBinding(), true);
            println("hello")
        } finally {
            db.close();
        }
    }
}