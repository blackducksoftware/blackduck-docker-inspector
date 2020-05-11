package com.synopsys.integration.blackduck.dockerinspector.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.gson.Gson;
import com.synopsys.integration.bdio.model.BdioBillOfMaterials;
import com.synopsys.integration.bdio.model.BdioProject;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.ProgramPaths;
import com.synopsys.integration.blackduck.dockerinspector.dockerclient.DockerClientManager;
import com.synopsys.integration.blackduck.imageinspector.linux.FileOperations;
import com.synopsys.integration.exception.IntegrationException;

@RunWith(SpringRunner.class)
public class OutputTest {

    @Mock
    private Config config;

    @Mock
    private ProgramPaths programPaths;

    @Mock
    private Gson gson;

    @Mock
    private ContainerFilesystemFilename containerFilesystemFilename;

    @InjectMocks
    private Output output;

    @BeforeAll
    public void setup() {

    }

    @Test
    public void testSquashedImageCreation() throws IOException, IntegrationException {

        final File testHome = new File("test/output/squashedImageCreation");
        FileUtils.deleteDirectory(testHome);
        final File outputDir = new File(testHome, "out");
        outputDir.mkdirs();
        final File containerFileSystemFrom = new File("src/test/resources/target_containerfilesystem.tar.gz");
        final File containerFileSystemTo = new File(outputDir, "target_containerfilesystem.tar.gz");
        FileUtils.copyFile(containerFileSystemFrom, containerFileSystemTo);
        final File workingDir = new File(testHome, "working");
        workingDir.mkdirs();

        final File squashedImageTarfile = new File(workingDir, "target_squashedimage.tar");
        final File squashingTempDir = new File(workingDir, "squashing_tmp");

        Mockito.when(config.getOutputPath()).thenReturn(outputDir.getAbsolutePath());
        Mockito.when(config.isOutputIncludeSquashedImage()).thenReturn(true);
        Mockito.when(programPaths.getUserOutputDirPath()).thenReturn(outputDir.getAbsolutePath());
        Mockito.when(programPaths.getDockerInspectorWorkingOutputPath()).thenReturn(workingDir.getAbsolutePath());
        Mockito.when(programPaths.getDockerInspectorSquashedImageTarFilePath()).thenReturn(squashedImageTarfile.getAbsolutePath());
        Mockito.when(programPaths.getDockerInspectorSquashedImageDirPath()).thenReturn(squashingTempDir.getAbsolutePath());

        final SimpleBdioDocument bdioDoc = Mockito.mock(SimpleBdioDocument.class);

        final BdioBillOfMaterials bom = new BdioBillOfMaterials();
        bom.spdxName = "registry.luciddg.com_luciddg_ldg-server-qa_2020.16.03_DPKG";
        Mockito.when(bdioDoc.getBillOfMaterials()).thenReturn(bom);
        final BdioProject project = new BdioProject();
        Mockito.when(bdioDoc.getProject()).thenReturn(project);

        Mockito.when(containerFilesystemFilename.deriveContainerFilesystemFilename(null, null)).thenReturn("target_containerfilesystem.tar.gz");

        final ImageTarFilename imageTarFilename = new ImageTarFilename();
        final FileOperations fileOperations = new FileOperations();
        final DockerClientManager dockerClientManager = new DockerClientManager(config, imageTarFilename, programPaths);
        SquashedImage squashedImage = new SquashedImage();
        squashedImage.setFileOperations(fileOperations);
        squashedImage.setDockerClientManager(dockerClientManager);
        output.setSquashedImage(squashedImage);

        final OutputFiles outputFiles = output.addOutputToFinalOutputDir(bdioDoc, null, null);

        final File generatedSquashedImageCompressedFile = outputFiles.getSquashedImageFile();
        final File generatedSquashedImageTarfile = new File(workingDir, "generatedImageTarfile");
        CompressedFile.gunZipFile(generatedSquashedImageCompressedFile, generatedSquashedImageTarfile);
        final File generatedSquashedImageContents = new File(workingDir, "generatedSquashedImageContents");
        CompressedFile.unTarFile(generatedSquashedImageTarfile, generatedSquashedImageContents);
        System.out.println(String.format("Look in: %s", generatedSquashedImageContents.getAbsolutePath()));
        final Collection<File> layerFiles = FileUtils.listFiles(generatedSquashedImageContents, new NameFileFilter("layer.tar"), TrueFileFilter.TRUE);
        assertEquals(1, layerFiles.size());
        final File generatedLayer = new File(workingDir, "generatedLayer");
        CompressedFile.unTarFile(layerFiles.iterator().next(), generatedLayer);
        final File expectedFile = new File(generatedLayer, "opt/luciddg-server/modules/django/bin/100_assets.csv");
        assertTrue(expectedFile.exists());
    }
}
