package com.synopsys.integration.blackduck.dockerinspector.output;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SquashedImage {
    private static final Logger logger = LoggerFactory.getLogger(SquashedImage.class);

    public static void createSquashedImageTarGz(final File targetImageFileSystemTarGz, final File squashedImageTarGz) throws IOException {
        logger.info(String.format("Transforming container filesystem %s to squashed image %s", targetImageFileSystemTarGz, squashedImageTarGz));

//        CompressedFile.gunZipUnTarFile(targetImageFileSystemTarGz, tempTarFile, destinationDir);
//        final File dockerfileDir = targetImageFileSystemRootDir.getParentFile();
//        final File dockerfile = new File(dockerfileDir, "Dockerfile");
//        final String dockerfileContents = String.format("FROM scratch\nCOPY %s .", targetImageFileSystemRootDir.getName());
//        FileUtils.writeStringToFile(dockerfile, dockerfileContents, StandardCharsets.UTF_8);
        // TODO: docker build -t repo:tag .
    }
}
