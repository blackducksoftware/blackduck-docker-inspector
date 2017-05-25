package com.blackducksoftware.integration.hub.docker.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

public class ClassPathPropertiesFile {
	private final Properties props;

	public ClassPathPropertiesFile(String propetiesFilename) throws IOException {
		props = new Properties();
		InputStream stream = this.getClass().getClassLoader().getResourceAsStream(propetiesFilename);
		props.load(stream);
	}
	
	public String getProperty(String key) {
		return props.getProperty(key);
	}
	
	public Set<Object> keySet() {
		return props.keySet();
	}
}
