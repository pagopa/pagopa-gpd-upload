package it.gov.pagopa.gpd.upload;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import it.gov.pagopa.gpd.upload.repository.BlobStorageRepository;
import it.gov.pagopa.gpd.upload.repository.StatusRepository;
import it.gov.pagopa.gpd.upload.service.BlobService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
class OpenApiGenerationTest {

    @Value("${openapi.application.version}")
    private String version;

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void swaggerSpringPlugin() throws Exception {
        boolean result = saveOpenAPI("/swagger/pagopa-gpd-upload-" + version + ".json", "openapi.json");

        assertTrue(result);
    }

        private boolean saveOpenAPI(String fromUri, String toFile) throws IOException {
            HttpResponse<String> response = client.toBlocking().exchange(fromUri, String.class);
            ObjectMapper objectMapper = new ObjectMapper();
            String responseBody = response.getBody().get();
            Object openAPI = objectMapper.readValue(responseBody, Object.class);
            String formatted = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(openAPI);
            Path basePath = Paths.get("openapi/");
            Files.createDirectories(basePath);
            Files.write(basePath.resolve(toFile), formatted.getBytes());
            return true;
        }

    @Bean
    @Primary
    public BlobService fileUploadService() throws IOException {
        return Mockito.mock(BlobService.class);
    }

    // real repositories are out of scope for this test, @PostConstruct init routine requires connection-string
    @Bean
    @Primary
    public BlobStorageRepository blobStorageRepository() {
        return Mockito.mock(BlobStorageRepository.class);
    }
    @Bean
    @Primary
    public StatusRepository statusRepository() {
        return Mockito.mock(StatusRepository.class);
    }
}
