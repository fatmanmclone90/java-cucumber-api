package exceptions;

/**
 * Custom Unchecked Exception related to unexpected HTTP status code.
 */
@SuppressWarnings("checkstyle:MissingJavadocMethod")
public class UnexpectedStatusCodeException extends RuntimeException {

  public UnexpectedStatusCodeException(int expectedStatus, int actualStatus, String url) {
    super(
        String.format(
            "Expected status code %d but received %d for URL %s",
            expectedStatus,
            actualStatus,
            url));
  }

  public UnexpectedStatusCodeException(
      int expectedStatus,
      int actualStatus,
      String url,
      Throwable cause) {
    super(
        String.format(
            "Expected status code %d but received %d for URL %s",
            expectedStatus,
            actualStatus,
            url),
        cause);
  }

  public UnexpectedStatusCodeException(Throwable cause) {
    super(cause);
  }
}
