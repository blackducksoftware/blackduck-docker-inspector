package com.blackducksoftware.integration.hub.docker.linux;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;

public class EtcDir {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final File etcFile;

    public EtcDir(final File etcFile) {
        this.etcFile = etcFile;
    }

    public OperatingSystemEnum getOperatingSystem() throws HubIntegrationException, IOException {
        if (etcFile == null) {
            throw new HubIntegrationException("Could not determine the Operating System because none of the expected etc files were found.");
        }
        if (etcFile == null || etcFile.listFiles() == null || (FileUtils.listFiles(etcFile, FileFileFilter.FILE, null).size() == 0)) {
            throw new HubIntegrationException(String.format("Could not determine the Operating System because we could not find the OS files in %s.", etcFile.getAbsolutePath()));
        }
        logger.debug(String.format("etc directory %s", etcFile.getAbsolutePath()));
        final OperatingSystemEnum osEnum = extractOperatingSystemFromFiles(etcFile.listFiles());
        return osEnum;
    }

    private static OperatingSystemEnum extractOperatingSystemFromFiles(final File[] osFiles) throws IOException {
        OperatingSystemEnum osEnum = null;
        for (final File osFile : osFiles) {
            String linePrefix = null;
            if (StringUtils.compare(osFile.getName(), "lsb-release") == 0) {
                linePrefix = "DISTRIB_ID=";
            } else if (StringUtils.compare(osFile.getName(), "os-release") == 0) {
                linePrefix = "ID=";
            }
            if (linePrefix != null) {

                final List<String> lines = FileUtils.readLines(osFile, "UTF-8");
                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith(linePrefix)) {
                        final String[] parts = line.split("=");
                        String value = parts[1];
                        value = value.replaceAll("\"", "");
                        osEnum = OperatingSystemEnum.determineOperatingSystem(value);
                    }
                }
            }
            if (osEnum != null) {
                break;
            }
        }
        return osEnum;
    }
}
