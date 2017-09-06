package com.blackducksoftware.integration.hub.docker.extractor;

import java.util.List;

import com.blackducksoftware.integration.hub.bdio.simple.model.BdioComponent;
import com.blackducksoftware.integration.hub.bdio.simple.model.DependencyNode;

public class ExtractionResults {
    final List<BdioComponent> components;
    final DependencyNode dependenciesRootNode;

    public ExtractionResults(final List<BdioComponent> components, final DependencyNode rootNode) {
        super();
        this.components = components;
        this.dependenciesRootNode = rootNode;
    }

    public List<BdioComponent> getComponents() {
        return components;
    }

    public DependencyNode getDependenciesRootNode() {
        return dependenciesRootNode;
    }
}
