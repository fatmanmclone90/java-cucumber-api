package clients;

import static io.restassured.RestAssured.given;

import enums.Configuration;
import enums.HttpVerb;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.Map;
import managers.ConfigurationManager;
import org.apache.hc.core5.http.HttpHeaders;

public class RestAssuredClient {

  public static final String HTTP_1_1 = "HTTP/1.1";
  private static final String EMPTY = "";
  private static final String APPLICATION_JSON = "application/json";
  private final RequestSpecification requestSpec;

  public RestAssuredClient() {
    RequestSpecBuilder builder = new RequestSpecBuilder();
    builder.addHeader(HttpHeaders.ACCEPT, APPLICATION_JSON);
    builder.addHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);
    builder.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US");
    var apiKey = ConfigurationManager.get().environment().asString(Configuration.API_KEY);
    if (apiKey != null) {
      builder.addHeader("api-key", apiKey);
    }
    builder.setBasePath(
        ConfigurationManager.get().environment().asRequiredString(Configuration.API_BASE_URL));
    requestSpec = builder.build();
  }


  public Response send(
      HttpVerb httpVerb,
      String urlFragment,
      String jsonContent,
      Map<String, String> queryParams,
      Map<String, String> additionalHeaders) {
    var response = given().spec(requestSpec)
        .when()
        .headers(additionalHeaders)
        .queryParams(queryParams)
        .body(jsonContent)
        .get(urlFragment)
        .then()
        .extract()
        .response();

    //TODO: Build HAR
    //TODO: Logging filters

    return response;

  }

}