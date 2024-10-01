package enums;

/**
 * enum to represent the different configuration options.
 */
public enum Configuration {
  API_BASE_URL("apiBaseURL"),
  API_KEY("apiKey"),
  API_TIMEOUT("apiTimeout"),
  API_RETRY_LOOP_TIMEOUT("apiRetryLoopTimeout"),
  ENVIRONMENT("environment"),
  LOG_TO_FILE_ON_FAILURE("logToFileOnFailure"),
  LOG_TO_FILE_ALWAYS("logToFileAlways"),
  MINIMUM_LOG_LEVEL_CONSOLE("minimumLogLevelConsole"),
  MINIMUM_LOG_LEVEL_FILE("minimumLogLevelFile");

  private final String property;

  Configuration(String property) {
    this.property = property;
  }

  /**
   * Gets the Property name as specified in the Configuration file.
   *
   * @return the Property name.
   */
  public String getProperty() {
    return this.property;
  }
}
