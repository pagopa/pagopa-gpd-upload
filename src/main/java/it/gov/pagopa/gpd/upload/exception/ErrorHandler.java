package it.gov.pagopa.gpd.upload.exception;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import it.gov.pagopa.gpd.upload.model.ProblemJson;
import jakarta.inject.Singleton;

@Produces
@Singleton
@Requires(classes = {AppException.class, ExceptionHandler.class})
public class ErrorHandler implements ExceptionHandler<AppException, HttpResponse> {

    @Override
    public HttpResponse handle(HttpRequest request, AppException exception) {
        ProblemJson errorResponse = ProblemJson.builder()
                .status(exception.getHttpStatus().getCode())
                .title(exception.getTitle())
                .detail(exception.getMessage())
                .build();
        return HttpResponse
                .status(exception.getHttpStatus())
                .body(errorResponse);
    }
}