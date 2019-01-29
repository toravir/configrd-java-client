package io.configrd.client;

import java.util.Properties;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import io.configrd.client.ConfigClient;
import io.configrd.client.ConfigClient.Method;
import io.configrd.core.exception.InitializationException;

public class TestConfigClientWithConfigrdURI {

  private final String yamlFile = "classpath:repos.yaml";

  private ConfigClient client;

  @Test
  public void testGetPropsFromRepoByNameAndPathWihoutFileName() throws Exception {

    client = new ConfigClient(yamlFile, "cfgrd://appx-d/json", Method.CONFIGRD_URI);
    client.init();

    Properties props = client.getProperties();

    Assert.assertTrue(props.containsKey("property.1.name"));
    Assert.assertEquals("simple", props.getProperty("property.1.name"));

    Assert.assertTrue(props.containsKey("property.4.name"));
    Assert.assertEquals("simple-${property.3.name}",
        props.getProperty("property.4.name"));

    Assert.assertTrue(props.containsKey("bonus.1.property"));
    Assert.assertEquals("bonus2", props.getProperty("bonus.1.property"));
    
    Assert.assertFalse(props.containsKey("log.root.level"));
  }

  @Test
  public void testGetPropsFromDefaultRepoByNameAndPathWihoutFileName() throws Exception {

    client = new ConfigClient(yamlFile, "cfgrd://default/env/dev/custom", Method.CONFIGRD_URI);
    client.init();

    Properties props = client.getProperties();

    Assert.assertTrue(props.containsKey("property.1.name"));
    Assert.assertEquals("custom", props.getProperty("property.1.name"));

    Assert.assertTrue(props.containsKey("property.4.name"));
    Assert.assertEquals("custom-custom",
        props.getProperty("property.4.name"));

    Assert.assertFalse(props.containsKey("bonus.1.property"));
    
    Assert.assertTrue(props.containsKey("log.root.level"));
  }
  
  @Ignore
  @Test
  public void testGetPropsFromNamedPath() throws Exception {

    client = new ConfigClient(yamlFile, "cfgrd://default/#custom", Method.CONFIGRD_URI);
    client.init();

    Properties props = client.getProperties();

    Assert.assertTrue(props.containsKey("property.1.name"));
    Assert.assertEquals("custom", props.getProperty("property.1.name"));

    Assert.assertTrue(props.containsKey("property.4.name"));
    Assert.assertEquals("custom-custom",
        props.getProperty("property.4.name"));

    Assert.assertFalse(props.containsKey("bonus.1.property"));
    
    Assert.assertTrue(props.containsKey("log.root.level"));
  }
  
  @Ignore
  @Test
  public void testGetPropsFromNamedPaths() throws Exception {

    client = new ConfigClient(yamlFile, "cfgrd://default/#custom,simple", Method.CONFIGRD_URI);
    client.init();

    Properties props = client.getProperties();

    Assert.assertTrue(props.containsKey("property.1.name"));
    Assert.assertEquals("custom", props.getProperty("property.1.name"));

    Assert.assertTrue(props.containsKey("property.4.name"));
    Assert.assertEquals("custom-custom",
        props.getProperty("property.4.name"));

    Assert.assertTrue(props.containsKey("bonus.1.property"));
    Assert.assertEquals("bonus2",
        props.getProperty("bonus.1.property"));
    
    Assert.assertTrue(props.containsKey("log.root.level"));
  }
  
  /* Uncomment after merge to master
  @Test
  public void testGetPropsFromNamedPathsOverHttp() throws Exception {

    client = new ConfigClient(yamlFile, "cfgrd://http-resource/#custom,simple", Method.CONFIGRD_URI);
    client.init();

    Properties props = client.getProperties();

    Assert.assertTrue(props.containsKey("property.1.name"));
    Assert.assertEquals("custom", props.getProperty("property.1.name"));

    Assert.assertTrue(props.containsKey("property.4.name"));
    Assert.assertEquals("custom-custom",
        props.getProperty("property.4.name"));

    Assert.assertTrue(props.containsKey("bonus.1.property"));
    Assert.assertEquals("bonus2",
        props.getProperty("bonus.1.property"));
    
    Assert.assertTrue(props.containsKey("log.root.level"));
  }
  */
  
  @Test
  public void testGetRootProperties() throws Exception {

    client = new ConfigClient(yamlFile, "cfgrd://default/", Method.CONFIGRD_URI);
    client.init();

    Properties props = client.getProperties();

    Assert.assertTrue(props.containsKey("property.5.name"));
    Assert.assertEquals("classpath", props.getProperty("property.5.name"));

    Assert.assertTrue(props.containsKey("property.6.name"));
    Assert.assertEquals("ENC(NvuRfrVnqL8yDunzmutaCa6imIzh6QFL)",
        props.getProperty("property.6.name"));

    Assert.assertFalse(props.containsKey("bonus.1.property"));
    
    Assert.assertTrue(props.containsKey("log.root.level"));
  }
  

  @Test(expected = InitializationException.class)
  public void testRequestInvalidRepoName() throws Exception {

    client = new ConfigClient(yamlFile, "cfgrd://invalid/", Method.CONFIGRD_URI);
    client.init();
    
  }
  
}
