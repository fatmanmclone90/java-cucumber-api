package stepdefinitions;

import enums.Configuration;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.Scenario;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import loggers.FileLogger;
import managers.ApiRequestManager;
import managers.ConfigurationManager;
import managers.PlaywrightManager;
import managers.ScenarioManager;
import pojos.CucumberScenario;

/**
 * Class containing Cucumber hooks for setup and teardown actions.
 */
public class Hooks {

  /**
   * Sets up PlaywrightManager before all scenarios.
   */
  @BeforeAll
  public static void setup() {
    PlaywrightManager.startPlaywright();
  }

  /**
   * Sets up the cucumber scenario and checks that it has been correctly tagged.
   *
   * @param scenario CucumberScenario
   */
  @Before(order = 1)
  public static void start(Scenario scenario) {
    ScenarioManager.instance().setScenario(new CucumberScenario(scenario));
  }

  /**
   * Performs cleanup actions after each scenario.
   *
   * @param scenario The scenario that just ran.
   */
  @After()
  public void afterScenario(Scenario scenario) throws IOException {
    FileLogger.instance().get().info("Test Complete");
    attachLog(scenario);
    attachHar(scenario);
    ScenarioManager.instance().teardown();
    PlaywrightManager.get().teardown();
    ApiRequestManager.get().teardown();
    FileLogger.instance().teardown();
  }

  private void attachLog(Scenario scenario) {
    if (ConfigurationManager.get().configuration().asFlag(
        Configuration.LOG_TO_FILE_ALWAYS, false)
        || (scenario.isFailed()
        && ConfigurationManager.get().configuration()
        .asFlag(Configuration.LOG_TO_FILE_ON_FAILURE, false))) {
      var logFilePath = Path.of(FileLogger.instance().getLogFileName());

      try {
        var logFile = new String(Files.readAllBytes(logFilePath));
        scenario.attach(
            logFile.getBytes(),
            "text/plain",
            logFilePath.getFileName().toString());
      } catch (IOException e) {
        FileLogger.instance().get().warning(
            String.format(
                "Failed to attach log file %s",
                logFilePath));
      }
    }
  }

  private void attachHar(Scenario scenario) throws IOException {
    if (ConfigurationManager.get().configuration().asFlag(
        Configuration.LOG_TO_FILE_ALWAYS, false)
        || (scenario.isFailed()
        && ConfigurationManager.get().configuration()
        .asFlag(Configuration.LOG_TO_FILE_ON_FAILURE, false))) {
      var harFilePath = ApiRequestManager.get().writeHar();
      if (harFilePath != null) {
        try {
          String linkHtml =
              String.format(
                  "<p>To view this HAR file, upload it to <b>\"https://jam.dev/utilities/har-file-viewer/\"</b>: "
                      + "<a href=\"../%s\">Download HAR File</a>",
                  Paths.get("target").relativize(harFilePath));
          scenario.attach(linkHtml.getBytes(), "text/html", "HAR File");
        } catch (Exception e) {
          FileLogger.instance().get().warning(
              String.format(
                  "Failed to attach har file %s",
                  harFilePath));
        }
      }
    }
  }

}
