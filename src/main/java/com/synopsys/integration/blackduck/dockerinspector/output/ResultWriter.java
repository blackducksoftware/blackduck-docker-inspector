/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.output;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class ResultWriter implements Closeable {
    private final Gson gson;
    private final JsonWriter jsonWriter;

    public ResultWriter(final Gson gson, final OutputStream outputStream) {
        this.gson = gson;
        this.jsonWriter = new JsonWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
        jsonWriter.setIndent("  ");
    }

    public void writeResult(final Result result) {
        gson.toJson(result, result.getClass(), jsonWriter);
    }

    @Override
    public void close() throws IOException {
        jsonWriter.close();
    }
}
