package managers;

import errors.ScenarioError;
import pojos.CucumberScenario;

public class ScenarioManager {

  public static final String SOURCE_FOLDER = "target";
  private static ScenarioManager instance;
  private final ThreadLocal<CucumberScenario> scenario;


  public ScenarioManager() {
    this.scenario = new ThreadLocal<>();
  }

  /**
   * Provides the singleton instance of the StateManager.
   *
   * @return the singleton instance.
   */
  public static synchronized ScenarioManager instance() {
    if (instance == null) {
      instance = new ScenarioManager();
    }
    return instance;
  }

  /**
   * Retrieves the current cucumber scenario.
   *
   * @return the current scenario.
   */
  public CucumberScenario getScenario() {
    return scenario.get();
  }

  /**
   * Sets the current scenario for the thread.
   *
   * @param scenario the scenario to set.
   */
  public void setScenario(CucumberScenario scenario) {
    this.scenario.set(scenario);
  }

  public String getFileName(String folder, String extension) {
    return "%s/%s/%s.%s".formatted(
        SOURCE_FOLDER,
        folder,
        getDefaultScenarioLoggerName(),
        extension);
  }

  private String getDefaultScenarioLoggerName() {
    if (scenario.get() == null) {
      throw new ScenarioError();
    }
    return String.format(
            "%s-%s",
            scenario.get().getName(),
            scenario.get().getId())
        .replaceAll("[^a-zA-Z0-9 ._-]", "-");
  }

  /**
   * Called from Hooks.
   */
  public void teardown() {
    this.scenario.remove();
  }


}