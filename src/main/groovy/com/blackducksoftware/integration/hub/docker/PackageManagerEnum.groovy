package com.blackducksoftware.integration.hub.docker

enum PackageManagerEnum {
    DPKG('/var/lib/dpkg', OperatingSystemEnum.UBUNTU),
    RPM('/var/lib/rpm', OperatingSystemEnum.CENTOS),
    APK('/lib/apk', OperatingSystemEnum.ALPINE)

    final String directory
    final OperatingSystemEnum operatingSystem

    private PackageManagerEnum(String directory, OperatingSystemEnum operatingSystem) {
        this.directory = directory
        this.operatingSystem = operatingSystem
    }

    static PackageManagerEnum getPackageManagerEnumByName(String name){
        PackageManagerEnum result = null
        if(name != null){
            name = name.toUpperCase()
            result = PackageManagerEnum.valueOf(name)
        }
        result
    }
}
