package it.gov.pagopa.gpd.upload;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.OpenAPIV3Parser;
import it.gov.pagopa.gpd.upload.repository.BlobStorageRepository;
import it.gov.pagopa.gpd.upload.repository.StatusRepository;
import it.gov.pagopa.gpd.upload.service.BlobService;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
@Slf4j
class OpenApiGenerationTest {

    @Value("${info.application.version}")
    private String version;

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void swaggerSpringPlugin() throws Exception {
        boolean resultV1 = saveOpenAPI("gpd", "v1", "/swagger/pagopa-gpd-upload-v1.json", false);
        assertTrue(resultV1);

        boolean resultV2 = saveOpenAPI("gpd", "v2", "/swagger/pagopa-gpd-upload-v2.json", false);
        assertTrue(resultV2);

        boolean resultV3 = saveOpenAPI("aca", "v2", "/swagger/pagopa-gpd-upload-v2.json", false);
        assertTrue(resultV3);

        boolean resultSupportAPI = saveOpenAPI("internal", "v1", "/swagger/pagopa-gpd-upload-v2.json", true);
        assertTrue(resultSupportAPI);
    }

    private boolean saveOpenAPI(String domain, String apimVersion, String fromUri, boolean isInternal) throws IOException {
        String toFile = String.format("%s-openapi-%s.json", domain, apimVersion);
        String newTitle = isInternal
                ? String.format("GPD-Upload-Support-API-%s", apimVersion)
                : String.format("%s-Upload-API-%s", domain.toUpperCase(), apimVersion);

        // retrieve openapi
        HttpResponse<String> response = client.toBlocking().exchange(fromUri, String.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
        OpenAPI openAPI = objectMapper.readValue(response.getBody().get(), OpenAPI.class);

        openAPI.getInfo().setTitle(newTitle);
        openAPI.getInfo().setVersion(version);

        // server management
        List<Server> servers = openAPI.getServers();
        List<Server> serversToReplace = new ArrayList<>();
        if (servers != null) {
            for (Server server : servers) {
                if (!server.getUrl().contains("localhost")) {
                    server.getVariables().forEach((key, value) -> {
                        log.info(key + " " + value);
                        if (key.equals("basePath")) {
                            if (!isInternal) {
                                // base case
                                String bp = value.getDefault().replace("upload/gpd", "upload/" + domain);
                                bp = switchVersion(bp, apimVersion);
                                value.setDefault(bp);
                            }
                            // update enum
                            List<String> enumerator = new ArrayList<>();
                            for (String e : value.getEnum()) {
                                if (shouldKeepEnum(e, domain, isInternal)) {
                                    enumerator.add(switchVersion(e, apimVersion));
                                }
                            }
                            value.setEnum(enumerator);
                        }
                    });
                    serversToReplace.add(server);
                }
            }
        }
        openAPI.setServers(serversToReplace);

        // path update
        io.swagger.v3.oas.models.Paths updated = new io.swagger.v3.oas.models.Paths();
        openAPI.getPaths().forEach((key, value) -> {
            boolean keep = !isInternal || !key.endsWith("file");
            if (keep) {
                String newKey = apimVersion.equals("v2") ? key.replace("/v2", "") : key;
                updated.addPathItem(newKey, value);
            }
        });
        openAPI.setPaths(updated);

        // save file
        Path basePath = Paths.get("openapi/");
        Files.createDirectories(basePath);
        Files.write(basePath.resolve(toFile), Json.pretty(openAPI).getBytes());
        return true;
    }

    private String switchVersion(String text, String apimVersion) {
        if (apimVersion.equals("v1")) {
            return text.replace("/v2", "/v1");
        } else if (apimVersion.equals("v2")) {
            return text.replace("/v1", "/v2");
        }
        return text;
    }

    private boolean shouldKeepEnum(String e, String domain, boolean isInternal) {
        if (isInternal) {
            return e.contains("gpd");
        }
        return e.contains(domain);
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

    private String removeV2FromPath(String openApiContent) {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        io.swagger.v3.oas.models.Paths updated = new io.swagger.v3.oas.models.Paths();

        OpenAPI result = parser.readContents(openApiContent).getOpenAPI();
        io.swagger.v3.oas.models.Paths paths = result.getPaths();
        paths.forEach(
                (k, v) -> {
                    if (k.contains("/v2")) {
                        updated.addPathItem(k.replace("/v2", ""), v);
                    } else {
                        updated.addPathItem(k, v);
                    }
                });
        result.setPaths(updated);
        return Json.pretty(result);
    }
}
