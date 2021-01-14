package com.synopsys.integration.blackduck.dockerinspector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.synopsys.integration.bdio.BdioReader;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.blackduck.dockerinspector.config.ProgramPaths;
import com.synopsys.integration.blackduck.dockerinspector.output.Result;
import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;
import com.synopsys.integration.exception.IntegrationException;

public class IntegrationTestCommon {
    private static final int START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE = 8100;
    private static final int START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS = 8101;
    public static int START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU = 8102;

    public static void testImage(Random random, ProgramVersion programVersion, String detectJarPath, TestConfig testConfig)
        throws IOException, InterruptedException, IntegrationException {
        String inspectTargetArg = null;
        File outputContainerFileSystemFile = null;
        if (testConfig.getInspectTargetImageRepoTag() != null) {
            outputContainerFileSystemFile = getOutputContainerFileSystemFileFromImageSpec(testConfig.getInspectTargetImageRepoTag());
            inspectTargetArg = String.format("--docker.image=%s", testConfig.getInspectTargetImageRepoTag());
        } else if (testConfig.getInspectTargetImageId() != null) {
            inspectTargetArg = String.format("--docker.image.id=%s", testConfig.getInspectTargetImageId());
        }

        ensureFileDoesNotExist(outputContainerFileSystemFile);
        File actualBdio = null;
        if (testConfig.getCodelocationName() != null) {
            if (testConfig.getMode() == TestConfig.Mode.DETECT) {
                actualBdio = new File(String.format(String.format("%s/blackduck/bdio/%s_bdio.jsonld", System.getProperty("user.home"), testConfig.getCodelocationName())));
            } else {
                actualBdio = new File(String.format(String.format("%s/output/%s_bdio.jsonld", TestUtils.TEST_DIR_REL_PATH, testConfig.getCodelocationName())));
            }
            ensureFileDoesNotExist(actualBdio);
        }

        List<String> cmd = createCmd(random, programVersion, testConfig.getMode(), detectJarPath, inspectTargetArg, testConfig.getTargetRepo(), testConfig.getTargetTag(), testConfig.getCodelocationName(),
            testConfig.getAdditionalArgs());

        System.out.println(String.format("Running end to end test on %s with command %s", testConfig.getInspectTargetImageRepoTag(), cmd.toString()));
        TestUtils.execCmd(String.join(" ", cmd), 30000L, true, testConfig.getEnv());
        System.out.println("blackduck-docker-inspector done; verifying results...");
        if (actualBdio != null) {
            System.out.printf("Expecting output BDIO file: %s\n", actualBdio.getAbsolutePath());
            assertTrue(actualBdio.exists());

            if (testConfig.isRequireBdioMatch()) {
                File expectedBdio = new File(String.format(String.format("src/test/resources/bdio/%s_bdio.jsonld", testConfig.getCodelocationName())));
                List<String> exceptLinesContainingThese = new ArrayList<>();
                exceptLinesContainingThese.add("\"@id\":");
                exceptLinesContainingThese.add("spdx:created");
                exceptLinesContainingThese.add("Tool:");
                exceptLinesContainingThese.add("kbSeparator");
                boolean outputBdioMatches = TestUtils.contentEquals(expectedBdio, actualBdio, exceptLinesContainingThese);
                assertTrue(outputBdioMatches);
            }
            SimpleBdioDocument doc = createBdioDocumentFromFile(actualBdio);
            assertTrue(doc.getComponents().size() >= testConfig.getMinNumberOfComponentsExpected());
            if (StringUtils.isNotBlank(testConfig.getOutputBomMustContainComponentPrefix())) {
                System.out.printf("Looking for component name starting with: %s\n", testConfig.getOutputBomMustContainComponentPrefix());
                boolean componentFound = false;
                componentFound = isComponentFound(doc, testConfig.getOutputBomMustContainComponentPrefix());
                assertTrue(componentFound);
                System.out.printf("Found it\n");
            }
            if (StringUtils.isNotBlank(testConfig.getOutputBomMustNotContainComponentPrefix())) {
                System.out.printf("Making sure there is no component name starting with: %s\n", testConfig.getOutputBomMustNotContainComponentPrefix());
                boolean componentFound = false;
                componentFound = isComponentFound(doc, testConfig.getOutputBomMustNotContainComponentPrefix());
                assertFalse(componentFound);
                System.out.printf("It's not there\n");
            }
            if (StringUtils.isNotBlank(testConfig.getOutputBomMustContainExternalSystemTypeId())) {
                System.out.printf("Looking for component with externalSystemTypeId: %s\n", testConfig.getOutputBomMustContainExternalSystemTypeId());
                boolean externalSystemTypeIdFound = false;
                for (int i = 0; i < doc.getComponents().size(); i++) {
                    System.out.printf("\tComponent: %s / %s; externalSystemTypeId: %s\n", doc.getComponents().get(i).name, doc.getComponents().get(i).version, doc.getComponents().get(i).bdioExternalIdentifier.forge);
                    if (doc.getComponents().get(i).bdioExternalIdentifier.forge.equals(testConfig.getOutputBomMustContainExternalSystemTypeId())) {
                        externalSystemTypeIdFound = true;
                        break;
                    }
                }
                assertTrue(externalSystemTypeIdFound);
                System.out.printf("Found it\n");
            }
        }
        if ((testConfig.getMode() != TestConfig.Mode.DETECT) && (outputContainerFileSystemFile != null)) {
            assertTrue(outputContainerFileSystemFile.exists());
        }
        if ((outputContainerFileSystemFile != null) && ((testConfig.getMinContainerFileSystemFileSize() > 0) || (testConfig.getMaxContainerFileSystemFileSize() > 0))) {
            long actualContainerFileSystemFileSize = outputContainerFileSystemFile.length();
            assertTrue(actualContainerFileSystemFileSize >= testConfig.getMinContainerFileSystemFileSize());
            assertTrue(actualContainerFileSystemFileSize <= testConfig.getMaxContainerFileSystemFileSize());
        }

        File resultsFile = new File(String.format(String.format("%s/output/%s", TestUtils.TEST_DIR_REL_PATH, ProgramPaths.RESULTS_JSON_FILENAME)));
        String resultsJsonString = FileUtils.readFileToString(resultsFile, StandardCharsets.UTF_8);
        Gson gson = new Gson();
        Result result = gson.fromJson(resultsJsonString, Result.class);
        assertTrue(result.isSucceeded());
        System.out.printf("results: %s", result.toString());

        if (testConfig.getInspectTargetImageRepoTag() != null) {
            if (testConfig.getInspectTargetImageRepoTag().contains(":")) {
                assertEquals(testConfig.getInspectTargetImageRepoTag(), String.format("%s:%s", result.getImageRepo(), result.getImageTag()));
            } else {
                assertEquals(testConfig.getInspectTargetImageRepoTag(), result.getImageRepo());
                assertEquals("latest", result.getImageTag());
            }
        } else {
            assertTrue(StringUtils.isNotBlank(result.getImageRepo()));
            assertTrue(StringUtils.isNotBlank(result.getImageTag()));
        }

        assertTrue(StringUtils.isNotBlank(result.getDockerTarfilename()));
        assertTrue(result.getDockerTarfilename().endsWith(".tar"));
        assertEquals(0, result.getReturnCode());
        assertTrue(result.getBdioFilename().endsWith(".jsonld"));
        if (testConfig.getOutputContainerFileSystemFile() != null) {
            assertTrue(result.getContainerFilesystemFilename().endsWith(".tar.gz"));
        }
        if (testConfig.isTestSquashedImageGeneration()) {
            assertTrue(result.getSquashedImageFilename().endsWith(".tar.gz"));
        }
    }

