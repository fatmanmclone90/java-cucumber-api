package valueresolvers;

import java.util.regex.Pattern;
import loggers.FileLogger;

/**
 * Converts text from Cucumber steps into resolved values.
 */
public class ValueResolver {

  /**
   * Looks up the value resolver and replaces text with resolved values.
   *
   * @param value Expected in format {@code $(Key:Lookup)}
   * @return Returns the replaced string.
   */
  public static String resolve(String value) {
    if (value != null) {
      var regexMatcher = Pattern.compile("\\$\\{(.*?)}");
      var matcher = regexMatcher.matcher(value);
      while (matcher.find()) {
        var parts = matcher.group(1).split(":");
        var key = parts[0];
        var lookupValue1 = parts[1];
        String lookupValue2 = null;
        String lookupValue3 = null;
        if (parts.length >= 3) {
          lookupValue2 = parts[2];
        }
        if (parts.length == 4) {
          lookupValue3 = parts[3];
        }

        var resolvedValue = switch (key) {
          case "Configuration" -> ConfigurationValueResolver.resolve(lookupValue1);
          default -> throw new IllegalArgumentException(
              String.format("Unknown Key %s, cannot generate value", key));
        };

        if (resolvedValue != null) {
          FileLogger.instance().get().fine(
              String.format("Resolved %s to %s", lookupValue1, resolvedValue));

          value = value.replace(matcher.group(0), resolvedValue);
        }
      }
    }

    return value;
  }
}
