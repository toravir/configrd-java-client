package io.configrd.client;

import java.io.File;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.beanutils.ConvertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.configrd.core.Config;
import io.configrd.core.ConfigSourceResolver;
import io.configrd.core.DefaultMergeStrategy;
import io.configrd.core.Environment;
import io.configrd.core.MergeStrategy;
import io.configrd.core.exception.InitializationException;
import io.configrd.core.file.FileRepoDef;
import io.configrd.core.processor.ProcessorSelector;
import io.configrd.core.processor.ProcessorSelector.Type;
import io.configrd.core.processor.PropertiesProcessor;
import io.configrd.core.source.ConfigSource;
import io.configrd.core.source.RepoDef;
import io.configrd.core.source.SecuredRepo;
import io.configrd.core.source.StreamPacket;
import io.configrd.core.util.StringUtils;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 
 * @author Krzysztof Karski
 *
 */
public class ConfigClient {

  public abstract class BaseClientBuilder {

    protected Map<String, Object> vals = new HashMap<>();
    protected String uri;
    protected String path;
    protected ConfigSourceResolver sourceResolver;
    protected Integer timerTTL = 0;

    protected BaseClientBuilder(String uri) {
      this.vals.put(RepoDef.URI_FIELD, uri);
      this.vals.put(RepoDef.SOURCE_NAME_FIELD, detectSourceName(uri));
      this.vals.put(RepoDef.NAME_FIELD, "default");
      this.vals.put(RepoDef.TRUST_CERTS_FIELD, "false");
      this.vals.put("path", path);
    }

    public BaseClientBuilder basicAuth(String username, String password) {
      vals.put(SecuredRepo.USERNAME_FIELD, username);
      vals.put(SecuredRepo.PASSWORD_FIELD, password);
      vals.put(SecuredRepo.AUTH_METHOD_FIELD, "HttpBasicAuth");
      return this;
    }

    public abstract Config build();

    /**
     * Change the config file name from default.properties.
     * 
     * @param name i.e. "myvars.yaml, myvars.properties, myvars.json"
     * @return
     */
    public BaseClientBuilder fileName(String name) {
      vals.put(FileRepoDef.FILE_NAME_FIELD, name);
      return this;
    }

    public BaseClientBuilder path(String path) {
      this.path = path;
      return this;
    }

    public BaseClientBuilder refresh(int seconds) {
      this.timerTTL = seconds;
      return this;
    }

    /**
     * Override source name detection.
     * 
     * @param name file or http is supported
     * @return
     */
    public BaseClientBuilder sourceName(String name) {
      vals.put(RepoDef.SOURCE_NAME_FIELD, name);
      return this;
    }

    /**
     * In case connecting over http/s, trust certs by default.
     * 
     * @param trust true or false. default: false
     * @return
     */
    public BaseClientBuilder trustCerts(boolean trust) {
      vals.put(RepoDef.TRUST_CERTS_FIELD, String.valueOf(trust));
      return this;
    }
  }

  protected class ConfigImpl implements Config, Refresh {

    private final ConfigSource configSource;
    private String path;
    private Set<String> named = new HashSet<>();

    private final AtomicReference<Properties> loadedProperties =
        new AtomicReference<>(new Properties());

    protected ConfigImpl(ConfigSource configSource, Set<String> named) {
      this.configSource = configSource;
      this.named = named;
      refresh();
    }

    protected ConfigImpl(ConfigSource configSource, String path) {
      this.configSource = configSource;
      this.path = path;
      refresh();
    }

    public Properties getProperties() {
      Properties props = new Properties();
      props.putAll(loadedProperties.get());
      return props;
    }

    public <T> T getProperty(String key, Class<T> clazz) {

      String value = loadedProperties.get().getProperty(key);

      if (StringUtils.hasText(value)) {
        return (T) ConvertUtils.convert(value, clazz);
      }

      return null;
    }

    public <T> T getProperty(String key, Class<T> clazz, T value) {

      T val = getProperty(key, clazz);

      if (val != null && val != "")
        return val;

      return value;

    }

    public void refresh() {

      final MergeStrategy merge = new DefaultMergeStrategy();

      if (configSource != null) {

        Map<String, Object> p = configSource.get(path, named);
        merge.addConfig(p);

      }

      // Variables defined on host override
      merge.addConfig((Map) environment.getEnvironment());
      Map<String, Object> merged = merge.merge();
      loadedProperties.set(PropertiesProcessor.asProperties(new StringUtils(merged).filled()));
      logger.info("Configs loaded.");
    }

  }

