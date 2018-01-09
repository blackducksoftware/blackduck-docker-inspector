package com.blackducksoftware.integration.hub.docker.imageinspector.result;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

public class ResultWriter implements Closeable {
    private final Gson gson;
    private final JsonWriter jsonWriter;

    public ResultWriter(final Gson gson, final Writer writer) throws IOException {
        this.gson = gson;
        this.jsonWriter = new JsonWriter(writer);
        jsonWriter.setIndent("  ");
    }

    public ResultWriter(final Gson gson, final OutputStream outputStream) throws IOException {
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
