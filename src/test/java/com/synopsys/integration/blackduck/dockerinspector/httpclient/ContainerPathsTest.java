package com.synopsys.integration.blackduck.dockerinspector.httpclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.blackduck.dockerinspector.ProcessId;
import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.ProgramPaths;

public class ContainerPathsTest {

    private static final String TARGET_FILE_PATH_LOCAL_WINDOWS = "C:\\Users\\Administrator\\blackduck-docker-inspector\\files\\shared\\run_1\\target\\alpine.tar";
    private static final String TARGET_FILE_PATH_LOCAL_LINUX = "/Users/Administrator/blackduck-docker-inspector/files/shared/run_1/target/alpine.tar";
    private static final String SHARED_DIR_PATH_LOCAL_LINUX = "/Users/Administrator/blackduck-docker-inspector/files/shared";
    private static final String SHARED_DIR_PATH_LOCAL_WINDOWS = "C:\\Users\\Administrator\\blackduck-docker-inspector\\files\\shared";
    private static final String SHARED_DIR_PATH_CONTAINER_LINUX = "/opt/blackduck-docker-inspector/shared";
    private static final String SHARED_DIR_PATH_CONTAINER_WINDOWS = "C:\\opt\\blackduck-docker-inspector\\shared";
    private static final String CONTAINER_PATH_TO_TARGET_FILE = "/opt/blackduck-docker-inspector/shared/run_1/target/alpine.tar";
    private static final String CONTAINER_PATH_TO_OUTPUT_FILE = "/opt/blackduck-docker-inspector/shared/run_1/output/test_out.tar";

    @Disabled
    @Test
    public void testLinux() throws IOException {
        String sharedDirPathLocal = SHARED_DIR_PATH_LOCAL_LINUX;
        String sharedDirPathContainer = SHARED_DIR_PATH_CONTAINER_LINUX;
        String targetFilePathLocal = TARGET_FILE_PATH_LOCAL_LINUX;

        doTest(sharedDirPathLocal, sharedDirPathContainer, targetFilePathLocal);
    }

    @Disabled
    @Test
    public void testWindows() throws IOException {
        String sharedDirPathLocal = SHARED_DIR_PATH_LOCAL_WINDOWS;
        String sharedDirPathContainer = SHARED_DIR_PATH_CONTAINER_WINDOWS;
        String targetFilePathLocal = TARGET_FILE_PATH_LOCAL_WINDOWS;

        doTest(sharedDirPathLocal, sharedDirPathContainer, targetFilePathLocal);
    }

    private void doTest(final String sharedDirPathLocal, final String sharedDirPathContainer,
        String targetFilePathLocal) throws IOException {
        Config config = Mockito.mock(Config.class);
        Mockito.when(config.getSharedDirPathLocal()).thenReturn(sharedDirPathLocal);
        Mockito.when(config.getSharedDirPathImageInspector()).thenReturn(sharedDirPathContainer);
        Mockito.when(config.getWorkingDirPath()).thenReturn(sharedDirPathLocal);
        ProcessId processId = Mockito.mock(ProcessId.class);
        Mockito.when(processId.addProcessIdToName("run")).thenReturn("run_1");
        ProgramPaths programPaths = new ProgramPaths(config, processId);
        ContainerPaths containerPaths = new ContainerPaths(config, programPaths);

        String containerPathToTargetFile = containerPaths.getContainerPathToTargetFile(targetFilePathLocal);
        assertEquals(CONTAINER_PATH_TO_TARGET_FILE, containerPathToTargetFile);

        String containerPathToOutputFile = containerPaths.getContainerPathToOutputFile("test_out.tar");
        assertEquals(CONTAINER_PATH_TO_OUTPUT_FILE, containerPathToOutputFile);
    }
}
