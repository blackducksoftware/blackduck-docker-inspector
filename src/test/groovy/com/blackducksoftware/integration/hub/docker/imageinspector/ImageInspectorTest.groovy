package com.blackducksoftware.integration.hub.docker.imageinspector;


import static org.junit.Assert.*

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import com.blackducksoftware.integration.hub.docker.config.Config
import com.blackducksoftware.integration.hub.docker.config.DockerInspectorOption
import com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.DockerTarParser
import com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.ImageInfoParsed
import com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.ImagePkgMgr
import com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.manifest.ManifestLayerMapping
import com.blackducksoftware.integration.hub.docker.imageinspector.lib.ImageInfoDerived
import com.blackducksoftware.integration.hub.docker.imageinspector.lib.ImageInspector
import com.blackducksoftware.integration.hub.docker.imageinspector.lib.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.imageinspector.lib.PackageManagerEnum
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.FileOperations
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.executor.ApkExecutor
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.executor.DpkgExecutor
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.executor.Executor
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.extractor.ApkExtractor
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.extractor.DpkgExtractor
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.extractor.Extractor

class ImageInspectorTest {

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
        ImageInfoParsed imageInfo = new ImageInfoParsed("image_${imageName}_v_${tagName}", os, imagePkgMgr)

        List<Extractor> extractors = new ArrayList<>()
        extractor.executor = executor
        extractor.executor.init()
        extractor.init()
        extractors.add(extractor)

        ImageInspector imageInspector = new ImageInspector()
        imageInspector.tarParser = [
            setWorkingDirectory: { File f ->
            }
        ] as DockerTarParser
        String tempDirPath = TestUtils.createTempDirectory().getAbsolutePath()
        imageInspector.init(tempDirPath, tempDirPath, "")
        imageInspector.extractors = extractors



        List<File> etcDirs = new ArrayList<>()
        File etcDir = TestUtils.createTempDirectory()
        File etcApkDir = new File(etcDir, "apk")
        File etcApkArchFile = new File(etcApkDir, "arch")
        etcApkDir.mkdirs()
        etcApkArchFile.createNewFile()
        etcApkArchFile.write 'amd64'

        etcDirs.add(etcDir)
        imageInspector.tarParser = [
            collectPkgMgrInfo: {File imageFilesDir, OperatingSystemEnum osEnum -> imageInfo}
        ] as DockerTarParser

        FileOperations.metaClass.static.findFileWithName = {File fileToSearch, String name -> etcDirs}

        List<ManifestLayerMapping> mappings = new ArrayList<ManifestLayerMapping>()
        List<String> layerIds = new ArrayList<>()
        layerIds.add("testLayerId")
        ManifestLayerMapping mapping = new ManifestLayerMapping(imageName, tagName, layerIds)
        mappings.add(mapping)
        File imageFilesDir = new File("src/test/resources/imageDir")
        ImageInfoDerived imageInfoDerived = imageInspector.generateBdioFromImageFilesDir(imageName, tagName, mappings, "testProjectName", "testProjectVersion", imageTarFile, imageFilesDir, os)
        File bdioFile = imageInspector.writeBdioFile(imageInfoDerived)
        File file1 = new File("src/test/resources/${imageName}_imageDir_testProjectName_testProjectVersion_bdio.jsonld")
        File file2 = bdioFile
        println "Comparing ${file2.getAbsolutePath()} to ${file1.getAbsolutePath()}"
        boolean filesAreEqual = TestUtils.contentEquals(file1, file2, [
            "\"@id\":",
            "\"externalSystemTypeId\":",
            "_Users_"
        ])
        assertTrue(filesAreEqual)
    }
}