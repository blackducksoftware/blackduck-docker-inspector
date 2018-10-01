package com.synopsys.integration.blackduck.dockerinspector;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.imageinspector.name.Names;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.BdioReader;
import com.synopsys.integration.hub.bdio.model.SimpleBdioDocument;

public class IntegrationTestCommon {

    private static int START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE = 8100;
    private static int START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS = 8101;
    public static int START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU = 8102;

    public static void testImage(final ProgramVersion programVersion, final String inspectTargetImageRepoTag, final String repo, final String tag, final String bdioFilename, final boolean requireBdioMatch,
            final boolean startContainersAsNeeded, final boolean forgeDerivedFromDistro,
            final String outputBomMustContainComponentPrefix, final int minNumberOfComponentsExpected, final List<String> additionalArgs, final Map<String, String> givenEnv)
            throws IOException, InterruptedException, IntegrationException {
        final File outputContainerFileSystemFile = getOutputContainerFileSystemFileFromImageSpec(inspectTargetImageRepoTag);
        final String inspectTargetArg = String.format("--docker.image=%s", inspectTargetImageRepoTag);
        ensureFileDoesNotExist(outputContainerFileSystemFile);
        final File actualBdio = new File(String.format(String.format("%s/output/%s", TestUtils.TEST_DIR_REL_PATH, bdioFilename)));
        ensureFileDoesNotExist(actualBdio);

        final List<String> cmd = new ArrayList<>();
        cmd.add("build/blackduck-docker-inspector.sh");
        cmd.add("--upload.bdio=false");
        cmd.add(String.format("--jar.path=build/libs/blackduck-docker-inspector-%s.jar", programVersion.getProgramVersion()));
        cmd.add(String.format("--output.path=%s/output", TestUtils.TEST_DIR_REL_PATH));
        cmd.add("--output.include.containerfilesystem=true");
        cmd.add("--blackduck.always.trust.cert=true");
        if (repo != null) {
            cmd.add(String.format("--docker.image.repo=%s", repo));
        }
        if (tag != null) {
            cmd.add(String.format("--docker.image.tag=%s", tag));
        }
        cmd.add("--logging.level.com.synopsys=DEBUG");
        if (startContainersAsNeeded) {
            // --imageinspector.service.start=true is left to default (true)
            cmd.add(String.format("--imageinspector.service.port.alpine=%d", START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE));
            cmd.add(String.format("--imageinspector.service.port.centos=%d", START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS));
            cmd.add(String.format("--imageinspector.service.port.ubuntu=%d", START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU));
            cmd.add(String.format("--shared.dir.path.local=%s/containerShared", TestUtils.TEST_DIR_REL_PATH));
        } else {
            cmd.add("--imageinspector.service.start=false");
            final File workingDir = new File(String.format("%s/endToEnd", TestUtils.TEST_DIR_REL_PATH));
            TestUtils.deleteDirIfExists(workingDir);
            cmd.add(String.format("--working.dir.path=%s", workingDir.getAbsolutePath()));
        }
        if (forgeDerivedFromDistro) {
            cmd.add("--bdio.component.forge.derived.from.distro=true");
        }
        cmd.add(inspectTargetArg);
        if (additionalArgs != null) {
            cmd.addAll(additionalArgs);
        }

        System.out.println(String.format("Running end to end test on %s with command %s", inspectTargetImageRepoTag, cmd.toString()));
        TestUtils.execCmd(String.join(" ", cmd), 30000L, true, givenEnv);
        System.out.println("blackduck-docker-inspector done; verifying results...");
        System.out.printf("Expecting output BDIO file: %s\n", actualBdio.getAbsolutePath());
        assertTrue(actualBdio.exists());
        if (requireBdioMatch) {
            final File expectedBdio = new File(String.format(String.format("src/test/resources/bdio/%s", bdioFilename)));
            final List<String> exceptLinesContainingThese = new ArrayList<>();
            exceptLinesContainingThese.add("\"@id\":");
            exceptLinesContainingThese.add("spdx:created");
            exceptLinesContainingThese.add("Tool:");
            final boolean outputBdioMatches = TestUtils.contentEquals(expectedBdio, actualBdio, exceptLinesContainingThese);
            assertTrue(outputBdioMatches);
        }
        if (StringUtils.isNotBlank(outputBomMustContainComponentPrefix)) {
            boolean componentFound = false;
            final SimpleBdioDocument doc = createBdioDocumentFromFile(actualBdio);
            assertTrue(doc.components.size() >= minNumberOfComponentsExpected);
            for (int i = 0; i < doc.components.size(); i++) {
                System.out.printf("\tComponent: %s / %s\n", doc.components.get(i).name, doc.components.get(i).version);
                if (doc.components.get(i).name.startsWith(outputBomMustContainComponentPrefix)) {
                    componentFound = true;
                }
            }
            assertTrue(componentFound);
        }
        assertTrue(outputContainerFileSystemFile.exists());
    }

