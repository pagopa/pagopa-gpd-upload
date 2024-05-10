package it.gov.pagopa.gpd.upload.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.reactivex.rxjava3.core.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.slf4j.MDC;

import java.util.UUID;


@Filter("/*")
@Slf4j
public class LogAspect implements HttpServerFilter {
    public static final String START_TIME = "startTime";
    public static final String METHOD = "method";
    public static final String STATUS = "status";
    public static final String CODE = "httpCode";
    public static final String RESPONSE_TIME = "responseTime";
    public static final String RESPONSE = "response";
    public static final String FAULT_CODE = "faultCode";
    public static final String FAULT_DETAIL = "faultDetail";
    public static final String REQUEST_ID = "requestId";
    public static final String OPERATION_ID = "operationId";
    public static final String ARGS = "args";

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        long startTime = System.currentTimeMillis();
        String path = request.getPath();
        MDC.put(METHOD, request.getMethod() + " " + path.substring(path.lastIndexOf('/')));
        MDC.put(START_TIME, String.valueOf(System.currentTimeMillis()));
        MDC.put(OPERATION_ID, UUID.randomUUID().toString());
        if(MDC.get(REQUEST_ID) == null) {
            var requestId = UUID.randomUUID().toString();
            MDC.put(REQUEST_ID, requestId);
        }
        String params = request.getParameters().asMap().toString();
        MDC.put(ARGS, params);

        log.info("Invoking API operation {} - args: {}", request.getMethodName(), params);

        return Flowable.fromPublisher(chain.proceed(request)).flatMap(response -> {

            MDC.put(STATUS, "OK");
            MDC.put(CODE, String.valueOf(response.getStatus().getCode()));
            MDC.put(RESPONSE_TIME, String.valueOf(System.currentTimeMillis() - startTime));
            MDC.put(RESPONSE, toJsonString(response.getBody().toString()));
            log.info("Successful API operation {} - result: {}", request.getMethodName(), response);
            MDC.remove(RESPONSE);
            MDC.remove(STATUS);
            MDC.remove(CODE);
            MDC.remove(RESPONSE_TIME);
            MDC.remove(START_TIME);

            return Flowable.just(response);
        });
    }

    private static String toJsonString(Object param) {
        try {
            return new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .writeValueAsString(param);
        } catch (JsonProcessingException e) {
            log.warn("An error occurred when trying to parse a parameter", e);
            return "parsing error";
        }
    }
}