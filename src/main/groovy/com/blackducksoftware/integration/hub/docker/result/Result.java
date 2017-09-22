package com.blackducksoftware.integration.hub.docker.result;

public class Result {
    private final Boolean succeeded;
    private final String message;

    public Result(final Boolean succeeded, final String message) {
        this.succeeded = succeeded;
        this.message = message;
    }

    public Boolean getSucceeded() {
        return succeeded;
    }

    public String getMessage() {
        return message;
    }

}
