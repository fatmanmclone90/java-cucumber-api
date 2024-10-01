package stepdefinitions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import clients.RestAssuredClient;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import enums.HttpVerb;
import enums.JsonPathOperation;
import errors.ConfigurationError;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import loggers.FileLogger;
import net.minidev.json.JSONArray;
import org.apache.hc.core5.http.HttpHeaders;
import pojos.ResolvedString;
import utils.JsonPathUtils;
import utils.JsonSerializer;
import utils.StepDefinitionUtils;

/**
 * Step definitions for common steps for api testing.
 */
//@ScenarioScoped
public class CommonApiSteps {

  private final RestAssuredClient httpClient;
  private Response apiResponse;
  private Map<String, String> headers;
  private Object content;

  public CommonApiSteps(RestAssuredClient httpClient) {
    this.httpClient = httpClient;
  }

  @Given("A request body of {string} with JSON Paths")
  public void requestBodyOfWithJsonPaths(String fileName, List<List<ResolvedString>> dataRows)
      throws IOException {
    StepDefinitionUtils.validateResolvedStrings(dataRows, 3,
        new String[]{"field", "value", "operation"});
    var json = new String(
        Files.readAllBytes(Path.of(String.format("src/test/resources/requests/%s", fileName))));

    var document = transform(
        json,
        StepDefinitionUtils.resolvedStringsToStrings(dataRows));
    this.content = document.json();
  }

  @Given("a request body of")
  public void requestBody(String docString) {
    this.content = JsonSerializer.fromJson(docString, JsonObject.class);
  }

  @Given("I apply JSON Path transformations")
  public void transformExistingBody(List<List<ResolvedString>> dataRows) {
    StepDefinitionUtils.validateResolvedStrings(dataRows, 3,
        new String[]{"field", "value", "operation"});

    if (this.content == null) {
      throw new ConfigurationError("Step can only be applied once a body has been set");
    }

    var json = JsonSerializer.toJson(this.content);
    var document = transform(
        json,
        StepDefinitionUtils.resolvedStringsToStrings(dataRows));
    this.content = document.json();
  }

  @When("I perform a HTTP {httpVerb} for route {string} and query params")
  public void performHttpRequestWithParams(
      HttpVerb httpVerb,
      String route,
      List<List<String>> dataRows) throws URISyntaxException, IOException, InterruptedException {
    StepDefinitionUtils.validateDatatable(dataRows, 2, new String[]{"header key", "header value"});

    var queryParams = dataRows
        .stream()
        .collect(Collectors.toMap(List::getFirst, item -> item.get(1)));

    this.apiResponse = send(httpVerb, route, queryParams);
  }

  @When("I perform a HTTP {httpVerb} for route {string}")
  public void performHttpRequest(HttpVerb httpVerb, String route)
      throws URISyntaxException, IOException, InterruptedException {
    this.apiResponse = send(httpVerb, route, null);
  }

  @Then("The http response contains JSON Paths")
  public void theHttpResponseContainsJsonPaths(List<List<ResolvedString>> dataRows) {
    StepDefinitionUtils.validateResolvedStrings(dataRows, 2, new String[]{"field", "value"});
    var contentType = this.apiResponse.headers().get(HttpHeaders.CONTENT_TYPE.toLowerCase());
    if (contentType.getValue().contains("application/json")) {
      var document = JsonPath.parse(this.apiResponse.body());
      for (var row : dataRows) {
        var field = document.read(row.get(0).getValue());
        if (field instanceof Double) {
          assertEquals(Double.parseDouble(row.get(1).getValue()), (Double) field);
        } else {
          assertEquals(row.get(1).getValue(), field.toString());
        }
      }
    } else {
      FileLogger.instance().get().severe(
          String.format("Response was not in JSON format: %s", this.apiResponse.body()));
      throw new ConfigurationError("Response was not in JSON format");

    }
  }

  @Given("I add HTTP headers")
  public void setRequestWithHeaders(List<List<String>> dataRows) {
    StepDefinitionUtils.validateDatatable(dataRows, 2, new String[]{"header key", "header value"});
    this.headers = dataRows
        .stream()
        .collect(Collectors.toMap(List::getFirst, item -> item.get(1)));
  }

  @Then("The Http Response code is {int}")
  public void httpResponseCodeIs(int httpResponseCode) {
    var actual = this.apiResponse.statusCode();
    assertEquals(
        httpResponseCode,
        actual,
        String.format(
            "The HTTP Status Code %s does not match expected %s.  HTTP Response Body: %s",
            httpResponseCode,
            actual,
            this.apiResponse.body()));
  }

  @Then("The http response contains array with JSON Path {resolvedString} containing values")
  public void theHttpResponseContainsArrayWithJsonPathAndValue(
      ResolvedString jsonPath,
      List<List<ResolvedString>> dataRows) {
    StepDefinitionUtils.validateResolvedStrings(dataRows, 1, new String[]{"JSON Path"});
    var document = JsonPathUtils.parse(this.apiResponse.body().toString());

    var array = (JSONArray) JsonPathUtils.read(document, jsonPath.getValue());
    for (var row : dataRows) {
      var expectedValue = row.getFirst().getValue();
      assertTrue(
          array.stream()
              .anyMatch(item -> Objects.equals(item.toString(), expectedValue)),
          String.format(
              "Value %s was not found in array %s",
              expectedValue,
              array.toJSONString()));
    }
  }

  @Then("I print the HTTP request to console")
  public void printRequest() {
    FileLogger.instance().get()
        .info("API Request: %s".formatted(
            JsonSerializer.prettyPrint(JsonSerializer.toJson(this.content), true)));
  }

  private Response send(
      HttpVerb httpVerb,
      String route,
      Map<String, String> queryParams) {
    return httpClient.send(
        httpVerb,
        route,
        JsonSerializer.toJson(this.content),
        queryParams,
        this.headers);
  }

  public DocumentContext transform(String json, List<List<String>> dataRows) {
    var document = JsonPathUtils.parse(json);
    for (var row : dataRows) {
      var operation = JsonPathOperation.valueOf(row.get(2).toUpperCase());
      var existingValue = JsonPathUtils.read(document, row.get(0));
      switch (operation) {
        case JsonPathOperation.REMOVE -> {
          if (existingValue != null) {
            JsonPathUtils.remove(document, row.getFirst());
          }
        }
        case JsonPathOperation.SET -> {
          switch (existingValue) {
            case Integer i ->
                JsonPathUtils.setValue(document, row.get(0), Integer.parseInt(row.get(1)));
            case Double v ->
                JsonPathUtils.setValue(document, row.get(0), Double.parseDouble(row.get(1)));
            case Boolean b ->
                JsonPathUtils.setValue(document, row.get(0), Boolean.parseBoolean(row.get(1)));
            case null, default -> JsonPathUtils.setValue(document, row.get(0), row.get(1));
          }
        }
        case JsonPathOperation.ADD_ARRAY_ITEM -> {
          Map<String, Object> blank = Map.of();
          JsonPathUtils.updateArray(
              document,
              row.getFirst(),
              blank);
        }
        default -> throw new IllegalArgumentException("Unknown JsonPath Operation");
      }
    }

    return document;
  }

  @Given("I set JSON at JSON Path {string}")
  public void iSetJSONAtJSONPath(String jsonPath, String docString) {
    var document = JsonPathUtils.parse(JsonSerializer.toJson(this.content));
    var json = JsonSerializer.fromJson(docString, JsonObject.class);
    JsonPathUtils.setJsonObject(document, jsonPath, json);
    this.content = document.json();
  }


}
