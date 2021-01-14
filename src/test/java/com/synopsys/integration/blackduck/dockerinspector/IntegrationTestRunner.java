package com.synopsys.integration.blackduck.dockerinspector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.synopsys.integration.bdio.BdioReader;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.blackduck.dockerinspector.config.ProgramPaths;
import com.synopsys.integration.blackduck.dockerinspector.output.Result;
import com.synopsys.integration.exception.IntegrationException;

public class IntegrationTestRunner {
    private final CommandCreator commandCreator;

    public IntegrationTestRunner(CommandCreator commandCreator) {
        this.commandCreator = commandCreator;
    }

    public void testImage(String detectJarPath, TestConfig testConfig)
        throws IOException, InterruptedException, IntegrationException {
        String inspectTargetArg = null;
        File outputContainerFileSystemFile = null;
        if (testConfig.getInspectTargetImageRepoTag() != null) {
            outputContainerFileSystemFile = getOutputContainerFileSystemFileFromImageSpec(testConfig.getInspectTargetImageRepoTag());
            inspectTargetArg = String.format("--docker.image=%s", testConfig.getInspectTargetImageRepoTag());
        } else if (testConfig.getInspectTargetImageId() != null) {
            inspectTargetArg = String.format("--docker.image.id=%s", testConfig.getInspectTargetImageId());
        }

        TestUtils.ensureFileDoesNotExist(outputContainerFileSystemFile);
        File actualBdio = null;
        if (testConfig.getCodelocationName() != null) {
            if (testConfig.getMode() == TestConfig.Mode.DETECT) {
                actualBdio = new File(String.format(String.format("%s/blackduck/bdio/%s_bdio.jsonld", System.getProperty("user.home"), testConfig.getCodelocationName())));
            } else {
                actualBdio = new File(String.format(String.format("%s/output/%s_bdio.jsonld", TestUtils.TEST_DIR_REL_PATH, testConfig.getCodelocationName())));
            }
            TestUtils.ensureFileDoesNotExist(actualBdio);
        }

        List<String> cmd = commandCreator.createCmd(testConfig.getMode(), detectJarPath, inspectTargetArg, testConfig.getTargetRepo(), testConfig.getTargetTag(), testConfig.getCodelocationName(),
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

    public void testTar(String detectJarPath,
        TestConfig testConfig)
        throws IOException, InterruptedException, IntegrationException {

        File targetTarFile;
        if (testConfig.getTargetTarInSharedDir() != null) {
            targetTarFile = testConfig.getTargetTarInSharedDir();
        } else {
            targetTarFile = new File(testConfig.getTarFilePath());
        }

        String inspectTargetArg = String.format("--docker.tar=%s", targetTarFile);
        TestUtils.ensureFileDoesNotExist(testConfig.getOutputContainerFileSystemFile());
        if (testConfig.getOutputSquashedImageFile() != null) {
            TestUtils.ensureFileDoesNotExist(testConfig.getOutputSquashedImageFile());
        }

        File actualBdio;
        if (testConfig.getMode() == TestConfig.Mode.DETECT) {
            actualBdio = new File(String.format("%s/blackduck/bdio/%s_bdio.jsonld", System.getProperty("user.home"), testConfig.getCodelocationName()));
        } else {
            actualBdio = new File(String.format("%s/output/%s_bdio.jsonld", TestUtils.TEST_DIR_REL_PATH, testConfig.getCodelocationName()));
        }
        TestUtils.ensureFileDoesNotExist(actualBdio);

        List<String> cmd = commandCreator.createCmd(testConfig.getMode(), detectJarPath, inspectTargetArg, testConfig.getTargetRepo(), testConfig.getTargetTag(), testConfig.getCodelocationName(),
            testConfig.getAdditionalArgs());
        System.out.printf("Running end to end test on %s with command %s\n", targetTarFile, cmd.toString());
        TestUtils.execCmd(String.join(" ", cmd), 240000L, true, testConfig.getEnv());
        System.out.println("blackduck-docker-inspector done; verifying results...");
        System.out.printf("Expecting output BDIO file: %s\n", actualBdio.getAbsolutePath());
        assertTrue(String.format("%s does not exist", actualBdio.getAbsolutePath()), actualBdio.exists());
        if (testConfig.isRequireBdioMatch()) {
            File expectedBdio = new File(
                String.format("src/test/resources/bdio/%s_bdio.jsonld", testConfig.getCodelocationName()));
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
            boolean componentFound = isComponentFound(doc, testConfig.getOutputBomMustContainComponentPrefix());
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

    private boolean isComponentFound(SimpleBdioDocument doc, String outputBomMustContainComponentPrefix) {
        for (int i = 0; i < doc.getComponents().size(); i++) {
            System.out.printf("\tComponent: %s / %s\n", doc.getComponents().get(i).name, doc.getComponents().get(i).version);
            if (doc.getComponents().get(i).name.startsWith(outputBomMustContainComponentPrefix)) {
                return true;
            }
        }
        return false;
    }

    private SimpleBdioDocument createBdioDocumentFromFile(File bdioFile) throws IOException {
        InputStream reader = new ByteArrayInputStream(FileUtils.readFileToByteArray(bdioFile));
        SimpleBdioDocument doc = null;
        try (BdioReader bdioReader = new BdioReader(new Gson(), reader)) {
            doc = bdioReader.readSimpleBdioDocument();
            return doc;
        }
    }

    private File getOutputContainerFileSystemFileFromImageSpec(String imageNameTag) {
        String path = String.format("%s/output/%s", TestUtils.TEST_DIR_REL_PATH, getContainerFileSystemTarFilenameFromImageRepoTag(imageNameTag));
        System.out.println(String.format("Expecting output container filesystem file at: %s", path));
        return new File(path);
    }

    private String getContainerFileSystemTarFilenameFromImageRepoTag(String givenImageRepoTag) {
        String adjustedImageRepoTag;
        if (givenImageRepoTag.contains(":")) {
            adjustedImageRepoTag = givenImageRepoTag;
        } else {
            adjustedImageRepoTag = String.format("%s:latest", givenImageRepoTag);
        }
        return String.format("%s_containerfilesystem.tar.gz", cleanImageName(adjustedImageRepoTag));
    }

    private String cleanImageName(String imageName) {
        return colonsToUnderscores(slashesToUnderscore(imageName));
    }

    private String colonsToUnderscores(String imageName) {
        return imageName.replaceAll(":", "_");
    }

    private String slashesToUnderscore(String givenString) {
        return givenString.replaceAll("/", "_");
    }
}
