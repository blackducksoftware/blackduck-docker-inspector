package com.blackducksoftware.integration.hub.docker;


import static org.junit.Assert.*

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import com.blackducksoftware.integration.hub.docker.dockerclient.DockerClientManager
import com.blackducksoftware.integration.hub.docker.help.formatter.UsageFormatter
import com.blackducksoftware.integration.hub.docker.hubclient.HubClient
import com.blackducksoftware.integration.hub.docker.imageinspector.ImageInspector
import com.blackducksoftware.integration.hub.docker.imageinspector.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.imageinspector.config.Config
import com.blackducksoftware.integration.hub.docker.imageinspector.config.ProgramPaths
import com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.manifest.ManifestLayerMapping
import com.blackducksoftware.integration.hub.docker.imageinspector.result.ResultFile
import com.google.gson.Gson

class ApplicationTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testNeedDifferentContainer() {
        doTest(true, "alpine", OperatingSystemEnum.ALPINE, false)
    }

    private void doTest(boolean onHost, String targetImageName, OperatingSystemEnum targetOsEnum, boolean expectBdioUpload) {
        Config config = [
            isOnHost: { true },
            isDryRun: { false },
            getLinuxDistro: { "" },
            getDockerTar: { "" },
            getDockerImage: { targetImageName },
            getDockerImageId: { "" },
            getDockerTar: { "" },
            getTargetImageName: { "" },
            getDockerImageRepo: { targetImageName },
            getDockerImageTag : { "" },
            getHubUrl: { "test prop public string value" },
            setDockerImageRepo: {},
            setDockerImageTag: {},
            getInspectorRepository: { "blackducksoftware" }
        ] as Config;

        DockerEnvImageInspector app = new DockerEnvImageInspector()
        app.config = config
        app.dockerImages = new InspectorImages()
        app.dockerImages.config = config
        ProgramVersion mockedProgramVersion = [
            getProgramVersion: { '1.2.3' }
        ] as ProgramVersion
        app.dockerImages.programVersion = mockedProgramVersion
        File tmpDir = TestUtils.createTempDirectory()
        File outputDir = new File(tmpDir, "output")
        File workingDir = new File(tmpDir, "working")
        ProgramPaths mockedProgramPaths = [
            getHubDockerOutputJsonPath: outputDir.getAbsolutePath(),
            getHubDockerWorkingDirPath: workingDir.getAbsolutePath()
        ] as ProgramPaths
        app.programPaths = mockedProgramPaths
        app.resultFile = [
            write: { final Gson gson, final boolean succeeded, final String msg, final OperatingSystemEnum targetOs, final String imageName, final String imageTag, final String dockerTarfilename, final String bdioFilename ->
            }
        ] as ResultFile

        app.hubClient = [
            isValid: { true },
            assertValid: {},
            phoneHome: {},
            testHubConnection: {},
            uploadBdioToHub: { File bdioFile ->
            }
        ] as HubClient

        List<File> layerTarFiles = new ArrayList<>()
        File tarFile = new File("src/test/resources/simple/layer.tar")
        layerTarFiles.add(tarFile)

        List<File> bdioFiles = new ArrayList<>()
        File bdioDir = TestUtils.createTempDirectory()
        File bdioFile = new File(bdioDir, "test.jsonld")
        bdioFile.createNewFile()
        bdioFiles.add(bdioFile)

        boolean uploadedBdioFiles = false
        boolean invokedSubContainer = false

        // TODO extractManifestFileContent value is set twice
        app.imageInspector = [
            init: { },
            extractLayerTars: { File dockerTar -> layerTarFiles },
            cleanWorkingDirectory: {},
            generateBdioFromPackageMgrDirs: {null},
            deriveDockerTarFile: {null},
            getTarFileFromDockerImage: {String imageName, String tagName -> new File("src/test/resources/image.tar")},
            extractDockerLayers: {List<File> layerTars, List<ManifestLayerMapping> layerMappings -> new File ("test")},
            detectOperatingSystem: {String operatingSystem -> targetOsEnum},
            detectCurrentOperatingSystem: {OperatingSystemEnum.UBUNTU},
            generateBdioFromImageFilesDir: {String imageRepo, String imageTag, List<ManifestLayerMapping> mappings, String projectName, String versionName, File dockerTar, File imageFilesDir, OperatingSystemEnum osEnum -> bdioFiles},
            extractManifestFileContent: {String tarFileName -> "[{\"Config\":\"ebcd9d4fca80e9e8afc525d8a38e7c56825dfb4a220ed77156f9fb13b14d4ab7.json\",\"RepoTags\":[\"${targetImageName}\"],\"Layers\":[\"68f9022b99e55f4856423cfcdc874e788299fc6147742a5551e10a62e8f2d521/layer.tar\",\"7cc064e7bb40e1237c51dbdf1a2a1d5c533ed67a27936b374adc87886da98a52/layer.tar\",\"09918b7293542c699c6b0c18c7c921472e574e279d42f43af2d70ec9d99dc608/layer.tar\",\"a9ee6ee9019fc117b5290d8f461d0312ff5c06e30b11cc96e43971573918f1b9/layer.tar\",\"9a9fb3763c4d41a028e3427fb412ce68d32a349b7b9fbd8c94eb967117f9b31d/layer.tar\"]}]"},
            uploadBdioFiles: {List<File> bdioFilesToUpload -> uploadedBdioFiles = true},
            extractManifestFileContent: {"[{\"Config\":\"0ef2e08ed3fabfc44002ccb846c4f2416a2135affc3ce39538834059606f32dd.json\",\"RepoTags\":[\"ubuntu:latest\"],\"Layers\":[\"cac2bb4c0c91ffa2821e9fecd2f8ccb32de91a33bfe079ae8c114170946c4dd2/layer.tar\",\"b5700afeeb6506d3e0387581b099aff4837d656276eb90afd4a53c72902ef00d/layer.tar\",\"8e985b9f58683665e5f66dc569e4eac9dc35c6d5cb9c00476c9860854df08308/layer.tar\",\"f2f739cbcf40cde68032e30fead7c9fb894db4210be3d2682f6950c7f1033be4/layer.tar\",\"7ec9b78a3372f84778c8b023dc9029848bb078674d49ae569e0a2663bfd1fe9f/layer.tar\"]}]"},
            getLayerMappings: {String tarFileName, String dockerImageName, String dockerTagName ->
                null},
            phoneHome: {
            },
            verifyHubConnection: {
            }
        ] as ImageInspector
        app.imageInspector.config = config

        app.programVersion = [
            getProgramVersion: {"1.2.3"}
        ] as ProgramVersion
        app.dockerClientManager = [
            getTarFileFromDockerImage: { String name, String tag -> new File("src/test/resources/simple/layer.tar") },
            getDockerEngineVersion: { -> "1" },
            pullImage: {String runOnImageName, String runOnImageVersion -> },
            run: {String runOnImageName, String runOnImageVersion, String runOnImageId, File dockerTarFile, boolean devMode, String image, String repo, String tag ->
                invokedSubContainer = true}
        ] as DockerClientManager

        final UsageFormatter helpGenerator = new UsageFormatter();
        helpGenerator.config = config;
        app.usageFormatter = helpGenerator

        app.inspectImage()
        if (expectBdioUpload) {
            assertTrue(uploadedBdioFiles)
            assertFalse(invokedSubContainer)
        } else {
            assertFalse(uploadedBdioFiles)
            assertTrue(invokedSubContainer)
        }
    }

}