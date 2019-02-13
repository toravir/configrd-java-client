package io.configrd.client;

import java.util.Properties;
import io.configrd.core.DefaultMergeStrategy;

/**
 * Interface for configuration implementations.
 * 
 * @author Krzysztof Karski
 *
 */
public interface Config {

  public final static String DEFAULT_MERGE_STRATEGY_CLASS = DefaultMergeStrategy.class.getName();
 
  /**
   * 
   * @return All loaded properties. Empty properties if none loaded.
   */
  public Properties getProperties();

  /**
   * 
   * @param key - key name of the property
   * @param clazz - the type of the property value for strong typing.
   * @return the property value case to the type provided. Returns null if no property is found
   */
  public <T> T getProperty(String key, Class<T> clazz);

  /**
   * 
   * @param key - key name of the property
   * @param clazz - the type of the property value for strong typing
   * @param defaultVal - a default value to return if a property value isn't found
   * @return the property value case to the type provided. Returns null if no property is found
   */
  public <T> T getProperty(String key, Class<T> clazz, T defaultVal);

}
