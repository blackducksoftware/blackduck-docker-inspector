package com.blackducksoftware.integration.hub.docker.dependencynode;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import com.blackducksoftware.integration.hub.bdio.simple.model.DependencyNode;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

public class DependencyNodeWriter implements Closeable {
    private final Gson gson;
    private final JsonWriter jsonWriter;

    public DependencyNodeWriter(final Gson gson, final Writer writer) throws IOException {
        this.gson = gson;
        this.jsonWriter = new JsonWriter(writer);
        jsonWriter.setIndent("  ");
    }

    public DependencyNodeWriter(final Gson gson, final OutputStream outputStream) throws IOException {
        this.gson = gson;
        this.jsonWriter = new JsonWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
        jsonWriter.setIndent("  ");
    }

    public void writeDependencyNode(final DependencyNode dependencyNode) {
        gson.toJson(dependencyNode, dependencyNode.getClass(), jsonWriter);
    }

    @Override
    public void close() throws IOException {
        jsonWriter.close();
    }

}
