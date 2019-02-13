package io.configrd.client.discovery;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import io.configrd.core.Environment;
import io.configrd.core.util.StringUtils;

public class HostsFileDiscoveryStrategy implements ConfigDiscoveryStrategy {

  private static final Logger logger =
      org.slf4j.LoggerFactory.getLogger(HostsFileDiscoveryStrategy.class);

  @Override
  public Optional<URI> lookupConfigPath(Map<String, Object> hostMappings,
      Map<String, Object> envProps) {

    String envName = (String) envProps.get(Environment.ENV_NAME);
    String hostName = (String) envProps.get(Environment.HOST_NAME);

    Optional<URI> uri = Optional.empty();

    String startPath = (String) hostMappings.get(hostName);

    // Attempt environment as a backup
    if (!StringUtils.hasText(startPath) && StringUtils.hasText(envName)) {

      startPath = (String) hostMappings.get(envName);

    }

    if (!StringUtils.hasText(startPath)) {

      logger.warn("Didn't locate any config path for host " + hostName + " or env " + envName
          + ". Falling back to '*' environment.");

      startPath = (String) hostMappings.get("*");// catch all

    }

    if (StringUtils.hasText(startPath)) {
      uri = Optional.ofNullable(URI.create(startPath));
    } else {
      logger.warn("Unable to resolve a config path from hosts lookup");
    }

    return uri;

  }

}
