package com.blackducksoftware.integration.hub.docker.linux

import static org.junit.Assert.*

import java.nio.file.Path

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import com.blackducksoftware.integration.hub.docker.TestUtils

class DirTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void test() {
        File parentDir = TestUtils.createTempDirectory()
        Path parentDirPath = parentDir.toPath()
        Path targetDirPath = parentDirPath.resolve("sub1/sub2/sub3")
        File targetDir = targetDirPath.toFile()
        targetDir.mkdirs()
        Path targetFilePath = targetDirPath.resolve("targetFile")
        File targetFile = targetFilePath.toFile()
        targetFile.createNewFile()

        List<File> results = Dir.findFileWithName(parentDir, "targetFile")
        assertEquals(1, results.size())
        assertEquals("targetFile", results.get(0).getName())
        println "Found: ${results.get(0).getAbsolutePath()}"
    }
}
