package com.synopsys.integration.blackduck.dockerinspector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.synopsys.integration.exception.IntegrationException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DockerInspectorSystemPropertiesTest {
  private static File propertiesFile;

  @BeforeAll
  public static void setUp() throws IOException {
    propertiesFile = new File("test/additionalSystemProperties.properties");
    FileUtils.writeStringToFile(propertiesFile, "testproperty=testvalue", StandardCharsets.UTF_8);
  }
  @Test
  public void testNothingAdded() throws IntegrationException {
    DockerInspectorSystemProperties propertyMgr = new DockerInspectorSystemProperties();

    final Properties systemPropertiesBefore = System.getProperties();
    propertyMgr.augmentSystemProperties(null);
    final Properties systemPropertiesAfter = System.getProperties();
    assertEquals(systemPropertiesAfter.size(), systemPropertiesBefore.size());
  }

  @Test
  public void testPropertyAdded() throws IntegrationException {
    DockerInspectorSystemProperties propertyMgr = new DockerInspectorSystemProperties();

    final int propertyCountBefore = System.getProperties().size();
    System.out.printf("Before: #properties: %d\n", propertyCountBefore);
    assertEquals(null, System.getProperty("testproperty"));
    propertyMgr.augmentSystemProperties(propertiesFile.getAbsolutePath());
    final int propertyCountAfter = System.getProperties().size();
    assertEquals("testvalue", System.getProperty("testproperty"));
    assertEquals(propertyCountBefore+1, propertyCountAfter);
  }

}
