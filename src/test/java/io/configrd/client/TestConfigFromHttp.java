package io.configrd.client;

import java.util.Properties;
import org.junit.Assert;
import org.junit.Test;
import io.configrd.core.Config;

public class TestConfigFromHttp {

  private ConfigClient.SimpleConfigClientBuilder client =
      ConfigClient.config("http://config.appcrossings.net");

  private Config config;

  @Test
  public void testGetPropertyFromClasspathByPath() throws Exception {
    config = client.path("env/dev/simple").build();
    Assert.assertNotNull(config.getProperty("property.3.name", String.class));
  }

  @Test
  public void testGetPropertyFromClasspathByAbsolutePath() throws Exception {
    config = client.path("/env/dev/simple").build();
    Assert.assertNotNull(config.getProperty("property.3.name", String.class));
  }

  @Test
  public void testGetPropertyFromClasspathByFilename() throws Exception {
    config = client.path("env/dev/simple/default.properties").build();
    Assert.assertNotNull(config.getProperty("property.3.name", String.class));
  }

  @Test
  public void testGetEmptyProperties() throws Exception {
    config = client.path("does/not/exist/doesntexist.file").build();
    Assert.assertTrue(config.getProperties().isEmpty());
  }

  @Test
  public void testGetPropertyFromClasspathOfJson() throws Exception {

    config = client.path("/env/dev/json/default.json").build();

    Properties props = config.getProperties();

    Assert.assertTrue(props.containsKey("property.1.name"));
    Assert.assertEquals("simple", props.getProperty("property.1.name"));

    Assert.assertTrue(props.containsKey("property.4.name"));
    Assert.assertEquals("simple-${property.3.name}", props.getProperty("property.4.name"));

    Assert.assertTrue(props.containsKey("bonus.1.property"));
    Assert.assertEquals("bonus2", props.getProperty("bonus.1.property"));
  }

  @Test
  public void testGetPropertyFromClasspathOfYaml() throws Exception {

    config = client.path("/env/dev/yaml/default.yaml").build();

    Properties props = config.getProperties();

    Assert.assertTrue(props.containsKey("property.1.name"));
    Assert.assertEquals("simple", props.getProperty("property.1.name"));

    Assert.assertTrue(props.containsKey("property.4.name"));
    Assert.assertEquals("simple-${property.3.name}", props.getProperty("property.4.name"));

    Assert.assertTrue(props.containsKey("bonus.1.property"));
    Assert.assertEquals("bonus2", props.getProperty("bonus.1.property"));
  }
}
