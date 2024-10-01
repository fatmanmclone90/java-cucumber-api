package utils;

import errors.ConfigurationError;
import java.util.List;
import pojos.ResolvedString;

/**
 * Utility methods for use in Step Definitions.
 */
public class StepDefinitionUtils {

  private StepDefinitionUtils() {
  }

  /**
   * Validates the user entered datatable contains the expected number of columns per row.
   *
   * @param dataRows           the user entered datatable.
   * @param expectedColumnSize The expected number of columns per row.
   * @param columns            The column headings, for exception message only.
   */
  public static void validateDatatable(
      List<List<String>> dataRows,
      int expectedColumnSize,
      String[] columns) {
    dataRows.forEach(item -> {
      if (item.size() != expectedColumnSize) {
        throw new ConfigurationError(
            String.format("Datatable must have %s columns %s",
                expectedColumnSize,
                String.join(",", columns)));
      }
    });
  }

  /**
   * Validates the user entered datatable contains the expected number of columns per row.
   *
   * @param dataRows           the user entered datatable.
   * @param expectedColumnSize The expected number of columns per row.
   * @param columns            The column headings, for exception message only.
   */
  public static void validateDatatable(
      List<List<String>> dataRows,
      int expectedColumnSize,
      int rows,
      String[] columns) {
    dataRows.forEach(item -> {
      if (item.size() != expectedColumnSize) {
        throw new ConfigurationError(
            String.format("Datatable must have %s columns %s",
                expectedColumnSize,
                String.join(",", columns)));
      }
    });
    if (dataRows.size() != rows) {
      throw new ConfigurationError("Datatable must have %d rows but was %d", rows,
          dataRows.size());
    }
  }

  public static void validateResolvedStrings(
      List<List<ResolvedString>> dataRows,
      int expectedSize,
      String[] columns) {
    validateDatatable(
        resolvedStringsToStrings(dataRows),
        expectedSize,
        columns);
  }

  public static List<List<String>> resolvedStringsToStrings(
      List<List<ResolvedString>> dataRows) {
    return dataRows.stream().map(i -> i.stream().map(ResolvedString::toString).toList()).toList();
  }

}
