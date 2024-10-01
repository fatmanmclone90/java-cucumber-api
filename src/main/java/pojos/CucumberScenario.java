package pojos;

import io.cucumber.java.Scenario;
import java.util.Collection;
import java.util.Collections;

/**
 * Wrapper around Cucumber Scenario.
 */
public class CucumberScenario {

  private final String name;
  private final String id;
  private final Collection<String> sourceTagNames;

  /**
   * Instantiate from Scenario.
   *
   * @param scenario The scenario.
   */
  public CucumberScenario(Scenario scenario) {
    name = scenario.getName();
    id = scenario.getId();
    sourceTagNames = scenario.getSourceTagNames();
  }

  /**
   * Instantiate from properties.
   */
  public CucumberScenario(String name, String id) {
    this.name = name;
    this.id = id;
    sourceTagNames = Collections.emptyList();
  }

  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }

  public Collection<String> getSourceTagNames() {
    return sourceTagNames;
  }

}