package it.gov.pagopa.gpd.upload.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

@Factory
public class JacksonFactory {
    @Singleton
    ObjectMapper objectMapper(ObjectMapper mapper) {
        return mapper.registerModule(new JavaTimeModule());
    }
}