  public class ConfigrdConfigClientBuilder extends BaseClientBuilder {

    private String repoName = "default";
    private String[] namedPaths = new String[] {};

    protected ConfigrdConfigClientBuilder(String uri) {
      super(uri);
    }

    public Config build() {

      final String sourceName = (String) vals.get(RepoDef.SOURCE_NAME_FIELD);
      this.sourceResolver = new ConfigSourceResolver(vals);
      Optional<ConfigSource> cs = sourceResolver.findConfigSourceByName(repoName);

      if (cs.isPresent()) {

        ConfigImpl c = null;
        
        if (namedPaths.length > 0) {
          c = new ConfigImpl(cs.get(), new HashSet<>(Arrays.asList(namedPaths)));
        } else {
          c = new ConfigImpl(cs.get(), path);
        }

        if (this.timerTTL > 0) {
          timer.get().schedule(new ReloadTask(c), (this.timerTTL * 1000), (this.timerTTL * 1000));
        }

        return c;

      } else {

        logger.error("Unable find config source '" + sourceName + "' to load uri " + uri);

        throw new InitializationException(
            "Unable find config source '" + sourceName + "' to load uri " + uri);

      }
    }

    public ConfigrdConfigClientBuilder named(String... names) {
      this.namedPaths = names;
      return this;
    }

    public ConfigrdConfigClientBuilder repo(String repo) {
      this.repoName = repo;
      return this;
    }

  }

  public class ConfigrdServerClientBuilder {

    private String uri;
    private String repoName;
    private String[] namedPaths = new String[] {};
    private String path;
    protected Integer timerTTL = 0;
    protected boolean trustCerts = false;
    protected OkHttpClient client;

    protected ConfigrdServerClientBuilder(String uri) {
      this.uri = uri;
    }

