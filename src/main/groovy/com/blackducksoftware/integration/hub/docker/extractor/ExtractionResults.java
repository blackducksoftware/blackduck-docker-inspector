package com.blackducksoftware.integration.hub.docker.extractor;

import java.util.List;

import com.blackducksoftware.integration.hub.bdio.simple.model.BdioComponent;
import com.blackducksoftware.integration.hub.detect.model.DetectCodeLocation;

public class ExtractionResults {
    final List<BdioComponent> components;
    final DetectCodeLocation dependenciesRootNode;

    public ExtractionResults(final List<BdioComponent> components, final DetectCodeLocation rootNode) {
        super();
        this.components = components;
        this.dependenciesRootNode = rootNode;
    }

    public List<BdioComponent> getComponents() {
        return components;
    }

    public DetectCodeLocation getDependenciesRootNode() {
        return dependenciesRootNode;
    }
}
