/*
 * Copyright (C) 2017 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Black Duck Software.
 */
package com.blackducksoftware.integration.hub.docker

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils;

class DockerTarParser {

    File workingDirectory

    void parseImageTar(File dockerTar){
        List<File> layerTars = extractLayerTars(dockerTar)
        def layerOutputDir = new File(workingDirectory, "layerFiles")
        layerTars.each { layerTar -> parseLayerTarAndExtract(layerTar, layerOutputDir) }
    }

    private List<File> extractLayerTars(File dockerTar){
        List<File> untaredFiles = new ArrayList<>()
        if(workingDirectory.exists()){
            FileUtils.deleteDirectory(workingDirectory)
        }
        final File outputDir = new File(workingDirectory, dockerTar.getName())
        def tarArchiveInputStream = new TarArchiveInputStream(new FileInputStream(dockerTar))
        try {
            def tarArchiveEntry
            while (null != (tarArchiveEntry = tarArchiveInputStream.getNextTarEntry())) {
                final File outputFile = new File(outputDir, tarArchiveEntry.getName())
                if (tarArchiveEntry.isDirectory()) {
                    outputFile.mkdirs()
                } else if(tarArchiveEntry.name.contains('layer.tar')){
                    final OutputStream outputFileStream = new FileOutputStream(outputFile)
                    try{
                        IOUtils.copy(tarArchiveInputStream, outputFileStream)
                        untaredFiles.add(outputFile)
                    } finally{
                        outputFileStream.close()
                    }
                }
            }
        } finally {
            IOUtils.closeQuietly(tarArchiveInputStream)
        }
        untaredFiles
    }

    private void parseLayerTarAndExtract(File layerTar, File layerOutputDir){
        def layerInputStream = new TarArchiveInputStream(new FileInputStream(layerTar))
        try {
            def layerEntry
            while (null != (layerEntry = layerInputStream.getNextTarEntry())) {
                if(shouldExtractEntry(layerEntry.name)){
                    println layerEntry.name
                    final File outputFile = new File(layerOutputDir, layerEntry.getName())
                    if (layerEntry.isDirectory()) {
                        outputFile.mkdirs()
                    } else {
                        final OutputStream outputFileStream = new FileOutputStream(outputFile)
                        try{
                            IOUtils.copy(layerInputStream, outputFileStream)
                        } finally{
                            outputFileStream.close()
                        }
                    }
                }
            }
        } finally {
            IOUtils.closeQuietly(layerInputStream)
        }
    }


    boolean shouldExtractEntry(String entryName){
        entryName.matches("var/lib/(dpkg|apt|yum|rpm|apk){1}.*")
    }
}
