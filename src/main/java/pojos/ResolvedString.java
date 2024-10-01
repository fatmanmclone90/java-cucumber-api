package pojos;

public class ResolvedString {

  private final String value;

  private final String expression;

  public ResolvedString(
      String value,
      String expression) {
    this.value = value;
    this.expression = expression;
  }

  public String getValue() {
    return value;
  }

  public String getExpression() {
    return expression;
  }

  @Override
  public String toString() {
    return this.value;
  }
}
