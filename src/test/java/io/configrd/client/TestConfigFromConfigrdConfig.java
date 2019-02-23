package io.configrd.client;

import java.util.Properties;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import io.configrd.core.Config;

public class TestConfigFromConfigrdConfig {

  private ConfigClient.ConfigrdConfigClientBuilder client = null;

  private Config config;

  @Before
  public void setup() {
    client = ConfigClient.configrdconfg("classpath:repos.yaml");
  }

  @Test
  public void testGetPropertyFromDefaultRepo() throws Exception {
    config = client.path("env/dev/simple").build();
    Assert.assertNotNull(config.getProperty("property.3.name", String.class));
  }
  
  @Test
  public void testGetPropertyFromDefaultRepoByNamedPath() throws Exception {
    config = client.named("simple").build();
    Assert.assertNotNull(config.getProperty("property.3.name", String.class));
  }

  @Test
  public void testGetPropertyFromDefaultRepoByAbsolutePath() throws Exception {
    config = client.path("/env/dev/simple").build();
    Assert.assertNotNull(config.getProperty("property.3.name", String.class));
  }

  @Test
  public void testGetPropertyFromDefaultRepoByFilename() throws Exception {
    config = client.path("env/dev/simple/default.properties").build();
    Assert.assertNotNull(config.getProperty("property.3.name", String.class));
  }

  @Test
  public void testGetEmptyProperties() throws Exception {
    config = client.path("does/not/exist/notexists.file").build();
    Assert.assertTrue(config.getProperties().isEmpty());
  }

  @Test
  public void testGetPropertyFromRepoOfJson() throws Exception {

    config = client.repo("appx-d").path("json").build();

    Properties props = config.getProperties();

    Assert.assertTrue(props.containsKey("property.1.name"));
    Assert.assertEquals("simple", props.getProperty("property.1.name"));

    Assert.assertTrue(props.containsKey("property.4.name"));
    Assert.assertEquals("simple-${property.3.name}", props.getProperty("property.4.name"));

    Assert.assertTrue(props.containsKey("bonus.1.property"));
    Assert.assertEquals("bonus2", props.getProperty("bonus.1.property"));
  }

  @Test
  public void testGetPropertyFromHttpOfYaml() throws Exception {

    config = client.repo("http-resource").path("/env/dev/yaml/default.yaml").build();

    Properties props = config.getProperties();

    Assert.assertTrue(props.containsKey("property.1.name"));
    Assert.assertEquals("simple", props.getProperty("property.1.name"));

    Assert.assertTrue(props.containsKey("property.4.name"));
    Assert.assertEquals("simple-${property.3.name}", props.getProperty("property.4.name"));

    Assert.assertTrue(props.containsKey("bonus.1.property"));
    Assert.assertEquals("bonus2", props.getProperty("bonus.1.property"));
  }

}
