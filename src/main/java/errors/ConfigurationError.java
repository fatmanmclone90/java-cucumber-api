package errors;

/**
 * Custom Unchecked Exception related to framework configuration issues.
 */
public class ConfigurationError extends RuntimeException {

  public ConfigurationError(String message) {
    super(message);
  }

  public ConfigurationError(String message, Object... formatting) {
    super(String.format(message, formatting));
  }

  public ConfigurationError(Throwable cause) {
    super(cause);
  }

}
