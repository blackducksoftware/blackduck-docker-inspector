package com.blackducksoftware.integration.hub.docker;


import static org.junit.Assert.*

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import com.blackducksoftware.integration.hub.docker.imageinspector.ImageInspector
import com.blackducksoftware.integration.hub.docker.imageinspector.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.imageinspector.PackageManagerEnum
import com.blackducksoftware.integration.hub.docker.imageinspector.config.Config
import com.blackducksoftware.integration.hub.docker.imageinspector.config.DockerInspectorOption
import com.blackducksoftware.integration.hub.docker.imageinspector.config.ProgramPaths
import com.blackducksoftware.integration.hub.docker.imageinspector.hub.HubClient
import com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.DockerTarParser
import com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.ImageInfo
import com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.ImagePkgMgr
import com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.manifest.ManifestLayerMapping
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.FileOperations
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.executor.ApkExecutor
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.executor.DpkgExecutor
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.executor.Executor
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.extractor.ApkExtractor
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.extractor.DpkgExtractor
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.extractor.Extractor

class HubDockerManagerTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testDpkg() {
        String[] packages = new File("src/test/resources/ubuntu_dpkg_output_1.txt")
        Executor executor = [
            listPackages: {-> packages},
            runPackageManager: {ImagePkgMgr imagePkgMgr-> packages}
        ] as DpkgExecutor
        doTest("ubuntu", "1.0", OperatingSystemEnum.UBUNTU, PackageManagerEnum.DPKG, new DpkgExtractor(), executor)
    }

    @Test
    public void testApk() {
        String[] packages = new File("src/test/resources/alpine_apk_output_1.txt")
        Executor executor = [
            listPackages: {-> packages},
            runPackageManager: {ImagePkgMgr imagePkgMgr-> packages}
        ] as ApkExecutor
        doTest("alpine", "1.0", OperatingSystemEnum.ALPINE, PackageManagerEnum.APK, new ApkExtractor(), executor)
    }

    private void doTest(String imageName, String tagName, OperatingSystemEnum os, PackageManagerEnum pkgMgr, Extractor extractor, Executor executor) {
        List<DockerInspectorOption> configOptions = new ArrayList<>();
        configOptions.add(new DockerInspectorOption("hub.url", "hubUrl", "testHubUrl", "Hub URL", String.class, "", Config.GROUP_PUBLIC, false));
        Config config = [
            isOnHost: { true },
            isDryRun: { false },
            getLinuxDistro: { "" },
            getDockerTar: { "" },
            getDockerImage: { targetImageName },
            getDockerImageId: { "" },
            getTargetImageName: { "" },
            getDockerImageRepo: { targetImageName },
            getDockerImageTag : { "" },
            getHubUrl: { "test prop public string value" },
            setDockerImageRepo: {},
            setJarPath: {},
            getJarPath: { "/tmp/t.jar" },
            getHubCodelocationPrefix: { "" },
            setHubCodelocationPrefix: { },
            setDockerImageTag: {
            },
            getHubUrl: { "testHubUrl" },
            getPublicConfigOptions: { configOptions }
        ] as Config;

        File imageTarFile = new File("test/image.tar")
        ImagePkgMgr imagePkgMgr = new ImagePkgMgr(new File("test/resources/imageDir/image_${imageName}_v_${tagName}/${pkgMgr.directory}"), pkgMgr)
        ImageInfo imageInfo = new ImageInfo("image_${imageName}_v_${tagName}", os, imagePkgMgr)

        List<Extractor> extractors = new ArrayList<>()
        extractor.executor = executor
        extractor.executor.init()
        extractor.init()
        extractors.add(extractor)

        ImageInspector mgr = new ImageInspector()
        String tempDirPath = TestUtils.createTempDirectory().getAbsolutePath()
        mgr.programPaths = [
            getHubDockerWorkingDirPath: { -> tempDirPath },
            getHubDockerOutputPath: { -> tempDirPath },
            getHubDockerOutputPathContainer: { -> tempDirPath }
        ] as ProgramPaths
        mgr.programPaths.config = config;
        mgr.hubClient = [
        ] as HubClient

        mgr.extractors = extractors



        List<File> etcDirs = new ArrayList<>()
        File etcDir = TestUtils.createTempDirectory()
        File etcApkDir = new File(etcDir, "apk")
        File etcApkArchFile = new File(etcApkDir, "arch")
        etcApkDir.mkdirs()
        etcApkArchFile.createNewFile()
        etcApkArchFile.write 'amd64'

        etcDirs.add(etcDir)
        mgr.tarParser = [
            collectPkgMgrInfo: {File imageFilesDir, OperatingSystemEnum osEnum -> imageInfo}
        ] as DockerTarParser

        FileOperations.metaClass.static.findFileWithName = {File fileToSearch, String name -> etcDirs}

        List<ManifestLayerMapping> mappings = new ArrayList<ManifestLayerMapping>()
        List<String> layerIds = new ArrayList<>()
        layerIds.add("testLayerId")
        ManifestLayerMapping mapping = new ManifestLayerMapping(imageName, tagName, layerIds)
        mapping.programPaths = new ProgramPaths()
        mappings.add(mapping)
        File imageFilesDir = new File("src/test/resources/imageDir")
        List<File> bdioFiles = mgr.generateBdioFromImageFilesDir("root", "1.0", mappings, "testProjectName", "testProjectVersion", imageTarFile, imageFilesDir, os)
        for (File bdioFile : bdioFiles) {
            println "${bdioFile.getAbsolutePath()}"
        }

        File file1 = new File("src/test/resources/${imageName}_imageDir_testProjectName_testProjectVersion_bdio.jsonld")
        File file2 = bdioFiles.get(0)
        println "Comparing ${file2.getAbsolutePath()} to ${file1.getAbsolutePath()}"
        boolean filesAreEqual = TestUtils.contentEquals(file1, file2, [
            "\"@id\":",
            "\"externalSystemTypeId\":",
            "_Users_"
        ])
        assertTrue(filesAreEqual)
    }
}