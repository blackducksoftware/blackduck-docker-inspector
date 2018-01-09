package com.blackducksoftware.integration.hub.docker.imageinspector.linux;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.blackducksoftware.integration.hub.docker.imageinspector.linux.FileOperations;

public class FileOperationsTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void test() throws IOException {
        final File fileToMove = new File("test/fileToMove.txt");
        fileToMove.createNewFile();
        final File destinationDir = new File("test/output");
        if (!destinationDir.exists()) {
            destinationDir.mkdirs();
        }
        final File destinationFile = new File(destinationDir, "fileToMove.txt");
        destinationFile.delete();
        FileOperations.moveFile(fileToMove, destinationDir);
        assertTrue(destinationFile.exists());
    }
}
