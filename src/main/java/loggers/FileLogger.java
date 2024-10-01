package loggers;

import enums.Configuration;
import errors.ScenarioError;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import managers.ConfigurationManager;
import managers.ScenarioManager;

/**
 * Logs to Console and File.
 */
public class FileLogger {

  private static final String LOG_FOLDER = "logs";
  private static final String SOURCE_FOLDER = "target";
  private static final String LOG_FOLDER_PATH = String.format("%s/%s", SOURCE_FOLDER, LOG_FOLDER);
  private static FileLogger instance;
  private final LogManager logManager;
  private final ThreadLocal<Logger> logger = new ThreadLocal<>();

  private FileLogger() {
    logManager = LogManager.getLogManager();
    setLoggingProperties(logManager);
  }

  /**
   * Returns the singleton instance of the PageManager.
   *
   * @return The PageManager instance.
   */
  public static synchronized FileLogger instance() {
    if (instance == null) {
      instance = new FileLogger();
    }
    return instance;
  }

  /**
   * Gets the default scenario Logger or initializes if not already setup.
   *
   * @return The Logger
   */
  public Logger get() {
    if (logger.get() == null) {
      var log = Logger.getLogger(getDefaultScenarioLoggerName());
      logManager.addLogger(log);
      logger.set(log);
      configureFileHandling(log);
    }
    return logger.get();
  }

  private void configureFileHandling(Logger logger) {
    if (ConfigurationManager.get().configuration()
        .asFlag(Configuration.LOG_TO_FILE_ON_FAILURE, true)
        || ConfigurationManager.get().configuration()
        .asFlag(Configuration.LOG_TO_FILE_ALWAYS, false)) {
      try {
        Files.createDirectories(Path.of(LOG_FOLDER_PATH));
        var fileHandler = new FileHandler(getLogFileName(logger.getName()));
        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(fileHandler);
      } catch (IOException exception) {
        logger.warning("Failed to create log directory and attach file handler");
      }
    }
  }

  /**
   * Generates a file name based on the provided name, sanitizing to replace non-allowed characters
   * with "-".
   *
   * @return Sanitized file name
   */
  private String getLogFileName(String loggerName) {
    return String.format("%s/%s/%s.log", SOURCE_FOLDER, LOG_FOLDER, loggerName);
  }

  /**
   * Generates a file name based on the default logger name, sanitizing to replace non-allowed
   * characters with "-".
   *
   * @return Sanitized file name
   */
  public String getLogFileName() {
    return String.format("%s/%s/%s.log", SOURCE_FOLDER, LOG_FOLDER, getDefaultScenarioLoggerName());
  }

  /**
   * Logs the time taken to execute the callable.
   *
   * @param callable Method to execute
   * @param <T>      Return type
   * @return Response from method
   */
  public <T> T timedAction(Callable<T> callable, String logMessage) {
    var start = System.nanoTime();
    try {
      var result = callable.call();
      var waitTime = (System.nanoTime() - start) / 1000_000;
      get().fine(String.format("Action Executed in %s ms : %s", waitTime, logMessage));
      return result;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String getDefaultScenarioLoggerName() {
    var scenario = ScenarioManager.instance().getScenario();
    if (scenario == null) {
      throw new ScenarioError();
    }
    return String.format(
            "%s-%s",
            scenario.getName(),
            scenario.getId())
        .replaceAll("[^a-zA-Z0-9 ._-]", "-");
  }

  private void setLoggingProperties(LogManager logManager) {
    try {
      var properties = new Properties();
      properties.setProperty("handlers", "java.util.logging.ConsoleHandler");
      properties.setProperty(
          ".level", "FINE");
      properties.setProperty(
          "java.util.logging.ConsoleHandler.level",
          ConfigurationManager.get().configuration().asString(
              Configuration.MINIMUM_LOG_LEVEL_CONSOLE,
              "INFO"));
      properties.setProperty(
          "java.util.logging.FileHandler.level",
          ConfigurationManager.get().configuration().asString(
              Configuration.MINIMUM_LOG_LEVEL_FILE,
              "INFO"));
      var stringWriter = new StringWriter();
      properties.store(stringWriter, "LoggingProperties");
      var loggingPropertiesBytes = stringWriter.toString().getBytes();
      logManager
          .readConfiguration(new ByteArrayInputStream(loggingPropertiesBytes));
    } catch (IOException e) {
      // Continue with default properties
    }
  }

  /**
   * Called from Hooks.
   */
  public void teardown() {
    logger.remove();
  }
}