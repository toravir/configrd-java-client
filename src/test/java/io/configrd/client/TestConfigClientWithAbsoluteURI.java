package io.configrd.client;

import java.util.Properties;
import org.junit.Assert;
import org.junit.Test;
import io.configrd.client.ConfigClient;
import io.configrd.client.ConfigClient.Method;

public class TestConfigClientWithAbsoluteURI {
  
  public static final String DEFAULT_CONFIGRD_CONFIG_URI = "classpath:repo-defaults.yml";


  @Test(expected = IllegalArgumentException.class)
  public void testExceptionWithRelativePath() throws Exception {
    ConfigClient client = new ConfigClient(DEFAULT_CONFIGRD_CONFIG_URI, "env/dev/simple", Method.ABSOLUTE_URI);
    client.init();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExceptionWithMissingHost() throws Exception {
    // Should be: "http://www.host.com/env/dev/simple"
    ConfigClient client = new ConfigClient(DEFAULT_CONFIGRD_CONFIG_URI, "http://env/dev/simple", Method.ABSOLUTE_URI);
    client.init();
  }
  
  @Test
  public void testGetPropertiesFromUsingDefaultRepo() throws Exception {
    ConfigClient client = new ConfigClient(DEFAULT_CONFIGRD_CONFIG_URI, "cfgrd://default/env/dev/simple", Method.ABSOLUTE_URI);
    client.init();

    Assert.assertNotNull(client.getProperty("property.3.name", String.class));
  }


  @Test
  public void testGetPropertiesFromClasspathWithoutFileName() throws Exception {
    ConfigClient client = new ConfigClient(DEFAULT_CONFIGRD_CONFIG_URI, "classpath:env/dev/simple", Method.ABSOLUTE_URI);
    client.init();

    Assert.assertNotNull(client.getProperty("property.3.name", String.class));
  }

  @Test
  public void testGetPropertiesFromClasspathWithFileName() throws Exception {
    ConfigClient client = new ConfigClient(DEFAULT_CONFIGRD_CONFIG_URI, "classpath:env/dev/simple/default.properties", Method.ABSOLUTE_URI);
    client.init();

    Assert.assertNotNull(client.getProperty("property.3.name", String.class));
  }

  @Test
  public void testFetchWithDirectPath() throws Exception {

    ConfigClient client = new ConfigClient(DEFAULT_CONFIGRD_CONFIG_URI, "classpath:/env/dev/json/default.json", Method.ABSOLUTE_URI);
    client.init();

    Properties props = client.getProperties();

    Assert.assertTrue(props.containsKey("property.1.name"));
    Assert.assertEquals("simple", props.getProperty("property.1.name"));

    Assert.assertTrue(props.containsKey("property.4.name"));
    Assert.assertEquals("simple-${property.3.name}", props.getProperty("property.4.name"));

    Assert.assertTrue(props.containsKey("bonus.1.property"));
    Assert.assertEquals("bonus2", props.getProperty("bonus.1.property"));
  }

  @Test
  public void testGetPropsFromByAbsoluteURIOnClasspath() throws Exception {

    ConfigClient client = new ConfigClient(DEFAULT_CONFIGRD_CONFIG_URI, "classpath:env/dev/json/default.json", Method.ABSOLUTE_URI);
    client.init();

    Properties props = client.getProperties();

    Assert.assertTrue(props.containsKey("property.1.name"));
    Assert.assertEquals("simple", props.getProperty("property.1.name"));

    Assert.assertTrue(props.containsKey("property.4.name"));
    Assert.assertEquals("simple-${property.3.name}", props.getProperty("property.4.name"));

    Assert.assertTrue(props.containsKey("bonus.1.property"));
    Assert.assertEquals("bonus2", props.getProperty("bonus.1.property"));

    // Because defualt.json, not default.properties
    Assert.assertFalse(props.containsKey("log.root.level"));
  }

  @Test
  public void testGetPropertiesFromHttpWithoutFileName() throws Exception {
    ConfigClient client = new ConfigClient(DEFAULT_CONFIGRD_CONFIG_URI, "http://config.appcrossings.net/env/dev/", Method.ABSOLUTE_URI);
    client.init();

    Assert.assertNotNull(client.getProperty("property.2.name", String.class));
  }

  @Test
  public void testGetPropertiesFromHttpClasspathWithFileName() throws Exception {
    ConfigClient client =
        new ConfigClient(DEFAULT_CONFIGRD_CONFIG_URI, "http://config.appcrossings.net/env/dev/default.properties", Method.ABSOLUTE_URI);
    client.init();

    Assert.assertNotNull(client.getProperty("property.2.name", String.class));
  }

}
