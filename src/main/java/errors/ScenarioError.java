package errors;

public class ScenarioError extends Error {

  private static final String message =
      "StateManager.instance().setScenario() must be called before accessing the logger.";

  public ScenarioError() {
    super(message);
  }

}
