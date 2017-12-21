package com.blackducksoftware.integration.hub.docker.result;

public class Result {
    private final Boolean succeeded;
    private final String message;
    private final String runOnImageName;
    private final String runOnImageTag;
    private final String dockerTarfilename;
    private final String bdioFilename;

    public Result(final Boolean succeeded, final String message, final String runOnImageName, final String runOnImageTag, final String dockerTarfilename, final String bdioFilename) {
        this.succeeded = succeeded;
        this.message = message;
        this.runOnImageName = runOnImageName == null ? "" : runOnImageName;
        this.runOnImageTag = runOnImageTag == null ? "" : runOnImageTag;
        this.dockerTarfilename = dockerTarfilename == null ? "" : dockerTarfilename;
        this.bdioFilename = bdioFilename == null ? "" : bdioFilename;
    }

    public boolean isSucceeded() {
        return succeeded;
    }

    public String getMessage() {
        return message;
    }

    public String getRunOnImageName() {
        return runOnImageName;
    }

    public String getRunOnImageTag() {
        return runOnImageTag;
    }

    public String getDockerTarfilename() {
        return dockerTarfilename;
    }

    public String getBdioFilename() {
        return bdioFilename;
    }
}
