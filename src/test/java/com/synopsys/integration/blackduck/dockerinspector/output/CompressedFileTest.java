package com.synopsys.integration.blackduck.dockerinspector.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CompressedFileTest {
    private static File fileToCompress;
    private static File compressedFile;

    @BeforeAll
    public static void setUp() throws IOException {
        fileToCompress = new File("test/fileToCompress.txt");
        compressedFile = new File("test/fileToCompress.txt.gz");
        compressedFile.delete();
        fileToCompress.createNewFile();

    }

    @AfterAll
    public static void tearDown() {
        fileToCompress.delete();
        compressedFile.delete();
    }

    @Test
    public void testGunZip() throws IOException {
        final File fileToUnpack = new File("src/test/resources/test.tar.gz");
        final File unCompressedFile = new File("test/output/test.tar");
        CompressedFile.gunZipFile(fileToUnpack, unCompressedFile);
        assertTrue(unCompressedFile.exists());
    }

    // TODO this will end up beiung redundant
    @Test
    public void testUnpackTarGz() throws IOException {
        final File fileToUnpack = new File("src/test/resources/test.tar.gz");
        final File destinationDir = new File("test/output/testTarGzDir");
        CompressedFile.unpackTarGz(fileToUnpack, destinationDir);
        final File unpackedFile = new File("test/output/testTarGzDir/test.txt");
        assertTrue(unpackedFile.exists());
    }

    @Test
    public void testGZipFile() throws IOException {
        assertTrue(fileToCompress.exists());
        assertFalse(compressedFile.exists());
        CompressedFile.gZipFile(fileToCompress, compressedFile);
        assertTrue(compressedFile.exists());
        assertEquals("fileToCompress.txt.gz", compressedFile.getName());
    }
}
