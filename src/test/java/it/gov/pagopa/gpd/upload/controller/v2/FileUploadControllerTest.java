package it.gov.pagopa.gpd.upload.controller.v2;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.multipart.MultipartBody;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import it.gov.pagopa.gpd.upload.model.v1.UploadReport;
import it.gov.pagopa.gpd.upload.service.BlobService;
import it.gov.pagopa.gpd.upload.service.StatusService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;

import static io.micronaut.http.HttpStatus.ACCEPTED;
import static it.gov.pagopa.gpd.upload.utils.TestConstants.QUERY_PARAM_SERVICE_TYPE_GPD;
import static it.gov.pagopa.gpd.upload.utils.TestConstants.URI_V2;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@MicronautTest
class FileUploadControllerTest {

    private static final String UPLOAD_KEY = "key";
    @Value("${post.file.response.headers.retry_after.millis}")
    private int retryAfter;

    @Inject
    @Client("/")
    HttpClient client;
    @Inject
    StatusService statusService;
    @Inject
    BlobService blobService;

    @BeforeEach
    void beforeEach() {
        Mockito.when(statusService.getReportV1(anyString(), anyString(), anyString(), any())).thenReturn(UploadReport.builder().build());
        Mockito.when(blobService.upsert(anyString(), anyString(), any(), any(), any())).thenReturn(UPLOAD_KEY);
    }

    @Test
    void createDebtPositionsByFile_OK() throws IOException {
        File file = getTempFile();

        HttpRequest httpRequest = HttpRequest.create(HttpMethod.POST, URI_V2 + QUERY_PARAM_SERVICE_TYPE_GPD)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(MultipartBody.builder()
                        .addPart("file", file.getName(), file)
                        .build());
        HttpResponse<?> response = client.toBlocking().exchange(httpRequest);

        assertNotNull(response);
        assertEquals(ACCEPTED, response.getStatus());
        file.delete();
    }

    @Test
    void updateDebtPositionsByFile_OK() throws IOException {
        File file = getTempFile();

        HttpRequest httpRequest = HttpRequest.create(HttpMethod.PUT, URI_V2 + QUERY_PARAM_SERVICE_TYPE_GPD)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(MultipartBody.builder()
                        .addPart("file", file.getName(), file)
                        .build());
        HttpResponse<?> response = client.toBlocking().exchange(httpRequest);

        assertNotNull(response);
        assertEquals(ACCEPTED, response.getStatus());
        file.delete();
    }

    @Test
    void deleteDebtPositionsByFile_OK() throws IOException {
        File file = getTempFile();

        HttpRequest httpRequest = HttpRequest.create(HttpMethod.DELETE, URI_V2 + QUERY_PARAM_SERVICE_TYPE_GPD)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(MultipartBody.builder()
                        .addPart("file", file.getName(), file)
                        .build());
        HttpResponse<?> response = client.toBlocking().exchange(httpRequest);

        assertNotNull(response);
        assertEquals(ACCEPTED, response.getStatus());
        file.delete();
    }

    File getTempFile() throws IOException {
        // Warning: This will fail on Windows as it doesn't support PosixFilePermissions.
        return Files.createTempFile(
                Path.of("./"), "test", ".zip",
                PosixFilePermissions.asFileAttribute(EnumSet.of(OWNER_READ, OWNER_WRITE)) // permissions `-rw-------`
        ).toFile();
    }
}
