package io.configrd.client;

import org.junit.Assert;
import org.junit.Test;
import io.configrd.client.ConfigClient;
import io.configrd.client.ConfigClient.Method;
import io.configrd.core.exception.InitializationException;

public class TestConfigClientWithHostFile {

  private final String yamlFile = "classpath:/repos.yaml";

  @Test(expected = InitializationException.class)
  public void createConfigWithWrongPath() throws Exception {
    ConfigClient config =
        new ConfigClient(yamlFile, "classpath:/env/wrong.properties", Method.HOST_FILE);
    config.init();
  }

  @Test
  public void createConfigWithNonExistingHost() throws Exception {
    ConfigClient config =
        new ConfigClient(yamlFile, "classpath:/env/hosts.properties", Method.HOST_FILE);
    config.getEnvironment().setHostName("doesntexist");
    config.init();

    Assert.assertEquals("value1", config.getProperty("property.1.name", String.class));
  }
  
  @Test
  public void createConfigWithNonExistingEnvironment() throws Exception {
    ConfigClient config =
        new ConfigClient(yamlFile, "classpath:/env/hosts.properties", Method.HOST_FILE);
    config.getEnvironment().setEnvironmentName("doesntexist");
    config.getEnvironment().setHostName("doesntexist");
    config.init();

    Assert.assertEquals("value1", config.getProperty("property.1.name", String.class));
  }

  @Test
  public void testLookupConfigByEnvironment() throws Exception {
    ConfigClient config =
        new ConfigClient(yamlFile, "classpath:/env/hosts.properties", Method.HOST_FILE);
    config.getEnvironment().setEnvironmentName("QA");
    config.init();

    Assert.assertEquals("custom", config.getProperty("property.3.name", String.class));
  }

  @Test
  public void testLookupConfigByHostname() throws Exception {
    ConfigClient config =
        new ConfigClient(yamlFile, "classpath:/env/hosts.properties", Method.HOST_FILE);
    config.getEnvironment().setHostName("michelangello-custom2");
    config.init();

    Assert.assertEquals("michelangello", config.getProperty("property.3.name", String.class));
  }

}