    private static boolean isComponentFound(SimpleBdioDocument doc, String outputBomMustContainComponentPrefix) {
        for (int i = 0; i < doc.getComponents().size(); i++) {
            System.out.printf("\tComponent: %s / %s\n", doc.getComponents().get(i).name, doc.getComponents().get(i).version);
            if (doc.getComponents().get(i).name.startsWith(outputBomMustContainComponentPrefix)) {
                return true;
            }
        }

        return false;
    }

    private static List<String> createCmd(Random random, ProgramVersion programVersion, TestConfig.Mode mode, String detectJarPath, String inspectTargetArg, String repo, String tag,
        String codelocationName, List<String> additionalArgs)
        throws IOException {
        if (mode == TestConfig.Mode.DETECT) {
            return createDetectCmd(programVersion, mode, detectJarPath, inspectTargetArg, repo, tag, codelocationName, additionalArgs);
        } else {
            return createDockerInspectorCmd(random, programVersion, mode, inspectTargetArg, repo, tag, codelocationName, additionalArgs);
        }
    }

    private static List<String> createDetectCmd(ProgramVersion programVersion, TestConfig.Mode mode, String detectJarPath, String inspectTargetArg, String repo, String tag,
        String codelocationName, List<String> additionalArgs)
        throws IOException {
        if (StringUtils.isBlank(detectJarPath)) {
            throw new UnsupportedOperationException("Detect jar path must be provided");
        }
        List<String> cmd = new ArrayList<>();
        cmd.add("java");
        cmd.add("-jar");
        cmd.add(detectJarPath);
        cmd.add(String.format("--detect.docker.inspector.path=build/libs/blackduck-docker-inspector-%s.jar", programVersion.getProgramVersion()));
        cmd.add("--blackduck.offline.mode=true");
        cmd.add(String.format("--detect.docker.passthrough.blackduck.codelocation.name=%s", codelocationName));
        cmd.add("--detect.blackduck.signature.scanner.disabled=true");
        if (repo != null) {
            cmd.add(String.format("--detect.docker.passthrough.docker.image.repo=%s", repo));
        }
        if (tag != null) {
            cmd.add(String.format("--detect.docker.passthrough.docker.image.tag=%s", tag));
        }
        cmd.add("--logging.level.com.blackducksoftware.integration=DEBUG");
        cmd.add("--detect.excluded.bom.tool.types=gradle");
        cmd.add("--detect.docker.passthrough.service.timeout=800000");
        cmd.add("--detect.docker.passthrough.command.timeout=800000");
        String adjustedTargetArg = inspectTargetArg.replace("--docker.", "--detect.docker.");
        cmd.add(adjustedTargetArg);

        if (additionalArgs != null) {
            for (String additionalArg : additionalArgs) {
                String adjustedArg = additionalArg.replace("--", "--detect.docker.passthrough.");
                cmd.add(adjustedArg);
            }
        }

        return cmd;
    }

