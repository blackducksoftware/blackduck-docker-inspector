package com.blackducksoftware.integration.hub.docker.imageinspector.result;

public class Result {
    private final Boolean succeeded;
    private final String message;
    private final String inspectOnOsName;
    private final String inspectOnImageName;
    private final String inspectOnImageTag;
    private final String dockerTarfilename;
    private final String bdioFilename;

    public Result(final Boolean succeeded, final String message, final String inspectOnOsName, final String inspectOnImageName, final String inspectOnImageTag, final String dockerTarfilename, final String bdioFilename) {
        this.succeeded = succeeded;
        this.message = message;
        this.inspectOnOsName = inspectOnOsName == null ? "" : inspectOnOsName;
        this.inspectOnImageName = inspectOnImageName == null ? "" : inspectOnImageName;
        this.inspectOnImageTag = inspectOnImageTag == null ? "" : inspectOnImageTag;
        this.dockerTarfilename = dockerTarfilename == null ? "" : dockerTarfilename;
        this.bdioFilename = bdioFilename == null ? "" : bdioFilename;
    }

    public boolean isSucceeded() {
        return succeeded;
    }

    public String getMessage() {
        return message;
    }

    public String getInspectOnOsName() {
        return inspectOnOsName;
    }

    public String getInspectOnImageName() {
        return inspectOnImageName;
    }

    public String getInspectOnImageTag() {
        return inspectOnImageTag;
    }

    public String getDockerTarfilename() {
        return dockerTarfilename;
    }

    public String getBdioFilename() {
        return bdioFilename;
    }
}
