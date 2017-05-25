package com.blackducksoftware.integration.hub.docker.client;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProgramVersion {
	private final Logger logger = LoggerFactory.getLogger(ProgramVersion.class);
	private String programVersion;
	
	public String getProgramVersion() throws IOException {
		if (programVersion == null) {
			final ClassPathPropertiesFile versionProperties = new ClassPathPropertiesFile("version.properties");
			programVersion = versionProperties.getProperty("program.version");
			logger.debug(String.format("programVersion: %s", programVersion));
		}
		return programVersion;
	}
}
