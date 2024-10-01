package filters;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import loggers.FileLogger;

public class RequestResponseLoggingFilter implements Filter {

  @Override
  public Response filter(FilterableRequestSpecification requestSpec,
      FilterableResponseSpecification responseSpec, FilterContext ctx) {
    Response response = ctx.next(requestSpec, responseSpec);
    if (response.statusCode() != 200) {
      FileLogger.instance().get()
          .warning(requestSpec.getMethod() + " " + requestSpec.getURI() + " => " +
              response.getStatusCode() + " " + response.getStatusLine());
    }
    FileLogger.instance().get().info(
        requestSpec.getMethod() + " " + requestSpec.getURI() + " \n Request Body =>"
            + requestSpec.getBody() + "\n Response Status => " +
            response.getStatusCode() + " " + response.getStatusLine() + " \n Response Body => "
            + response.getBody().prettyPrint());
    return response;
  }

}
