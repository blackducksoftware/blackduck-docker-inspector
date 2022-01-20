package com.synopsys.integration.blackduck.dockerinspector.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import com.synopsys.integration.blackduck.imageinspector.api.name.ImageNameResolver;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.blackduck.dockerinspector.ProcessId;
import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.ProgramPaths;
import com.synopsys.integration.blackduck.dockerinspector.dockerclient.DockerClientManager;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.exception.IntegrationException;

@Tag("integration")
public class SquashedImageTest {
    private static SquashedImage squashedImage;
    private static DockerClientManager dockerClientManager;
    private static File testWorkingDir;

    @BeforeAll
    public static void setUp() throws IOException {
        testWorkingDir = new File("test/output/squashingTest");
        ImageTarFilename imageTarFilename = new ImageTarFilename();
        FileOperations fileOperations = new FileOperations();
        Config config = Mockito.mock(Config.class);
        Mockito.when(config.getWorkingDirPath()).thenReturn(testWorkingDir.getCanonicalPath());
        ProgramPaths programPaths = new ProgramPaths(config, new ProcessId());
        dockerClientManager = new DockerClientManager(fileOperations, new ImageNameResolver(), config, imageTarFilename, programPaths);

        squashedImage = new SquashedImage();
        squashedImage.setDockerClientManager(dockerClientManager);
        squashedImage.setFileOperations(new FileOperations());
    }

    @Test
    public void testCreateSquashedImageTarGz() throws IOException, IntegrationException {

        File targetImageFileSystemTarGz = new File("src/test/resources/test_containerfilesystem.tar.gz");

        FileUtils.deleteDirectory(testWorkingDir);
        File tempTarFile = new File(testWorkingDir, "tempContainerFileSystem.tar");
        File squashingWorkingDir = new File(testWorkingDir, "squashingCode");
        squashingWorkingDir.mkdirs();
        File squashedImageTarGz = new File("test/output/squashingTest/test_squashedimage.tar.gz");

        squashedImage.createSquashedImageTarGz(targetImageFileSystemTarGz, squashedImageTarGz, tempTarFile, squashingWorkingDir);

        File unpackedSquashedImageDir = new File(testWorkingDir, "squashedImageUnpacked");
        unpackedSquashedImageDir.mkdirs();
        CompressedFile.gunZipUnTarFile(squashedImageTarGz, tempTarFile, unpackedSquashedImageDir);


        // TODO TEMP
        Collection<File> filesFound = FileUtils.listFilesAndDirs(unpackedSquashedImageDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        System.out.printf("kkk Contents of %s:\n", unpackedSquashedImageDir.getAbsolutePath());
        for (File foundFile : filesFound) {
            System.out.printf("%s (symlink: %b; dir: %b)\n", foundFile.getAbsolutePath(), Files.isSymbolicLink(foundFile.toPath()), foundFile.isDirectory());
        }
        ////////////

        File manifestFile = new File(unpackedSquashedImageDir, "manifest.json");
        assertTrue(manifestFile.isFile());

        // Find the one layer dir in image
        File layerDir = null;
        for (File imageFile : unpackedSquashedImageDir.listFiles()) {
            if (imageFile.isDirectory()) {
                layerDir = imageFile;
                break;
            }
        }
        assertNotNull(layerDir);

        // Untar the one layer.tar file in image
        File layerTar = layerDir.listFiles()[0];
        File layerUnpackedDir = new File(squashingWorkingDir, "squashedImageLayerUnpacked");
        CompressedFile.unTarFile(layerTar, layerUnpackedDir);

        // TODO TEMP
        filesFound = FileUtils.listFilesAndDirs(layerUnpackedDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        System.out.printf("kkk Contents of %s:\n", layerUnpackedDir.getAbsolutePath());
        for (File foundFile : filesFound) {
            System.out.printf("%s (symlink: %b; dir: %b)\n", foundFile.getAbsolutePath(), Files.isSymbolicLink(foundFile.toPath()), foundFile.isDirectory());
        }
        ////////////

        // Verify that the symlink made it into the squashed image
        File symLink = new File(layerUnpackedDir, "usr/share/apk/keys/aarch64/alpine-devel@lists.alpinelinux.org-58199dcc.rsa.pub");
        assertTrue(symLink.exists());
        Path symLinkPath = symLink.toPath();
        assertTrue(Files.isSymbolicLink(symLinkPath));
        Path symLinkTargetPath = Files.readSymbolicLink(symLinkPath);
        assertEquals("../alpine-devel@lists.alpinelinux.org-58199dcc.rsa.pub", symLinkTargetPath.toString());
    }

    @Test
    public void testGenerateUniqueImageRepoTag() throws IntegrationException {
        String generatedRepTag = squashedImage.generateUniqueImageRepoTag();

        assertTrue(generatedRepTag.startsWith("dockerinspectorsquashed-"));
        assertTrue(generatedRepTag.endsWith(":1"));
    }
}
