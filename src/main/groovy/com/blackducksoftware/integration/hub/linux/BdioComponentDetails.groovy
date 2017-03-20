package com.blackducksoftware.integration.hub.linux

import com.blackducksoftware.integration.hub.bdio.simple.model.BdioComponent
import com.blackducksoftware.integration.hub.bdio.simple.model.BdioExternalIdentifier

class BdioComponentDetails {
    String name
    String version
    BdioExternalIdentifier externalIdentifier

    BdioComponent createBdioComponent() {
        def component = new BdioComponent()
        component.id = "uuid:${UUID.randomUUID()}"
        component.name = name
        component.version = version
        component.bdioExternalIdentifier = externalIdentifier

        component
    }
}
