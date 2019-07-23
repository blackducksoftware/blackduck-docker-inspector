package com.synopsys.integration.blackduck.dockerinspector.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.blackduck.dockerinspector.ProcessId;
import com.synopsys.integration.blackduck.dockerinspector.config.ProgramPaths;
import com.synopsys.integration.blackduck.dockerinspector.dockerclient.DockerClientManager;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.exception.IntegrationException;

@Tag("integration")
public class SquashedImageTest {
    private static SquashedImage squashedImage;
    private static DockerClientManager dockerClientManager;

    @BeforeAll
    public static void setUp() {
        dockerClientManager = new DockerClientManager();
        final ImageTarFilename imageTarFilename = new ImageTarFilename();
        final ProgramPaths programPaths = new ProgramPaths();
        final ProcessId processId = new ProcessId();

        programPaths.setConfig(null);
        programPaths.setProcessId(processId);
        imageTarFilename.setConfig(null);
        imageTarFilename.setDockerClientManager(dockerClientManager);
        imageTarFilename.setProgramPaths(programPaths);
        dockerClientManager.setConfig(null);
        dockerClientManager.setImageTarFilename(imageTarFilename);

        squashedImage = new SquashedImage();
        squashedImage.setDockerClientManager(dockerClientManager);
        squashedImage.setFileOperations(new FileOperations());
    }

    @Test
    public void testCreateSquashedImageTarGz() throws IOException, IntegrationException {

        final File targetImageFileSystemTarGz = new File("src/test/resources/test_containerfilesystem.tar.gz");
        final File testWorkingDir = new File("test/output/squashingTest");
        FileUtils.deleteDirectory(testWorkingDir);
        final File tempTarFile = new File(testWorkingDir, "tempContainerFileSystem.tar");
        final File squashingWorkingDir = new File(testWorkingDir, "squashingCode");
        squashingWorkingDir.mkdirs();
        final File squashedImageTarGz = new File("test/output/squashingTest/test_squashedimage.tar.gz");

        squashedImage.createSquashedImageTarGz(targetImageFileSystemTarGz, squashedImageTarGz, tempTarFile, squashingWorkingDir);

        final File unpackedSquashedImageDir = new File(testWorkingDir, "squashedImageUnpacked");
        unpackedSquashedImageDir.mkdirs();
        CompressedFile.gunZipUnTarFile(squashedImageTarGz, tempTarFile, unpackedSquashedImageDir);
        final File manifestFile = new File(unpackedSquashedImageDir, "manifest.json");
        assertTrue(manifestFile.isFile());

        // Find the one layer dir in image
        File layerDir = null;
        for (final File imageFile : unpackedSquashedImageDir.listFiles()) {
            if (imageFile.isDirectory()) {
                layerDir = imageFile;
                break;
            }
        }
        assertNotNull(layerDir);

        // Untar the one layer.tar file in image
        final File layerTar = layerDir.listFiles()[0];
        final File layerUnpackedDir = new File(squashingWorkingDir, "squashedImageLayerUnpacked");
        CompressedFile.unTarFile(layerTar, layerUnpackedDir);

        // Verify that the symlink made it into the squashed image
        final File symLink = new File(layerUnpackedDir, "usr/share/apk/keys/aarch64/alpine-devel@lists.alpinelinux.org-58199dcc.rsa.pub");
        assertTrue(symLink.exists());
        final Path symLinkPath = symLink.toPath();
        assertTrue(Files.isSymbolicLink(symLinkPath));
        final Path symLinkTargetPath = Files.readSymbolicLink(symLinkPath);
        assertEquals("../alpine-devel@lists.alpinelinux.org-58199dcc.rsa.pub", symLinkTargetPath.toString());
    }

    @Test
    public void testGenerateUniqueImageRepoTag() throws IntegrationException {
        final String generatedRepTag = squashedImage.generateUniqueImageRepoTag();

        assertTrue(generatedRepTag.startsWith("dockerinspectorsquashed-"));
        assertTrue(generatedRepTag.endsWith(":1"));
    }
}
