package com.blackducksoftware.integration.hub.docker;

import java.io.File;
import java.util.List;

import com.blackducksoftware.integration.hub.docker.imageinspector.imageformat.docker.manifest.ManifestLayerMapping;

public class DissectedImage {
    private File dockerTarFile = null;
    private List<File> layerTars = null;
    private List<ManifestLayerMapping> layerMappings = null;
    private File targetImageFileSystemRootDir = null;
    private OperatingSystemEnum targetOs = null;
    private String runOnImageName = null;
    private String runOnImageTag = null;
    private String bdioFilename = null;

    public DissectedImage() {
    }

    public File getDockerTarFile() {
        return dockerTarFile;
    }

    public void setDockerTarFile(final File dockerTarFile) {
        this.dockerTarFile = dockerTarFile;
    }

    public List<File> getLayerTars() {
        return layerTars;
    }

    public void setLayerTars(final List<File> layerTars) {
        this.layerTars = layerTars;
    }

    public List<ManifestLayerMapping> getLayerMappings() {
        return layerMappings;
    }

    public void setLayerMappings(final List<ManifestLayerMapping> layerMappings) {
        this.layerMappings = layerMappings;
    }

    public File getTargetImageFileSystemRootDir() {
        return targetImageFileSystemRootDir;
    }

    public void setTargetImageFileSystemRootDir(final File targetImageFileSystemRootDir) {
        this.targetImageFileSystemRootDir = targetImageFileSystemRootDir;
    }

    public OperatingSystemEnum getTargetOs() {
        return targetOs;
    }

    public void setTargetOs(final OperatingSystemEnum targetOs) {
        this.targetOs = targetOs;
    }

    public String getRunOnImageName() {
        return runOnImageName;
    }

    public void setRunOnImageName(final String runOnImageName) {
        this.runOnImageName = runOnImageName;
    }

    public String getRunOnImageTag() {
        return runOnImageTag;
    }

    public void setRunOnImageTag(final String runOnImageTag) {
        this.runOnImageTag = runOnImageTag;
    }

    public String getBdioFilename() {
        return bdioFilename;
    }

    public void setBdioFilename(final String bdioFilename) {
        this.bdioFilename = bdioFilename;
    }
}
