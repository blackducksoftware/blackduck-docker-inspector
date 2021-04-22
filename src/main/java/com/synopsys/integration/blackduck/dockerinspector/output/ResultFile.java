/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.dockerinspector.output;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import java.io.File;
import java.io.FileOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ResultFile {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public void write(final Gson gson, final File resultsOutputFile, final Result result) {
        try {
            logger.trace(String.format("Writing resultsOutputFile: %s; result: %s", resultsOutputFile.getAbsolutePath(), result.toString()));
            resultsOutputFile.getParentFile().mkdirs();
            try (FileOutputStream resultOutputStream = new FileOutputStream(resultsOutputFile)) {
                try (ResultWriter resultWriter = new ResultWriter(gson, resultOutputStream)) {
                    resultWriter.writeResult(result);
                }
            }
        } catch (final Exception e) {
            logger.error(String.format("Error writing output file: %s", e.getMessage()));
        }
    }
}
