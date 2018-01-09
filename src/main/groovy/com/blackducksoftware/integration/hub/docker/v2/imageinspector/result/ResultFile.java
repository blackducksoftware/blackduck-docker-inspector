package com.blackducksoftware.integration.hub.docker.v2.imageinspector.result;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.docker.v2.OperatingSystemEnum;
import com.blackducksoftware.integration.hub.docker.v2.ProgramPaths;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.google.gson.Gson;

@Component
public class ResultFile {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ProgramPaths programPaths;

    public Result read(final Gson gson) throws HubIntegrationException {
        final String resultFilePath = programPaths.getHubDockerResultPath();
        Result result = null;
        try {
            final File resultFile = new File(resultFilePath);
            final String resultFileContent = FileUtils.readFileToString(resultFile, "UTF8");
            result = gson.fromJson(resultFileContent, Result.class);
        } catch (final IOException e) {
            throw new HubIntegrationException(String.format("Error reading result file %s: %s", resultFilePath, e.getMessage()));
        }
        if (result == null) {
            throw new HubIntegrationException(String.format("Error reading result file %s: result object is null", resultFilePath));
        }
        return result;
    }

    public void write(final Gson gson, final boolean succeeded, final String msg, final OperatingSystemEnum targetOs, final String runOnImageName, final String runOnImageTag, final String dockerTarfilename, final String bdioFilename) {
        final String runOnOsName = targetOs == null ? "" : targetOs.name().toLowerCase();
        final Result result = new Result(succeeded, msg, runOnOsName, runOnImageName, runOnImageTag, dockerTarfilename, bdioFilename);
        try {
            final File outputDirectory = new File(programPaths.getHubDockerOutputPath());
            outputDirectory.mkdirs();
            final File resultOutputFile = new File(programPaths.getHubDockerResultPath());

            try (FileOutputStream resultOutputStream = new FileOutputStream(resultOutputFile)) {
                try (ResultWriter resultWriter = new ResultWriter(gson, resultOutputStream)) {
                    resultWriter.writeResult(result);
                }
            }
        } catch (final Exception e) {
            logger.error(String.format("Error writing result file: %s", e.getMessage()));
        }
    }
}
