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
package com.blackducksoftware.integration.hub.linux.creator

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

import org.junit.Test

class CreatorTest {
    @Test
    void testCommandAvailableFalse() {
        def creator = [init: {}] as Creator
        creator.testCommand = 'a_bad_cmd_'
        creator.executionCommand = ''
        assertFalse creator.isCommandAvailable(120000)
    }

    @Test
    void testCommandAvailableTrue() {
        def creator = [init: {}] as Creator
        creator.testCommand = 'ls -al'
        creator.executionCommand = ''
        assertTrue creator.isCommandAvailable(120000)
    }

    @Test
    void testCreateOutputInvalidCommand() {
        def creator = [init: {}] as Creator
        creator.testCommand = ''
        creator.executionCommand = 'a_bad_cmd_ --version'

        def directory = new File("./build/tmp")
        def file = new File(directory, "creator_test_file.txt")
        creator.writeOutputFile(file, 120000)

        System.out.println("testCreateOutputInvalidCommand: file = "+ file.getCanonicalPath())
        def directoryPath = directory.getCanonicalPath()

        assertNotNull file
        assertEquals(directory.getCanonicalPath(), file.getParentFile().getCanonicalPath())
        assertEquals('creator_test_file.txt', file.getName())
        assertFalse file.exists()
        assertEquals(file.size(), 0)

        file.delete()
    }

    @Test
    void testCreateOutputCommand() {
        def creator = [init: {}] as Creator
        creator.testCommand = 'ls -al'
        creator.executionCommand = 'ls -al'

        def directory = new File("./build/tmp")
        def file = new File(directory, "creator_test_file.txt")
        creator.writeOutputFile(file, 120000)

        System.out.println("testCreateOutputCommand: file = "+ file.getCanonicalPath())
        def directoryPath = directory.getCanonicalPath()

        assertNotNull file
        assertTrue file.isFile()
        assertTrue(file.size() > 0)
        assertEquals(directory.getCanonicalPath(), file.getParentFile().getCanonicalPath())
        assertEquals("creator_test_file.txt", file.getName())

        file.delete()
    }
}
