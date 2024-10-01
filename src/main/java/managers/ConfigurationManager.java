package managers;

import enums.Configuration;
import errors.ConfigurationError;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * Singleton class to manage Environment and Testing configuration.
 */
public class ConfigurationManager {

  private static ConfigurationManager instance;
  private final PropertyHandler configuration;
  private PropertyHandler environment;

  /**
   * Constructs a ConfigurationManager and initialises the configuration PropertyHandler.
   */
  private ConfigurationManager() {
    configuration = new PropertyHandler("./src/test/resources/config/configuration.properties");
  }

  /**
   * Retrieves the singleton instance of ConfigurationManager.
   *
   * @return The singleton instance of ConfigurationManager.
   */
  public static synchronized ConfigurationManager get() {
    if (instance == null) {
      instance = new ConfigurationManager();
    }
    return instance;
  }

  /**
   * Retrieves the environment PropertyHandler.
   *
   * @return The environment PropertyHandler.
   */
  public PropertyHandler environment() {
    if (environment == null) {
      environment = new PropertyHandler(
          String.format(
              "./src/test/resources/config/%s.env.properties",
              configuration.asRequiredString(Configuration.ENVIRONMENT)));
    }
    return environment;
  }

  /**
   * Retrieves the configuration PropertyHandler.
   *
   * @return The configuration PropertyHandler.
   */
  public PropertyHandler configuration() {
    return configuration;
  }

  /**
   * A utility class which provides mechanisms to retrieve configuration data.
   */
  public static class PropertyHandler {

    private final Properties properties;

    /**
     * Constructs a PropertyHandler and loads properties from the given path.
     *
     * @param path The path to the properties file.
     */
    public PropertyHandler(String path) {
      try (FileInputStream input = new FileInputStream(path)) {
        properties = new Properties();
        properties.load(input);
        overwriteSecrets(path);
      } catch (IOException e) {
        throw new ConfigurationError(
            String.format("There was an error loading the property file at path: %s", path), e);
      }
    }

    /**
     * Retrieves a configuration property with the given name.
     *
     * @param configuration The enum of the configuration property.
     * @param strict        Indicates whether to throw an error if the property is not found.
     * @return The value of the property.
     * @throws NoSuchFieldError If the property is not found and strict mode is enabled.
     */
    private Object getConfiguration(Configuration configuration, boolean strict)
        throws NoSuchFieldError {
      var property = configuration.getProperty();
      var envProperty = getEnvNameForOperatingSystem(property);
      Object config = Optional.ofNullable(System.getenv(envProperty))
          .orElse(System.getProperty(property));

      if (strict) {
        return Optional.ofNullable(config)
            .orElseGet(() ->
                Optional.ofNullable(properties.get(property))
                    .orElseThrow(
                        () ->
                            new NoSuchFieldError(
                                String.format("No configuration value found for %s", property))));
      }
      return Optional.ofNullable(config).orElse(properties.get(property));
    }

    /**
     * Retrieves a configuration property as a boolean.
     *
     * @param configuration The enum of the configuration property.
     * @return The boolean value of the property, or null if not found.
     */
    public Boolean asFlag(Configuration configuration) {
      var configurationValue = getConfiguration(configuration, false);
      return configurationValue == null
          ? null
          : Boolean.parseBoolean(String.valueOf(configurationValue));
    }

    /**
     * Retrieves a configuration property as a boolean but returns the default value if no matching
     * property found.
     *
     * @param configuration The enum of the configuration property.
     * @param defaultValue  The default value.
     * @return The boolean value of the property, or the default value if not found.
     */
    public boolean asFlag(Configuration configuration, boolean defaultValue) {
      return Optional.ofNullable(asFlag(configuration)).orElse(defaultValue);
    }

    /**
     * Retrieves a required configuration property as a boolean.
     *
     * @param configuration The enum of the configuration property.
     * @return The boolean value of the property.
     * @throws NoSuchFieldError If the property is not found.
     */
    public boolean asRequiredFlag(Configuration configuration) throws NoSuchFieldError {
      return Boolean.parseBoolean(String.valueOf(getConfiguration(configuration, true)));
    }

    /**
     * Retrieves a configuration property as a string.
     *
     * @param configuration The enum of the configuration property.
     * @return The string value of the property, or null if not found.
     */
    public String asString(Configuration configuration) {
      var configurationValue = getConfiguration(configuration, false);
      return configurationValue == null ? null : String.valueOf(configurationValue);
    }

    /**
     * Retrieves a configuration property as a string but returns the default value if no matching
     * property found.
     *
     * @param configuration The enum of the configuration property.
     * @param defaultValue  The default value.
     * @return The string value of the property, or the default value if not found.
     */
    public String asString(Configuration configuration, String defaultValue) {
      return Optional.ofNullable(asString(configuration)).orElse(defaultValue);
    }

    /**
     * Retrieves a required configuration property as a string.
     *
     * @param configuration The enum of the configuration property.
     * @return The string value of the property.
     * @throws NoSuchFieldError If the property is not found.
     */
    public String asRequiredString(Configuration configuration) throws NoSuchFieldError {
      return String.valueOf(getConfiguration(configuration, true));
    }

    /**
     * Retrieves a configuration property as an integer.
     *
     * @param configuration The enum of the configuration property.
     * @return The integer value of the property, or null if not found.
     */
    public Integer asInteger(Configuration configuration) {
      var configurationValue = getConfiguration(configuration, false);
      return configurationValue == null
          ? null
          : Integer.valueOf(String.valueOf(configurationValue));
    }

    /**
     * Retrieves a configuration property as an integer but returns the default value if no matching
     * property found.
     *
     * @param configuration The enum of the configuration property.
     * @param defaultValue  The default value.
     * @return The integer value of the property, or the default value if not found.
     */
    public Integer asInteger(Configuration configuration, Integer defaultValue) {
      return Optional.ofNullable(asInteger(configuration)).orElse(defaultValue);
    }

    /**
     * Retrieves a required configuration property as an integer.
     *
     * @param configuration The enum of the configuration property.
     * @return The integer value of the property.
     * @throws NoSuchFieldError If the property is not found.
     */
    public Integer asRequiredInteger(Configuration configuration) throws NoSuchFieldError {
      return Integer.valueOf(String.valueOf(getConfiguration(configuration, true)));
    }

    private String getEnvNameForOperatingSystem(String env) {
      if (Objects.equals(System.getenv("AGENT_OS"), "Linux")) {
        return env.toUpperCase();
      }

      return env;
    }

    /**
     * Attempts to load file {@code <environment-name>.env.secrets} into properties. Secret values
     * take precedence.
     *
     * @param path The path to the environment file, assumes convention
     *             {@code <environment-name>.env.properties}.
     */
    private void overwriteSecrets(String path) {
      var secretsPath = path != null ? path.replace(".properties", ".secrets") : null;
      if (secretsPath != null && new File(secretsPath).exists()) {
        try (FileInputStream secrets = new FileInputStream(secretsPath)) {
          var secretProps = new Properties();
          secretProps.load(secrets);
          properties.putAll(secretProps);
        } catch (IOException e) {
          throw new ConfigurationError(
              "There was an error loading the property file at path: %s".formatted(secretsPath),
              e);
        }
      }
    }
  }
}
