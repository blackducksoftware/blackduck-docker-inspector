package com.blackducksoftware.integration.hub.docker

enum PackageManagerEnum {
    DPKG('/var/lib/dpkg'),
    RPM('/var/lib/rpm'),
    APK('/var/lib/apk')

    String directory

    private PackageManagerEnum(String directory) {
        this.directory = directory
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
