package com.synopsys.integration.blackduck.dockerinspector.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CompressedFileTest {
    private static File fileToCompress;
    private static File compressedFile;
    private static File destinationDir;
    private static File testTarGzDir;
    private static File tempTarFile;

    @BeforeAll
    public static void setUp() throws IOException {
        fileToCompress = new File("test/fileToCompress.txt");
        compressedFile = new File("test/fileToCompress.txt.gz");
        compressedFile.delete();
        fileToCompress.createNewFile();

        destinationDir = new File("test/output/testTarContents");
        FileUtils.deleteDirectory(destinationDir);

        testTarGzDir = new File("test/output/testTarGzDir");
        FileUtils.deleteDirectory(testTarGzDir);
        testTarGzDir.mkdirs();

        tempTarFile = new File("test/output/testTempTar.tar");
    }

    @AfterAll
    public static void tearDown() {
        fileToCompress.delete();
        compressedFile.delete();
        try {
            FileUtils.deleteDirectory(destinationDir);
        } catch (IOException e) {
        }
        try {
            FileUtils.deleteDirectory(testTarGzDir);
        } catch (IOException e) {
        }
        tempTarFile.delete();
    }

    @Test
    public void testGunZipFile() throws IOException {
        final File fileToUnZip = new File("src/test/resources/test1.tar.gz");
        final File unCompressedFile = new File("test/output/test.tar");
        CompressedFile.gunZipFile(fileToUnZip, unCompressedFile);
        assertTrue(unCompressedFile.exists());
    }

    @Test
    public void testUnTarFile() throws IOException {
        final File fileToUnTar = new File("src/test/resources/test2.tar");

        CompressedFile.unTarFile(fileToUnTar, destinationDir);

        final File expectedFile1 = new File(destinationDir, "test2.txt");
        assertTrue(expectedFile1.exists());

        final File expectedFile2 = new File(destinationDir, "subdir/test3.txt");
        assertTrue(expectedFile2.exists());
    }

    @Test
    public void testGunZipUnTarFile() throws IOException {
        final File fileToUnpack = new File("src/test/resources/test1.tar.gz");
        assertTrue(fileToUnpack.exists());
        assertFalse(tempTarFile.exists());

        CompressedFile.gunZipUnTarFile(fileToUnpack, tempTarFile, testTarGzDir);
        final File unpackedFile = new File("test/output/testTarGzDir/test1.txt");
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
