package valueresolvers;

import enums.Configuration;
import errors.ConfigurationError;
import java.util.Optional;
import managers.ConfigurationManager;

/**
 * Converts text from Cucumber steps into resolved values using the Configuration Manager.
 */
public class ConfigurationValueResolver {

  /**
   * Returns the corresponding Configuration value.
   *
   * @param value The configuration value to lookup.
   * @return The configuration value as a String.
   */
  public static String resolve(String value) {
    var configuration = Configuration.valueOf(value.toUpperCase());
    return
        Optional.ofNullable(
        ConfigurationManager
            .get()
            .configuration()
            .asString(configuration))
            .orElseGet(() -> Optional.ofNullable(ConfigurationManager
            .get()
            .environment()
            .asString(configuration)).orElseThrow(() -> new ConfigurationError("No configuration property found for value %s".formatted(value))));
  }
}
