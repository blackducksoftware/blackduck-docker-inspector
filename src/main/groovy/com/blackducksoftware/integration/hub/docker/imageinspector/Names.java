package com.blackducksoftware.integration.hub.docker.imageinspector;

public class Names {
    // TODO This is part of the lib API... not sure this is ideal
    public static String getImageTarFilename(final String imageName, final String tagName) {
        return String.format("%s_%s.tar", imageName, tagName);
    }

    public static String getTargetImageFileSystemRootDirName(final String imageName, final String imageTag) {
        return String.format("image_%s_v_%s", imageName.replaceAll("/", "_"), imageTag);
    }
}
