package com.blackducksoftware.integration.hub.docker.dockerinspector.imageinspector.linux

import static org.junit.Assert.*

import java.nio.file.Path

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test

import com.blackducksoftware.integration.hub.docker.dockerinspector.imageinspector.TestUtils
import com.blackducksoftware.integration.hub.docker.dockerinspector.imageinspector.linux.FileOperations

class DirTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    // TODO for some reason this is failing running from command line,
    // but only when all tests are run (succeeds by itself)
    @Ignore
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

        List<File> results = FileOperations.findFileWithName(parentDir, "targetFile")
        assertEquals(1, results.size())
        assertEquals("targetFile", results.get(0).getName())
        println "Found: ${results.get(0).getAbsolutePath()}"
    }
}
