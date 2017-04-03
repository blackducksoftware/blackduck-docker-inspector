package com.blackducksoftware.integration.hub.docker

enum PackageManagerEnum {
    APT('/var/lib/apt'),
    YUM('/var/lib/yum'),
    RPM('/var/lib/rpm'),
    DPKG('/var/lib/dpkg'),
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