    public static List<String> createSimpleDockerInspectorScriptCmd(ProgramVersion programVersion, List<String> args) {
        List<String> cmd = new ArrayList<>();

        cmd.add("build/blackduck-docker-inspector.sh");
        cmd.add(String.format("--jar.path=build/libs/blackduck-docker-inspector-%s.jar", programVersion.getProgramVersion()));
        if (args != null) {
            cmd.addAll(args);
        }
        return cmd;
    }

    private static List<String> createDockerInspectorCmd(Random random, ProgramVersion programVersion, TestConfig.Mode mode, String inspectTargetArg, String repo, String tag,
        String codelocationName, List<String> additionalArgs) throws IOException {
        List<String> cmd = new ArrayList<>();
        if (random.nextBoolean()) {
            cmd.add("build/blackduck-docker-inspector.sh");
            cmd.add(String.format("--jar.path=build/libs/blackduck-docker-inspector-%s.jar", programVersion.getProgramVersion()));
        } else {
            cmd.add("java");
            cmd.add("-jar");
            cmd.add(String.format("build/libs/blackduck-docker-inspector-%s.jar", programVersion.getProgramVersion()));
        }
        cmd.add("--upload.bdio=false");
        cmd.add(String.format("--blackduck.codelocation.name=%s", codelocationName));
        cmd.add(String.format("--output.path=%s/output", TestUtils.TEST_DIR_REL_PATH));
        cmd.add("--output.include.containerfilesystem=true");
        if (repo != null) {
            cmd.add(String.format("--docker.image.repo=%s", repo));
        }
        if (tag != null) {
            cmd.add(String.format("--docker.image.tag=%s", tag));
        }
        cmd.add("--logging.level.com.synopsys=DEBUG");
        cmd.add("--service.timeout=800000");
        cmd.add("--command.timeout=800000");
        if (mode == TestConfig.Mode.SPECIFY_II_DETAILS) {
            // --imageinspector.service.start=true is left to default (true)
            cmd.add(String.format("--imageinspector.service.port.alpine=%d", START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE));
            cmd.add(String.format("--imageinspector.service.port.centos=%d", START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS));
            cmd.add(String.format("--imageinspector.service.port.ubuntu=%d", START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU));
            cmd.add(String.format("--shared.dir.path.local=%s/containerShared", TestUtils.TEST_DIR_REL_PATH));
        } else if (mode == TestConfig.Mode.NO_SERVICE_START) {
            cmd.add("--imageinspector.service.start=false");
            File workingDir = new File(String.format("%s/endToEnd", TestUtils.TEST_DIR_REL_PATH));
            TestUtils.deleteDirIfExists(workingDir);
            cmd.add(String.format("--working.dir.path=%s", workingDir.getAbsolutePath()));
        } else if (mode == TestConfig.Mode.DEFAULT) {
            // Proceed with defaults (mostly)
            cmd.add(String.format("--shared.dir.path.local=%s/containerShared", TestUtils.TEST_DIR_REL_PATH));
        } else {
            throw new UnsupportedOperationException(String.format("Unexpected mode: %s", mode.toString()));
        }
        cmd.add(inspectTargetArg);
        if (additionalArgs != null) {
            cmd.addAll(additionalArgs);
        }

        return cmd;
    }

