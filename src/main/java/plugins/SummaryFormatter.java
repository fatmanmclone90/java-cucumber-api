package plugins;

import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.TestCase;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestCaseStarted;
import java.util.ArrayList;
import java.util.List;

/**
 * Outputs running totals to console during test execution.
 */
public class SummaryFormatter implements ConcurrentEventListener {

  public static final String NEW_LINE = "-----------------------------------------------------%n";
  private int failedTestCases;
  private int passedTestCases;
  private int otherTestsCases;
  private final List<TestCase> runningTestCases = new ArrayList<>();

  private int getTotalTestCases() {
    return this.passedTestCases + this.otherTestsCases + this.failedTestCases;
  }

  @Override
  public void setEventPublisher(EventPublisher eventPublisher) {
    eventPublisher.registerHandlerFor(TestCaseStarted.class, this::testCaseStartedHandler);
    eventPublisher.registerHandlerFor(TestCaseFinished.class, this::testCaseFinishedHandler);
  }

  private void testCaseStartedHandler(TestCaseStarted testCaseStarted) {
    runningTestCases.add(testCaseStarted.getTestCase());
    printTable();
  }

  private void testCaseFinishedHandler(TestCaseFinished testCaseFinished) {
    runningTestCases.remove(testCaseFinished.getTestCase());
    switch (testCaseFinished.getResult().getStatus()) {
      case PASSED -> passedTestCases++;
      case FAILED -> failedTestCases++;
      default -> otherTestsCases++;
    }
    printTable();
  }

  private void printTable() {
    System.out.printf(NEW_LINE);
    System.out.printf("              Running Test Cases (%d)         %n", runningTestCases.size());
    System.out.printf(NEW_LINE);
    for (TestCase testCase : runningTestCases) {
      System.out.printf("- %-10s", testCase.getName());
    }
    System.out.printf(NEW_LINE);
    System.out.printf(NEW_LINE);
    System.out.printf("                   Test Summary             %n");
    System.out.printf("                  Running Totals            %n");
    System.out.printf(NEW_LINE);
    System.out.printf(
        "| %-10s | %-10s | %10s | %10s |%n",
        "Successful",
        "Failed",
        "Unknown",
        "Total");
    System.out.printf(NEW_LINE);
    System.out.printf(
        "| %-10s | %-10s | %-10s | %-10s |%n",
        this.passedTestCases,
        this.failedTestCases,
        this.otherTestsCases,
        getTotalTestCases());
    System.out.printf(NEW_LINE);
  }

}