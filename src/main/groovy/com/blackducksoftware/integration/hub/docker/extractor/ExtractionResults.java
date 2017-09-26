package com.blackducksoftware.integration.hub.docker.extractor;

import java.util.List;

import com.blackducksoftware.integration.hub.bdio.simple.model.BdioComponent;

// TODO this class can now go away

public class ExtractionResults {
    final List<BdioComponent> components;

    public ExtractionResults(final List<BdioComponent> components) {
        super();
        this.components = components;
    }

    public List<BdioComponent> getComponents() {
        return components;
    }

}
