package com.blackducksoftware.integration.hub.linux

enum PackageManagerEnum {
    APT('apt.txt'),
    YUM('yum.txt'),
    RPM('rpm.txt'),
    DPKG('dpkg.txt'),
    DPKG_STATUS_FILE('status')

    String filenameSuffix

    private PackageManagerEnum(String filenameSuffix) {
        this.filenameSuffix = filenameSuffix
    }

    public boolean fileMatches(File file) {
        file.name.endsWith(filenameSuffix)
    }
}
