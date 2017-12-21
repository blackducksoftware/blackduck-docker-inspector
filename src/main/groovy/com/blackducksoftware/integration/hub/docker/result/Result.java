package com.blackducksoftware.integration.hub.docker.result;

public class Result {
    private final Boolean succeeded;
    private final String message;
    private final String runOnImageName;
    private final String runOnImageTag;

    public Result(final Boolean succeeded, final String message, final String runOnImageName, final String runOnImageTag) {
        this.succeeded = succeeded;
        this.message = message;
        this.runOnImageName = runOnImageName;
        this.runOnImageTag = runOnImageTag;
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
}