    public Config build() {

      final OkHttpClient.Builder builder = new OkHttpClient.Builder();

      if (this.trustCerts) {
        try {

          final SSLContext sslContext = SSLContext.getInstance("TSL");
          sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
          final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

          builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
          builder.hostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
              return true;
            }
          });

        } catch (Exception e) {
          logger.error(e.getMessage());
        }
      }

      builder.connectTimeout(10, TimeUnit.SECONDS);
      builder.writeTimeout(10, TimeUnit.SECONDS);
      builder.readTimeout(30, TimeUnit.SECONDS);

      client = builder.build();
      URI i = URI.create(uri);

      if (path.startsWith("/")) {
        path = path.replaceFirst("/", "");
      }

      String root = i.getPath();
      if (root.startsWith("/")) {
        root = root.replaceFirst("/", "");
      }

      HttpUrl.Builder httpBuilder = new HttpUrl.Builder().scheme(i.getScheme()).host(i.getHost())
          .addPathSegments(root).addPathSegments(path);

      if (i.getPort() > 0) {
        httpBuilder.port(i.getPort());
      }

      if (namedPaths.length > 0) {
        StringJoiner joiner = new StringJoiner(",");

        for (String n : namedPaths) {
          joiner.add(n);
        }
        httpBuilder.addQueryParameter("p", joiner.toString());
      }

      if (StringUtils.hasText(repoName)) {
        httpBuilder.addQueryParameter("r", repoName);
      }

      Request.Builder request = new Request.Builder().url(httpBuilder.build())
          .addHeader("Accept", "application/json").get();

      logger.info("Fetching " + httpBuilder.toString());

      try (Response call = client.newCall(request.build()).execute()) {

        if (call.isSuccessful() && !call.isRedirect() && call.body().contentLength() > 0) {

          StreamPacket packet = new StreamPacket(URI.create(uri), call.body().byteStream());
          packet.setETag(call.header("ETag"));
          packet.putAll(ProcessorSelector.process(Type.JSON, packet.bytes()));
          return packet;

        } else if (call.isSuccessful() && call.isRedirect()) {

          logger.error("Redirect handling not implemented. Server returned location "
              + call.header("location"));
        }

      } catch (UnknownHostException e) {

        logger.error(e.getMessage(), e);
        throw new IllegalArgumentException(e.getMessage());

      } catch (Exception e) {
        logger.debug(e.getMessage(), e);
        // nothing else
      }

      return new StreamPacket(URI.create(uri));
    }

    public ConfigrdServerClientBuilder named(String... names) {
      this.namedPaths = names;
      return this;
    }

    public ConfigrdServerClientBuilder path(String path) {
      this.path = path;
      return this;
    }

    public ConfigrdServerClientBuilder refresh(int seconds) {
      this.timerTTL = seconds;
      return this;
    }

    public ConfigrdServerClientBuilder repo(String name) {
      this.repoName = name;
      return this;
    }

    /**
     * In case connecting over http/s, trust certs by default.
     * 
     * @param trust true or false. default: false
     * @return
     */
    public ConfigrdServerClientBuilder trustCerts() {
      this.trustCerts = true;
      return this;
    }
  }

  private interface Refresh {
    public void refresh();
  }

  private class ReloadTask extends TimerTask {

    private Refresh client;

    ReloadTask(Refresh client) {
      this.client = client;
    }

    @Override
    public void run() {
      try {

        client.refresh();

      } catch (Exception e) {
        logger.error("Error refreshing configs", e);
      }
    }
  }

  public class SimpleConfigClientBuilder extends BaseClientBuilder {

    protected SimpleConfigClientBuilder(String uri) {
      super(uri);
    }

    public Config build() {

      final String sourceName = (String) vals.get(RepoDef.SOURCE_NAME_FIELD);
      this.sourceResolver = new ConfigSourceResolver();
      Optional<ConfigSource> cs = sourceResolver.newConfigSource("default", vals);

      if (cs.isPresent()) {

        ConfigImpl c = new ConfigImpl(cs.get(), path);
        if (this.timerTTL > 0) {
          timer.get().schedule(new ReloadTask(c), (this.timerTTL * 1000), (this.timerTTL * 1000));
        }

        return c;

      } else {

        logger.error("Unable find config source '" + sourceName + "' to load uri " + uri);

        throw new InitializationException(
            "Unable find config source '" + sourceName + "' to load uri " + uri);

      }
    }

  }

  private final static Logger logger = LoggerFactory.getLogger(ConfigClient.class);

  private static final AtomicReference<Timer> timer = new AtomicReference<Timer>(new Timer(true));

  private static final TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
    @Override
    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
        throws CertificateException {}

    @Override
    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
        throws CertificateException {}

    @Override
    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
      return new java.security.cert.X509Certificate[] {};
    }
  }};

  /**
   * Build a config client sourcing configurations directly from a known absolute URI location such
   * as files on disc, on classpath or over http/s.
   * 
   * @param uri The absolute URI to the configs including file name to fetch.
   * @return
   */
  public static SimpleConfigClientBuilder config(String uri) {

    return new ConfigClient().new SimpleConfigClientBuilder(uri);

  }

  /**
   * Build a config client source configurations based on a repo configuration from a configrd
   * config file
   * 
   * @param uri the configrd config file's absolute URI location
   * @return
   */
  public static ConfigrdConfigClientBuilder configrdconfg(String uri) {
    return new ConfigClient().new ConfigrdConfigClientBuilder(uri);
  }

  /**
   * Build a config client source configurations from a remote configrd server instance.
   * 
   * @param uri the server's URL with scheme, host, port and root (i.e.
   *        https://host:port/configrd/v1/).
   * @return
   */
  public static ConfigrdServerClientBuilder server(String uri) {
    return new ConfigClient().new ConfigrdServerClientBuilder(uri);
  }

  public final Environment environment = new Environment();

  /**
   * 
   * @param uri Connect to a config repo by specifying an aboslute URI (file, http(s)) to the root
   *        of the repo. The location must be accessible to the client
   * @throws Exception
   */
  public ConfigClient() {

  }

  private String detectSourceName(String uri) {

    if (uri == "" || uri.toLowerCase().startsWith(File.separator + File.separator)
        || uri.toLowerCase().startsWith("file:") || uri.toLowerCase().startsWith("classpath")) {
      return "file";
    } else if (uri.trim().startsWith("http")) {
      return "http";
    } else {
      logger.warn("Unable to determine file, classpath or http/s config source from uri " + uri);
      return "";
    }
  }


  public Environment getEnvironment() {
    return environment;
  }

  protected Optional<URI> resolveConfigPathFromConfigrd(URI serverPath) {

    return Optional.empty();

  }

}
