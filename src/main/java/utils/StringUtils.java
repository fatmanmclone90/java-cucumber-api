package utils;

/**
 * Helper methods for type String.
 */
public final class StringUtils {

  private StringUtils() {
    //Private constructor to hide implicit public one
  }

  /**
   * Checks for null, empty of whitespace.
   *
   * @param value String
   * @return True when not null, empty or whitespace.
   */
  public static boolean isNotNullEmptyOrWhitespace(String value) {
    return value != null && !value.trim().isEmpty();
  }

  /**
   * Checks for null, empty of whitespace.
   *
   * @param value String
   * @return True when null, empty or whitespace.
   */
  public static boolean isNullEmptyOrWhitespace(String value) {
    return !isNotNullEmptyOrWhitespace(value);
  }
}
