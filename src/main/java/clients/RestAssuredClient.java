package clients;

import static io.restassured.RestAssured.given;

import com.smartbear.har.builder.HarEntryBuilder;
import com.smartbear.har.builder.HarRequestBuilder;
import com.smartbear.har.builder.HarResponseBuilder;
import com.smartbear.har.model.HarContent;
import com.smartbear.har.model.HarHeader;
import com.smartbear.har.model.HarPostData;
import com.smartbear.har.model.HarQueryString;
import enums.Configuration;
import enums.HttpVerb;
import filters.RequestResponseLoggingFilter;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.SpecificationQuerier;
import java.util.Map;
import managers.ApiRequestManager;
import managers.ConfigurationManager;
import org.apache.hc.core5.http.HttpHeaders;
import utils.StringUtils;

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
    builder.setBaseUri(
        ConfigurationManager.get().environment().asRequiredString(Configuration.API_BASE_URL));
    builder.addFilter(new RequestResponseLoggingFilter());
    requestSpec = builder.build();
  }


  public Response send(
      HttpVerb httpVerb,
      String urlFragment,
      String jsonContent,
      Map<String, String> queryParams,
      Map<String, String> additionalHeaders) {
    var when = given().spec(requestSpec)
        .when();

    if (additionalHeaders != null) {
      when.headers(additionalHeaders);
    }

    if (queryParams != null) {
      when.queryParams(queryParams);
    }

    if (StringUtils.isNotNullEmptyOrWhitespace(jsonContent)) {
      when.body(jsonContent);
    }

    var start = System.nanoTime();
    var response = switch (httpVerb) {
      case GET -> when
          .get(urlFragment)
          .then()
          .extract()
          .response();
      case POST -> when
          .post(urlFragment)
          .then()
          .extract()
          .response();
      case PUT -> when
          .put(urlFragment)
          .then()
          .extract()
          .response();
      default -> throw new IllegalStateException("Unexpected value: " + httpVerb);
    };
    var waitTime = (System.nanoTime() - start) / 1000_000;

    buildHarEntry(
        httpVerb,
        urlFragment,
        jsonContent,
        additionalHeaders,
        queryParams,
        response,
        waitTime);

    return response;

  }

  private void buildHarEntry(
      HttpVerb httpVerb,
      String urlFragment,
      String jsonContent,
      Map<String, String> requestHeaders,
      Map<String, String> queryParams,
      Response httpResponse,
      long waitTime) {
    if (queryParams == null) {
      queryParams = Map.of();
    }
    var url = buildUrl(urlFragment);
    var harRequest = new HarRequestBuilder()
        .withMethod(httpVerb.toString())
        .withUrl(url)
        .withHttpVersion(HTTP_1_1)
        .withPostData(new HarPostData(APPLICATION_JSON, null, jsonContent, EMPTY))
        .withQueryString(queryParams.entrySet().stream()
            .map(q -> new HarQueryString(q.getKey(), q.getValue(), EMPTY)).toList())
        .withHeaders(requestHeaders.entrySet().stream()
            .map(q -> new HarHeader(q.getKey(), q.getValue(), EMPTY)).toList())
        .build();
    var contentLength = Long.parseLong(
        httpResponse.headers().get(HttpHeaders.CONTENT_LENGTH.toLowerCase()).getValue());
    var harResponse = new HarResponseBuilder().withContent(
            new HarContent(contentLength, 0L, APPLICATION_JSON, httpResponse.getBody().prettyPrint(),
                EMPTY))
        .withHttpVersion(HTTP_1_1)
        .withHeaders(httpResponse.headers().asList().stream()
            .map(h -> new HarHeader(h.getName(), h.getValue(), EMPTY)).toList())
        .withStatus(httpResponse.statusCode())
        .build();

    ApiRequestManager.get().addRequest(
        new HarEntryBuilder()
            .withRequest(harRequest)
            .withResponse(harResponse)
            .withTime(waitTime)
            .build());
  }

  private String buildUrl(String urlFragment) {
    var queryable = SpecificationQuerier.query(this.requestSpec);
    var baseUrl = queryable.getBaseUri();
    //TODO: use URL builder
    return baseUrl + urlFragment;

  }

}