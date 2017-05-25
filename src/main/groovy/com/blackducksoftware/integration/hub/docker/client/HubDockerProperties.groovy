package com.blackducksoftware.integration.hub.docker.client


import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@PropertySource("classpath:application.properties")
@Component
class HubDockerProperties {

    private final Logger logger = LoggerFactory.getLogger(HubDockerProperties.class)

	@Autowired
	private Environment env

    private Properties propsForSubContainer

    public void load() {
        propsForSubContainer = new Properties()
		
		final ClassPathPropertiesFile propertiesFromFile = new ClassPathPropertiesFile('application.properties')
		for (String propertyKey : propertiesFromFile.keySet()) {
			String value = env.getProperty(propertyKey)
			logger.debug("load(): ${propertyKey}=${value}")
			propsForSubContainer.put(propertyKey, value)
		}
    }

    public void set(String key, String value) {
        propsForSubContainer.setProperty(key, value)
    }

    public void save(String path) {
        File propertiesFile = new File(path)
        if (!propertiesFile.getParentFile().exists()) {
            propertiesFile.getParentFile().mkdirs()
        }
        if (propertiesFile.exists()) {
            propertiesFile.delete()
        }
        propsForSubContainer.store(new FileOutputStream(propertiesFile), null)
    }
}
