package it.gov.pagopa.gpd.upload.config;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;

@Slf4j
@Filter("/**")
public class RequestFilter implements HttpServerFilter {
    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        log.info("Request: " + request.getMethodName() + request.getPath() + ", content-length: " + request.getContentLength());
        request.getHeaders().forEach(h -> log.debug(
                "header: " + h.getKey() + " = " + h.getValue())
        );
        return chain.proceed(request);
    }
}