    public static void testTar(final ProgramVersion programVersion, final String inspectTargetTarfile, final String bdioFilename, final String repo, final String tag,
            final boolean requireBdioMatch,
            final boolean testStartServiceModeUsingCustomPorts,
            final List<String> additionalArgs, final boolean needWorkingDir, final File outputContainerFileSystemFile, final Map<String, String> givenEnv)
            throws IOException, InterruptedException, IntegrationException {

        final String inspectTargetArg = String.format("--docker.tar=%s", inspectTargetTarfile);

        ensureFileDoesNotExist(outputContainerFileSystemFile);

        final File actualBdio = new File(
                String.format(String.format("%s/output/%s", TestUtils.TEST_DIR_REL_PATH, bdioFilename)));
        ensureFileDoesNotExist(actualBdio);

        final List<String> cmd = new ArrayList<>();
        cmd.add("build/blackduck-docker-inspector.sh");
        cmd.add("--upload.bdio=false");
        cmd.add(String.format("--jar.path=build/libs/blackduck-docker-inspector-%s.jar", programVersion.getProgramVersion()));
        cmd.add(String.format("--output.path=%s/output", TestUtils.TEST_DIR_REL_PATH));
        cmd.add("--output.include.containerfilesystem=true");
        cmd.add("--blackduck.always.trust.cert=true");
        if (repo != null) {
            cmd.add(String.format("--docker.image.repo=%s", repo));
        }
        if (tag != null) {
            cmd.add(String.format("--docker.image.tag=%s", tag));
        }
        cmd.add("--logging.level.com.synopsys=DEBUG");
        if (needWorkingDir) {
            final File workingDir = new File(String.format("%s/endToEnd", TestUtils.TEST_DIR_REL_PATH));
            TestUtils.deleteDirIfExists(workingDir);
            cmd.add(String.format("--working.dir.path=%s", workingDir.getAbsolutePath()));
        }
        cmd.add(inspectTargetArg);
        if (testStartServiceModeUsingCustomPorts) {
            // --imageinspector.service.start=true is left to default (true)
            cmd.add(String.format("--imageinspector.service.port.alpine=%d", START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE));
            cmd.add(String.format("--imageinspector.service.port.centos=%d", START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS));
            cmd.add(String.format("--imageinspector.service.port.ubuntu=%d", START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU));
            cmd.add(String.format("--shared.dir.path.local=%s/containerShared", TestUtils.TEST_DIR_REL_PATH));
        } else {
            cmd.add("--imageinspector.service.start=false");
        }
        if (additionalArgs != null && additionalArgs.size() > 0) {
            cmd.addAll(additionalArgs);
        }
        System.out.println(String.format("Running end to end test on %s with command %s", inspectTargetTarfile, cmd.toString()));
        TestUtils.execCmd(String.join(" ", cmd), 240000L, true, givenEnv);
        System.out.println("blackduck-docker-inspector done; verifying results...");
        System.out.printf("Expecting output BDIO file: %s\n", actualBdio.getAbsolutePath());
        assertTrue(actualBdio.exists());
        if (requireBdioMatch) {
            final File expectedBdio = new File(
                    String.format(String.format("src/test/resources/bdio/%s", bdioFilename)));
            final List<String> exceptLinesContainingThese = new ArrayList<>();
            exceptLinesContainingThese.add("\"@id\":");
            exceptLinesContainingThese.add("spdx:created");
            exceptLinesContainingThese.add("Tool:");
            final boolean outputBdioMatches = TestUtils.contentEquals(expectedBdio, actualBdio, exceptLinesContainingThese);
            assertTrue(outputBdioMatches);
        }

        assertTrue(String.format("%s does not exist", outputContainerFileSystemFile.getAbsolutePath()), outputContainerFileSystemFile.exists());
    }

    public static File getOutputContainerFileSystemFileFromTarFilename(final String tarFilename) {
        final String path = String.format("%s/output/%s", TestUtils.TEST_DIR_REL_PATH, Names.getContainerFileSystemTarFilename(null, tarFilename));
        System.out.println(String.format("Expecting output container filesystem file at: %s", path));
        return new File(path);
    }

    private static SimpleBdioDocument createBdioDocumentFromFile(final File bdioFile) throws IOException {
        final InputStream reader = new ByteArrayInputStream(FileUtils.readFileToByteArray(bdioFile));
        SimpleBdioDocument doc = null;
        try (BdioReader bdioReader = new BdioReader(new Gson(), reader)) {
            doc = bdioReader.readSimpleBdioDocument();
            return doc;
        }
    }

    private static File getOutputContainerFileSystemFileFromImageSpec(final String imageNameTag) {
        final String path = String.format("%s/output/%s", TestUtils.TEST_DIR_REL_PATH, Names.getContainerFileSystemTarFilename(imageNameTag, null));
        System.out.println(String.format("Expecting output container filesystem file at: %s", path));
        return new File(path);
    }

    private static void ensureFileDoesNotExist(final File outputContainerFileSystemFile) throws IOException {
        Files.deleteIfExists(outputContainerFileSystemFile.toPath());
        assertFalse(outputContainerFileSystemFile.exists());
    }
}