    public static void testTar(Random random, ProgramVersion programVersion, String detectJarPath,
        TestConfig testConfig)
        throws IOException, InterruptedException, IntegrationException {

        File targetTarFile;
        if (testConfig.getTargetTarInSharedDir() != null) {
            targetTarFile = testConfig.getTargetTarInSharedDir();
        } else {
            targetTarFile = new File(testConfig.getTarFilePath());
        }

        String inspectTargetArg = String.format("--docker.tar=%s", targetTarFile);
        ensureFileDoesNotExist(testConfig.getOutputContainerFileSystemFile());
        if (testConfig.getOutputSquashedImageFile() != null) {
            ensureFileDoesNotExist(testConfig.getOutputSquashedImageFile());
        }

        File actualBdio;
        if (testConfig.getMode() == TestConfig.Mode.DETECT) {
            actualBdio = new File(String.format(String.format("%s/blackduck/bdio/%s_bdio.jsonld", System.getProperty("user.home"), testConfig.getCodelocationName())));
        } else {
            actualBdio = new File(String.format(String.format("%s/output/%s_bdio.jsonld", TestUtils.TEST_DIR_REL_PATH, testConfig.getCodelocationName())));
        }
        ensureFileDoesNotExist(actualBdio);

        List<String> cmd = createCmd(random, programVersion, testConfig.getMode(), detectJarPath, inspectTargetArg, testConfig.getTargetRepo(), testConfig.getTargetTag(), testConfig.getCodelocationName(),
            testConfig.getAdditionalArgs());
        System.out.println(String.format("Running end to end test on %s with command %s", targetTarFile, cmd.toString()));
        TestUtils.execCmd(String.join(" ", cmd), 240000L, true, testConfig.getEnv());
        System.out.println("blackduck-docker-inspector done; verifying results...");
        System.out.printf("Expecting output BDIO file: %s\n", actualBdio.getAbsolutePath());
        assertTrue(String.format("%s does not exist", actualBdio.getAbsolutePath()), actualBdio.exists());
        if (testConfig.isRequireBdioMatch()) {
            File expectedBdio = new File(
                String.format(String.format("src/test/resources/bdio/%s_bdio.jsonld", testConfig.getCodelocationName())));
            List<String> exceptLinesContainingThese = new ArrayList<>();
            exceptLinesContainingThese.add("\"@id\":");
            exceptLinesContainingThese.add("spdx:name");
            exceptLinesContainingThese.add("spdx:created");
            exceptLinesContainingThese.add("Tool:");
            exceptLinesContainingThese.add("kbSeparator");
            boolean outputBdioMatches = TestUtils.contentEquals(expectedBdio, actualBdio, exceptLinesContainingThese);
            assertTrue("BDIO produced does not match expected BDIO", outputBdioMatches);
        }

        SimpleBdioDocument doc = createBdioDocumentFromFile(actualBdio);
        assertTrue(doc.getComponents().size() >= testConfig.getMinNumberOfComponentsExpected());
        if (StringUtils.isNotBlank(testConfig.getOutputBomMustContainComponentPrefix())) {
            System.out.printf("Looking for component name starting with: %s\n", testConfig.getOutputBomMustContainComponentPrefix());
            boolean componentFound = false;
            componentFound = isComponentFound(doc, testConfig.getOutputBomMustContainComponentPrefix());
            assertTrue(componentFound);
            System.out.printf("Found it\n");
        }
        if (StringUtils.isNotBlank(testConfig.getOutputBomMustNotContainComponentPrefix())) {
            System.out.printf("Making sure there is no component name starting with: %s\n", testConfig.getOutputBomMustNotContainComponentPrefix());
            boolean componentFound = false;
            componentFound = isComponentFound(doc, testConfig.getOutputBomMustNotContainComponentPrefix());
            assertFalse(componentFound);
            System.out.printf("It's not there\n");
        }
        if (StringUtils.isNotBlank(testConfig.getOutputBomMustContainExternalSystemTypeId())) {
            System.out.printf("Looking for component with externalSystemTypeId: %s\n", testConfig.getOutputBomMustContainExternalSystemTypeId());
            boolean externalSystemTypeIdFound = false;
            for (int i = 0; i < doc.getComponents().size(); i++) {
                System.out.printf("\tComponent: %s / %s; externalSystemTypeId: %s\n", doc.getComponents().get(i).name, doc.getComponents().get(i).version, doc.getComponents().get(i).bdioExternalIdentifier.forge);
                if (doc.getComponents().get(i).bdioExternalIdentifier.forge.equals(testConfig.getOutputBomMustContainExternalSystemTypeId())) {
                    externalSystemTypeIdFound = true;
                    break;
                }
            }
            assertTrue(externalSystemTypeIdFound);
            System.out.printf("Found it\n");
        }

        if (testConfig.getMode() != TestConfig.Mode.DETECT) {
            assertTrue(String.format("%s does not exist", testConfig.getOutputContainerFileSystemFile().getAbsolutePath()), testConfig.getOutputContainerFileSystemFile().exists());
        }
        if (testConfig.getOutputSquashedImageFile() != null) {
            assertTrue(String.format("%s does not exist", testConfig.getOutputSquashedImageFile().getAbsolutePath()), testConfig.getOutputSquashedImageFile().exists());
        }
        if ((testConfig.getMinContainerFileSystemFileSize() > 0) || (testConfig.getMaxContainerFileSystemFileSize() > 0)) {
            long actualContainerFileSystemFileSize = testConfig.getOutputContainerFileSystemFile().length();
            assertTrue(actualContainerFileSystemFileSize >= testConfig.getMinContainerFileSystemFileSize());
            assertTrue(actualContainerFileSystemFileSize <= testConfig.getMaxContainerFileSystemFileSize());
        }

        File resultsFile = new File(String.format(String.format("%s/output/%s", TestUtils.TEST_DIR_REL_PATH, ProgramPaths.RESULTS_JSON_FILENAME)));
        String resultsJsonString = FileUtils.readFileToString(resultsFile, StandardCharsets.UTF_8);
        Gson gson = new Gson();
        Result result = gson.fromJson(resultsJsonString, Result.class);
        assertTrue(result.isSucceeded());
        System.out.printf("results: %s", result.toString());
        assertTrue(StringUtils.isNotBlank(result.getDockerTarfilename()));
        assertTrue(result.getDockerTarfilename().endsWith(".tar"));
        assertEquals(0, result.getReturnCode());
        assertTrue(result.getBdioFilename().endsWith(".jsonld"));
        assertTrue(testConfig.getTarFilePath().endsWith(result.getDockerTarfilename()));
        if (testConfig.getOutputContainerFileSystemFile() != null) {
            assertTrue(result.getContainerFilesystemFilename().endsWith(".tar.gz"));
        }
        if (testConfig.isTestSquashedImageGeneration()) {
            assertTrue(result.getSquashedImageFilename().endsWith(".tar.gz"));
        }
    }

