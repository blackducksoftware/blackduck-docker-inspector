package com.blackducksoftware.integration.hub.docker.client


import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

// TODO this implementation is horrible
// Look for a way to determine keys and values at runtime

@Component
class HubDockerProperties {

    private final Logger logger = LoggerFactory.getLogger(HubDockerProperties.class)

    @Value('${hub.url}')
    String hubUrl

    @Value('${hub.timeout}')
    String hubTimeout

    @Value('${hub.username}')
    String hubUsername

    @Value('${hub.password}')
    String hubPassword

    @Value('${hub.proxy.host}')
    String hubProxyHost

    @Value('${hub.proxy.port}')
    String hubProxyPort

    @Value('${hub.proxy.username}')
    String hubProxyUsername

    @Value('${hub.proxy.password}')
    String hubProxyPassword

    @Value('${key.store}')
    String keyStore

    @Value('${key.store.pass}')
    String keyStorePass

    @Value('${hub.project.name}')
    String hubProjectName

    @Value('${hub.project.version}')
    String hubProjectVersion

    @Value('${linux.distro}')
    String linuxDistro

    @Value('${working.directory}')
    String workingDirectory

    @Value('${command.timeout}')
    String commandTimeout

    @Value('${docker.tar}')
    String dockerTar

    @Value('${docker.image.name}')
    String dockerImageName

    @Value('${docker.tag.name}')
    String dockerTagName

    @Value('${docker.host}')
    String dockerHost

    @Value('${docker.tls.verify}')
    String dockerTlsVerify

    @Value('${docker.cert.path}')
    String dockerCertPath

    @Value('${install.dir}')
    String installDir

    @Value('${logging.level.com.blackducksoftware}')
    String loggingLevelComBlackducksoftware

    @Value('${dev.mode}')
    String devMode


    private Properties propsForSubContainer

    public void load() {
        propsForSubContainer = new Properties()

        propsForSubContainer.put('hub.url', hubUrl)
        propsForSubContainer.put('hub.timeout', hubTimeout)
        propsForSubContainer.put('hub.username', hubUsername)
        propsForSubContainer.put('hub.password', hubPassword)
        propsForSubContainer.put('hub.proxy.host', hubProxyHost)
        propsForSubContainer.put('hub.proxy.port', hubProxyPort)
        propsForSubContainer.put('hub.proxy.username', hubProxyUsername)
        propsForSubContainer.put('hub.proxy.password', hubProxyPassword)
        propsForSubContainer.put('key.store', keyStore)
        propsForSubContainer.put('key.store.pass', keyStorePass)
        propsForSubContainer.put('hub.project.name', hubProjectName)
        propsForSubContainer.put('hub.project.version', hubProjectVersion)
        propsForSubContainer.put('linux.distro', linuxDistro)
        propsForSubContainer.put('working.directory', workingDirectory)
        propsForSubContainer.put('command.timeout', commandTimeout)
        propsForSubContainer.put('docker.tar', dockerTar)
        propsForSubContainer.put('docker.image.name', dockerImageName)
        propsForSubContainer.put('docker.tag.name', dockerTagName)
        propsForSubContainer.put('docker.host', dockerHost)
        propsForSubContainer.put('docker.tls.verify', dockerTlsVerify)
        propsForSubContainer.put('docker.cert.path', dockerCertPath)
        propsForSubContainer.put('install.dir', installDir)
        propsForSubContainer.put('logging.level.com.blackducksoftware', loggingLevelComBlackducksoftware)
        propsForSubContainer.put('dev.mode', devMode)

        for (String key : propsForSubContainer.keySet()) {
            logger.debug("load(): ${key}=${propsForSubContainer.get(key)}")
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
