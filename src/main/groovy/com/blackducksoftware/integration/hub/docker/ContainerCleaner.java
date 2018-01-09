package com.blackducksoftware.integration.hub.docker;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.hub.docker.dockerclient.DockerClientManager;

public class ContainerCleaner implements Callable<String> {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final boolean removeImage;
    private final DockerClientManager dockerClientManager;
    private final String imageId;
    private final String containerId;

    public ContainerCleaner(final DockerClientManager dockerClientManager, final String imageId, final String containerId, final boolean removeImage) {
        this.dockerClientManager = dockerClientManager;
        this.imageId = imageId;
        this.containerId = containerId;
        this.removeImage = removeImage;
    }

    @Override
    public String call() {
        String statusMessage = "Cleanup of container/image: Success";
        try {
            logger.info(String.format("Cleaning up container %s / image %s", containerId, imageId));
            dockerClientManager.stopRemoveContainer(containerId);
            if (removeImage) {
                dockerClientManager.removeImage(imageId);
            }
            logger.debug(statusMessage);
        } catch (final Throwable e) {
            statusMessage = String.format("Error during cleanup container %s / image %s: %s", containerId, imageId, e.getMessage());
            logger.debug(statusMessage);
        }
        return statusMessage;
    }
}
