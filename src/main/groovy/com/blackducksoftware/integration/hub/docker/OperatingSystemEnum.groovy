package com.blackducksoftware.integration.hub.docker

enum OperatingSystemEnum {
    ALPINE ('alpine'),
    CENTOS ('centos'),
    DEBIAN('debian'),
    FEDORA('fedora'),
    UBUNTU('ubuntu'),
    REDHAT('redhat')

    String forge

    private OperatingSystemEnum(String forge) {
        this.forge = forge
    }

    static OperatingSystemEnum determineOperatingSystem(String operatingSystemName){
        OperatingSystemEnum result = null
        if(operatingSystemName != null){
            operatingSystemName = operatingSystemName.toUpperCase()
            result = OperatingSystemEnum.valueOf(operatingSystemName)
        }
        result
    }
}
