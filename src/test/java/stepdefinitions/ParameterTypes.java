package stepdefinitions;

import enums.HttpVerb;
import io.cucumber.java.DataTableType;
import io.cucumber.java.ParameterType;
import java.util.List;
import java.util.Objects;
import pojos.ResolvedString;
import valueresolvers.ValueResolver;

/**
 * Class for reusable ParameterTypes across step definition files.
 */
public class ParameterTypes {

  @DataTableType
  public String nullToString(String cell) {
    return Objects.isNull(cell) ? "" : cell;
  }

  @ParameterType(value = ".*")
  public ResolvedString resolvedString(String string) {
    return convertToResolvedString(string);
  }

  @DataTableType()
  public List<ResolvedString> resolvedStringList(List<String> row) {
    return row.stream().map(this::convertToResolvedString).toList();
  }

  @ParameterType("GET|POST|PUT|")
  public HttpVerb httpVerb(String verb) {
    return HttpVerb.valueOf(verb.toUpperCase());
  }

  private ResolvedString convertToResolvedString(String string) {
    if (string != null) {
      var sanitizedString = string.replace("\"", "");
      var resolvedString = ValueResolver.resolve(sanitizedString);
      return new ResolvedString(resolvedString, sanitizedString);
    } else {
      return new ResolvedString("", "");
    }
  }

}
