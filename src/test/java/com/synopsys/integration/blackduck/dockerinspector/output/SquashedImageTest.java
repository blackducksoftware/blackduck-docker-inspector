package com.synopsys.integration.blackduck.dockerinspector.output;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.synopsys.integration.blackduck.dockerinspector.ProcessId;
import com.synopsys.integration.blackduck.dockerinspector.config.ProgramPaths;
import com.synopsys.integration.blackduck.dockerinspector.dockerclient.DockerClientManager;
import com.synopsys.integration.exception.IntegrationException;

public class SquashedImageTest {

    @Test
    public void testCreateSquashedImageTarGz() throws IOException, IntegrationException {

        final DockerClientManager dockerClientManager = new DockerClientManager();
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

        final SquashedImage squashedImage = new SquashedImage();
        squashedImage.setDockerClientManager(dockerClientManager);

        final File targetImageFileSystemTarGz = new File("src/test/resources/test_containerfilesystem.tar.gz");
        final File testWorkingDir = new File("test/output/squashingTest");
        final File tempTarFile = new File(testWorkingDir, "tempContainerFileSystem.tar");
        final File squashingWorkingDir = new File(testWorkingDir, "squashingCode");
        squashingWorkingDir.mkdirs();
        final File squashedImageTarGz = new File("test/output/squashing/test_squashedimage.tar.gz");

        squashedImage.createSquashedImageTarGz(targetImageFileSystemTarGz, squashedImageTarGz, tempTarFile, squashingWorkingDir);

        final File unpackedSquashedImage = new File(testWorkingDir, "squashedImageUnpacked");
        unpackedSquashedImage.mkdirs();
        CompressedFile.gunZipUnTarFile(squashedImageTarGz, tempTarFile, unpackedSquashedImage);
        final File expectedFile = new File(unpackedSquashedImage, "manifest.json");
        assertTrue(expectedFile.isFile());
    }
}
