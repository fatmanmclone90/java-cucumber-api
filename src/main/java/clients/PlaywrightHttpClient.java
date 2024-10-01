package clients;

import static java.util.stream.Collectors.toList;

import com.microsoft.playwright.APIRequest.NewContextOptions;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
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
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import loggers.FileLogger;
import managers.ApiRequestManager;
import managers.ConfigurationManager;
import managers.PlaywrightManager;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import utils.JsonSerializer;

/**
 * Wrapper around the Playwright HTTP Client.
 */
public class PlaywrightHttpClient {

  public static final String HTTP_1_1 = "HTTP/1.1";
  private static final String EMPTY = "";
  private static final String APPLICATION_JSON = "application/json";
  private final Level apiLogLevel;

  public PlaywrightHttpClient() {
    this.apiLogLevel = Level.FINE;
  }

  public APIResponse send(
      HttpVerb httpVerb,
      String urlFragment,
      String jsonContent,
      Map<String, String> queryParams,
      Map<String, String> headers) {
    return sendPrivate(
        httpVerb,
        urlFragment,
        jsonContent,
        queryParams,
        headers);
  }

  private APIResponse sendPrivate(
      HttpVerb httpVerb,
      String urlFragment,
      String jsonContent,
      Map<String, String> queryParams,
      Map<String, String> headers) {
    var request = buildRequest(headers);

    var url = buildUrl(urlFragment, queryParams);

    logHttpRequest(
        httpVerb,
        url,
        jsonContent);

    var start = System.nanoTime();
    var httpResponse = switch (httpVerb) {
      case HttpVerb.GET -> request.get(url);
      case HttpVerb.POST -> request.post(
          url,
          RequestOptions.create().setData(jsonContent));
      case HttpVerb.PUT -> request.put(
          url,
          RequestOptions.create().setData(jsonContent));
    };
    var waitTime = (System.nanoTime() - start) / 1000_000;
    FileLogger.instance().get().fine(String.format("Action Executed in %s ms", waitTime));

    logHttpResponse(httpResponse);

    buildHarEntry(httpVerb, urlFragment, jsonContent, queryParams, headers, httpResponse, waitTime);

    return httpResponse;
  }

  private APIRequestContext buildRequest(Map<String, String> additionalHeaders) {
    var baseUrl = getBaseUrl();
    FileLogger.instance().get().fine(
        String.format("Created API Request Object for URL : %s%n", baseUrl));

    var headers = setHeaders(additionalHeaders);
    var playwright = PlaywrightManager.get().playwright();
    return playwright.request().newContext(
        new NewContextOptions()
            .setBaseURL(baseUrl)
            .setExtraHTTPHeaders(headers)
            .setTimeout(
                ConfigurationManager.get().environment()
                    .asRequiredInteger(Configuration.API_TIMEOUT))
            .setIgnoreHTTPSErrors(true));
  }

  private String getBaseUrl() {
    URIBuilder builder = null;
    try {
      builder = new URIBuilder(ensureTrailingSlash(
          ConfigurationManager.get().environment().asRequiredString(Configuration.API_BASE_URL)));
    } catch (URISyntaxException e) {
      throw new ConfigurationError(e);
    }
    return ensureTrailingSlash(builder.toString());
  }

  private Map<String, String> setHeaders(Map<String, String> additionalHeaders) {
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

  private String buildUrl(String urlFragment, Map<String, String> queryParams) {
    urlFragment = removeLeadingSlash(urlFragment);

    if (queryParams != null && !queryParams.isEmpty()) {
      URIBuilder builder = null;
      try {
        builder = new URIBuilder(urlFragment);
      } catch (URISyntaxException e) {
        throw new ConfigurationError(e);
      }
      builder.addParameters(queryParams.entrySet()
          .stream()
          .map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue()))
          .collect(toList()));
      return builder.toString();
    }

    return urlFragment;
  }

  /**
   * Playwright requires no leading slash for a relative URL.
   *
   * @param urlFragment The URL fragment.
   * @return The URL fragment with leading slash removed if present.
   */
  private String removeLeadingSlash(String urlFragment) {
    if (urlFragment.startsWith("/")) {
      return urlFragment.substring(1);
    } else {
      return urlFragment;
    }
  }

  /**
   * URLBuilder Playwright requires trailing slash on base URL.
   *
   * @param urlFragment The URL fragment.
   * @return The URL fragment with trailing slash added if not present.
   */
  private String ensureTrailingSlash(String urlFragment) {
    if (!urlFragment.endsWith("/")) {
      return urlFragment + "/";
    } else {
      return urlFragment;
    }
  }

  private void logHttpResponse(APIResponse httpResponse) {
    var content =
        Objects.equals(
            httpResponse.headers().get(HttpHeaders.CONTENT_TYPE.toLowerCase()),
            "application/json")
            ? JsonSerializer.prettyPrint(httpResponse.text())
            : httpResponse.text();

    FileLogger.instance().get().log(
        this.apiLogLevel,
        String.format(
            "Received API Response with Status {%s} and Content: {%s%n}",
            httpResponse.status(),
            content));
  }

  private void logHttpRequest(
      HttpVerb httpVerb,
      String url,
      String jsonContent) {
    FileLogger.instance().get().log(
        this.apiLogLevel,
        String.format(
            "Sending %s API Request with Route: %s with Content: %s%n",
            httpVerb,
            url,
            jsonContent != null ? jsonContent : "none"));
  }

  private void buildHarEntry(
      HttpVerb httpVerb,
      String urlFragment,
      String jsonContent,
      Map<String, String> queryParams,
      Map<String, String> headers,
      APIResponse httpResponse,
      long waitTime) {
    if (queryParams == null) {
      queryParams = Map.of();
    }
    if (headers == null) {
      headers = Map.of();
    }
    var baseUrl = getBaseUrl();
    var url = buildUrl(urlFragment, null);
    var fullUrl = baseUrl + url;
    var harRequest = new HarRequestBuilder()
        .withMethod(httpVerb.toString())
        .withUrl(fullUrl)
        .withHttpVersion(HTTP_1_1)
        .withPostData(new HarPostData(APPLICATION_JSON, null, jsonContent, EMPTY))
        .withQueryString(queryParams.entrySet().stream()
            .map(q -> new HarQueryString(q.getKey(), q.getValue(), EMPTY)).toList())
        .withHeaders(headers.entrySet().stream()
            .map(q -> new HarHeader(q.getKey(), q.getValue(), EMPTY)).toList())
        .build();
    var contentLength = Long.parseLong(
        httpResponse.headers().get(HttpHeaders.CONTENT_LENGTH.toLowerCase()));
    var harResponse = new HarResponseBuilder().withContent(
            new HarContent(contentLength, 0L, APPLICATION_JSON, httpResponse.text(), EMPTY))
        .withHttpVersion(HTTP_1_1)
        .withHeaders(httpResponse.headers().entrySet().stream()
            .map(h -> new HarHeader(h.getKey(), h.getValue(), EMPTY)).toList())
        .withStatus(httpResponse.status())
        .build();

    ApiRequestManager.get().addRequest(
        new HarEntryBuilder()
            .withRequest(harRequest)
            .withResponse(harResponse)
            .withTime(waitTime)
            .build());
  }

}