    public static File getOutputContainerFileSystemFileFromTarFilename(String tarFilename) {
        String path = String.format("%s/output/%s", TestUtils.TEST_DIR_REL_PATH, getContainerFileSystemTarFilenameFromTarFilename(tarFilename));
        System.out.println(String.format("Expecting output container filesystem file at: %s", path));
        return new File(path);
    }

    private static String getContainerFileSystemTarFilenameFromTarFilename(String tarFilename) {
        int finalPeriodIndex = tarFilename.lastIndexOf('.');
        return String.format("%s_containerfilesystem.tar.gz", tarFilename.substring(0, finalPeriodIndex));
    }

    private static SimpleBdioDocument createBdioDocumentFromFile(File bdioFile) throws IOException {
        InputStream reader = new ByteArrayInputStream(FileUtils.readFileToByteArray(bdioFile));
        SimpleBdioDocument doc = null;
        try (BdioReader bdioReader = new BdioReader(new Gson(), reader)) {
            doc = bdioReader.readSimpleBdioDocument();
            return doc;
        }
    }

    private static File getOutputContainerFileSystemFileFromImageSpec(String imageNameTag) {
        String path = String.format("%s/output/%s", TestUtils.TEST_DIR_REL_PATH, getContainerFileSystemTarFilenameFromImageRepoTag(imageNameTag));
        System.out.println(String.format("Expecting output container filesystem file at: %s", path));
        return new File(path);
    }

    private static String getContainerFileSystemTarFilenameFromImageRepoTag(String givenImageRepoTag) {
        String adjustedImageRepoTag;
        if (givenImageRepoTag.contains(":")) {
            adjustedImageRepoTag = givenImageRepoTag;
        } else {
            adjustedImageRepoTag = String.format("%s:latest", givenImageRepoTag);
        }
        return String.format("%s_containerfilesystem.tar.gz", cleanImageName(adjustedImageRepoTag));
    }

    private static String cleanImageName(String imageName) {
        return colonsToUnderscores(slashesToUnderscore(imageName));
    }

    private static String colonsToUnderscores(String imageName) {
        return imageName.replaceAll(":", "_");
    }

    private static String slashesToUnderscore(String givenString) {
        return givenString.replaceAll("/", "_");
    }

    private static void ensureFileDoesNotExist(File outputContainerFileSystemFile) throws IOException {
        if (outputContainerFileSystemFile == null) {
            return;
        }
        Files.deleteIfExists(outputContainerFileSystemFile.toPath());
        assertFalse(outputContainerFileSystemFile.exists());
    }
}
