package com.synopsys.integration.blackduck.dockerinspector;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;

public class CommandCreator {
    private static final int START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE = 8100;
    private static final int START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS = 8101;
    private static final int START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU = 8102;
    private final Random random;
    private final ProgramVersion programVersion;
    private final String configuredAlternateJavaCmd;

    public CommandCreator(Random random, ProgramVersion programVersion, String configuredAlternateJavaCmd) {
        this.random = random;
        this.programVersion = programVersion;
        this.configuredAlternateJavaCmd = configuredAlternateJavaCmd;
    }

    public List<String> createCmd(TestConfig.Mode mode, String detectJarPath, String inspectTargetArg, String repo, String tag,
        String codelocationName, List<String> additionalArgs) {
        if (mode == TestConfig.Mode.DETECT) {
            return createDetectCmd(detectJarPath, inspectTargetArg, repo, tag, codelocationName, additionalArgs);
        } else {
            return createDockerInspectorCmd(mode, inspectTargetArg, repo, tag, codelocationName, additionalArgs);
        }
    }

    private List<String> createDetectCmd(String detectJarPath, String inspectTargetArg, String repo, String tag,
        String codelocationName, List<String> additionalArgs) {
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

    public List<String> createSimpleDockerInspectorScriptCmd(List<String> args) {
        List<String> cmd = new ArrayList<>();

        cmd.add("build/blackduck-docker-inspector.sh");
        cmd.add(String.format("--jar.path=build/libs/blackduck-docker-inspector-%s.jar", programVersion.getProgramVersion()));
        if (args != null) {
            cmd.addAll(args);
        }
        return cmd;
    }

    private List<String> createDockerInspectorCmd(TestConfig.Mode mode, String inspectTargetArg, String repo, String tag,
        String codelocationName, List<String> additionalArgs) {
        List<String> cmd = new ArrayList<>();
        if (random.nextBoolean()) {
            System.out.println("The coin toss chose to run the script");
            cmd.add("build/blackduck-docker-inspector.sh");
            cmd.add(String.format("--jar.path=build/libs/blackduck-docker-inspector-%s.jar", programVersion.getProgramVersion()));
        } else {
            System.out.println("The coin toss chose to run the jar");
            String javaCmd = "java";
            if (random.nextBoolean()) {
                System.out.println("Will use alternate java command if configured");
                if (StringUtils.isNotBlank(configuredAlternateJavaCmd)) {
                    System.out.printf("Will use alternate java command: %s\n", configuredAlternateJavaCmd);
                    javaCmd = configuredAlternateJavaCmd;
                } else {
                    System.out.printf("No alternate java command is configured; defaulting to %s\n", javaCmd);
                }
            }
            cmd.add(javaCmd);
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
}
