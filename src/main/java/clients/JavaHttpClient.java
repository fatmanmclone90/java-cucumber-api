package clients;

import static java.util.stream.Collectors.toList;

import com.smartbear.har.builder.HarEntryBuilder;
import com.smartbear.har.builder.HarRequestBuilder;
import com.smartbear.har.builder.HarResponseBuilder;
import com.smartbear.har.model.HarContent;
import com.smartbear.har.model.HarHeader;
import com.smartbear.har.model.HarPostData;
import com.smartbear.har.model.HarQueryString;
import enums.Configuration;
import enums.HttpVerb;
import errors.ConfigurationError;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import loggers.FileLogger;
import managers.ApiRequestManager;
import managers.ConfigurationManager;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import utils.StringUtils;

public class JavaHttpClient {

  public static final String HTTP_1_1 = "HTTP/1.1";
  private static final String EMPTY = "";
  private static final String APPLICATION_JSON = "application/json";

  public HttpResponse<String> send(
      HttpVerb httpVerb,
      String urlFragment,
      String jsonContent,
      Map<String, String> queryParams,
      Map<String, String> headers) throws URISyntaxException, IOException, InterruptedException {

    var builder = HttpRequest.newBuilder()
        .uri(new URI(buildUrl(urlFragment, queryParams)))
        .timeout(Duration.ofSeconds(
            ConfigurationManager.get().environment().asRequiredInteger(Configuration.API_TIMEOUT)));

    for (var header : setHeaders(headers).entrySet()) {
      builder.header(header.getKey(), header.getValue());
    }

    if (StringUtils.isNotNullEmptyOrWhitespace(jsonContent)) {
      builder.method(httpVerb.toString(), BodyPublishers.ofString(jsonContent));
    }

    var request = builder.build();
    try (var client = HttpClient.newHttpClient()) {
      var start = System.nanoTime();
      var httpResponse = client.send(request, BodyHandlers.ofString());
      var waitTime = (System.nanoTime() - start) / 1000_000;
      buildHarEntry(
          httpVerb,
          urlFragment,
          jsonContent,
          queryParams,
          httpResponse,
          waitTime);
      return httpResponse;
      //TODO: Look into logging
    }
  }

  private String buildUrl(String urlFragment, Map<String, String> queryParams) {
    URIBuilder builder = null;
    try {
      builder = new URIBuilder(
          ConfigurationManager.get().environment().asRequiredString(Configuration.API_BASE_URL));
      builder.appendPath(urlFragment);
    } catch (URISyntaxException e) {
      throw new ConfigurationError(e);
    }
    if (queryParams != null) {
      builder.addParameters(queryParams.entrySet()
          .stream()
          .map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue()))
          .collect(toList()));
    }

    return builder.toString();

  }

  private Map<String, String> setHeaders(Map<String, String> additionalHeaders) {
    // TODO: Look into default headers
    var headers = new HashMap<String, String>();
    headers.put(HttpHeaders.ACCEPT, APPLICATION_JSON);
    headers.put(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);
    headers.put(HttpHeaders.ACCEPT_LANGUAGE, "en-US");

    var apiKey = ConfigurationManager.get().environment().asString(Configuration.API_KEY);
    if (apiKey != null) {
      headers.put("api-key", apiKey);
      FileLogger.instance().get().fine("API Key added to request.");
    }

    if (additionalHeaders != null && !additionalHeaders.isEmpty()) {
      headers.putAll(additionalHeaders);
    }

    return headers;
  }

  private void buildHarEntry(
      HttpVerb httpVerb,
      String urlFragment,
      String jsonContent,
      Map<String, String> queryParams,
      HttpResponse<String> httpResponse,
      long waitTime) {
    if (queryParams == null) {
      queryParams = Map.of();
    }
    var headers = httpResponse.request().headers().map();
    var url = buildUrl(urlFragment, null);
    var harRequest = new HarRequestBuilder()
        .withMethod(httpVerb.toString())
        .withUrl(url)
        .withHttpVersion(HTTP_1_1)
        .withPostData(new HarPostData(APPLICATION_JSON, null, jsonContent, EMPTY))
        .withQueryString(queryParams.entrySet().stream()
            .map(q -> new HarQueryString(q.getKey(), q.getValue(), EMPTY)).toList())
        .withHeaders(headers.entrySet().stream()
            .map(q -> new HarHeader(q.getKey(), q.getValue().getFirst(), EMPTY)).toList())
        .build();
    var contentLength = Long.parseLong(
        httpResponse.headers().map().get(HttpHeaders.CONTENT_LENGTH.toLowerCase()).getFirst());
    var harResponse = new HarResponseBuilder().withContent(
            new HarContent(contentLength, 0L, APPLICATION_JSON, httpResponse.body(), EMPTY))
        .withHttpVersion(HTTP_1_1)
        .withHeaders(httpResponse.headers().map().entrySet().stream()
            .map(h -> new HarHeader(h.getKey(), h.getValue().getFirst(), EMPTY)).toList())
        .withStatus(httpResponse.statusCode())
        .build();

    ApiRequestManager.get().addRequest(
        new HarEntryBuilder()
            .withRequest(harRequest)
            .withResponse(harResponse)
            .withTime(waitTime)
            .build());
  }

}
