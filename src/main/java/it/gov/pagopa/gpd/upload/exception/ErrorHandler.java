package it.gov.pagopa.gpd.upload.exception;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import it.gov.pagopa.gpd.upload.model.ProblemJson;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import static it.gov.pagopa.gpd.upload.config.LogAspect.*;

@Produces
@Singleton
@Requires(classes = {AppException.class, ExceptionHandler.class})
@Slf4j
public class ErrorHandler implements ExceptionHandler<AppException, HttpResponse> {

    @Override
    public HttpResponse handle(HttpRequest request, AppException exception) {
        ProblemJson errorResponse = ProblemJson.builder()
                .status(exception.getHttpStatus().getCode())
                .title(exception.getTitle())
                .detail(exception.getMessage())
                .build();

        MDC.put(STATUS, "KO");
        MDC.put(CODE, errorResponse.getStatus().toString());
        MDC.put(RESPONSE_TIME, getExecutionTime());
        MDC.put(FAULT_CODE, errorResponse.title);
        MDC.put(FAULT_DETAIL, errorResponse.detail);
        log.error("[ERROR] AppException raised: {} {} {} {}", exception.toString(), exception.getCause(), exception.getMessage(), exception.getLocalizedMessage());
        log.info("Failed API operation {} - error: {}", MDC.get(METHOD), errorResponse);

        return HttpResponse
                .status(exception.getHttpStatus())
                .body(errorResponse);
    }

    public static String getExecutionTime() {
        String startTime = MDC.get(START_TIME);
        if (startTime != null) {
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - Long.parseLong(startTime);
            return String.valueOf(executionTime);
        }
        return "-";
    }

}
