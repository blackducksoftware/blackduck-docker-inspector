package com.blackducksoftware.integration.hub.linux


class ExtractionResults {
    private Map<String, Map<String, List<ExtractionResult>>> projectToVersionsWithComponents = [:]

    void addExtractionResult(ExtractionResult extractionResult) {
        def project = extractionResult.hubProjectName
        def version = extractionResult.hubProjectVersionName
        def components = extractionResult.bdioComponentDetailsList
        if (projectToVersionsWithComponents.containsKey(project)) {
            if (projectToVersionsWithComponents.get(project).containsKey(version)) {
                projectToVersionsWithComponents.get(project).get(version).addAll(components)
            } else {
                projectToVersionsWithComponents.get(project).put(version, components)
            }
        } else {
            Map<String, List<BdioComponentDetails>> versionToComponents = new HashMap<>()
            versionToComponents.put(version, components)
            projectToVersionsWithComponents.put(project, versionToComponents)
        }
    }

    List<ExtractionResult> getExtractionResults() {
        def extractionResults = []

        for (String project : projectToVersionsWithComponents.keySet()) {
            Map<String, List<BdioComponentDetails>> versionToComponents = projectToVersionsWithComponents.get(project)
            for (String version : versionToComponents.keySet()) {
                def components = versionToComponents.get(version)
                def extractionResult = new ExtractionResult(hubProjectName: project, hubProjectVersionName:version, bdioComponentDetailsList: components)
                extractionResults.add(extractionResult)
            }
        }

        extractionResults
    }
}
