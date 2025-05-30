package it.gov.pagopa.gpd.upload.controller;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.*;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.multipart.MultipartBody;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import it.gov.pagopa.gpd.upload.model.UploadReport;
import it.gov.pagopa.gpd.upload.repository.BlobStorageRepository;
import it.gov.pagopa.gpd.upload.repository.StatusRepository;
import it.gov.pagopa.gpd.upload.service.BlobService;

import it.gov.pagopa.gpd.upload.service.StatusService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;

import static io.micronaut.http.HttpStatus.*;
import static java.nio.file.attribute.PosixFilePermission.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;

@MicronautTest
class FileUploadControllerTest {

    private static final String URI = "brokers/broker-ID/organizations/fiscal-code/debtpositions/file";
    private static final String UPLOAD_KEY = "key";
    @Value("${post.file.response.headers.retry_after.millis}")
    private int retryAfter;

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void createDebtPositionsByFile_OK() throws IOException {
        File file = getTempFile();

        HttpRequest httpRequest = HttpRequest.create(HttpMethod.POST, URI)
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

        HttpRequest httpRequest = HttpRequest.create(HttpMethod.PUT, URI)
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

        HttpRequest httpRequest = HttpRequest.create(HttpMethod.DELETE, URI)
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

    @Test
    void getUploadStatus_KO() throws IOException {
        this.createDebtPositionsByFile_OK();

        HttpRequest httpRequest = HttpRequest.create(HttpMethod.GET, URI + "/fileID" + "/report");
        HttpResponse<?> response = client.toBlocking().exchange(httpRequest);

        assertNotNull(response);
        assertEquals(OK, response.getStatus());
    }

    ////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////MOCK//////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////

    @Bean
    @Primary
    public BlobService fileUploadService() throws IOException {
        BlobService blobService = Mockito.mock(BlobService.class);
        Mockito.when(blobService.upsert(anyString(), anyString(), Mockito.any(), Mockito.any())).thenReturn(UPLOAD_KEY);
        return blobService;
    }

    @Bean
    @Primary
    public StatusService statusService() throws IOException {
        StatusService statusService = Mockito.mock(StatusService.class);
        Mockito.when(statusService.getReport(anyString(), anyString())).thenReturn(UploadReport.builder().build());
        return statusService;
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
