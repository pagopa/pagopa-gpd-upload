package it.gov.pagopa.gpd.upload.controller.v2;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import it.gov.pagopa.gpd.upload.model.FileIdListResponse;
import it.gov.pagopa.gpd.upload.model.ProblemJson;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import it.gov.pagopa.gpd.upload.repository.BlobStorageRepository;
import it.gov.pagopa.gpd.upload.repository.StatusRepository;
import it.gov.pagopa.gpd.upload.service.BlobService;
import it.gov.pagopa.gpd.upload.service.StatusService;

@MicronautTest(environments = "test")
class CheckUploadControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    StatusService statusServiceMock; 

    // ===== MOCK BEANS ===========================
    @MockBean(StatusService.class)
    @Replaces(StatusService.class)
    StatusService mockStatusService() {
        return mock(StatusService.class);
    }

    @MockBean(StatusRepository.class)
    @Replaces(StatusRepository.class)
    StatusRepository mockStatusRepository() {
        return mock(StatusRepository.class);
    }

    @MockBean(BlobStorageRepository.class)
    @Replaces(BlobStorageRepository.class)
    BlobStorageRepository mockBlobStorageRepository() {
        return mock(BlobStorageRepository.class);
    }

    @MockBean(BlobService.class)
    @Replaces(BlobService.class)
    BlobService mockBlobService() {
        return mock(BlobService.class);
    }
    // ========================================================================

    private static final String BASE =
            "v2/brokers/brokertest/organizations/77777777777/debtpositions/files";

    @Test
    void shouldReturn200WithBodyAndContinuationHeader() {
        FileIdListResponse stub = FileIdListResponse.builder()
                .fileIds(List.of("id1", "id2"))
                .size(2)              
                .hasMore(true)
                .continuationToken("next-token-123")
                .build();

        when(statusServiceMock.getFileIdList(
                eq("brokertest"),
                eq("77777777777"),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(100),
                isNull(),
                eq(ServiceType.GPD)
        )).thenReturn(stub);

        String url = BASE + "?from=2025-09-01&to=2025-09-06&size=100";

        HttpRequest<?> req = HttpRequest.GET(url).contentType(MediaType.APPLICATION_JSON);

        HttpResponse<FileIdListResponse> resp =
                client.toBlocking().exchange(req, FileIdListResponse.class);

        // Assert
        assertEquals(HttpStatus.OK, resp.getStatus());
        assertTrue(resp.getBody().isPresent());
        FileIdListResponse body = resp.getBody().get();

        assertEquals(List.of("id1", "id2"), body.getFileIds());
        assertEquals(2, body.getSize());
        assertTrue(body.isHasMore());
        assertEquals("next-token-123", resp.getHeaders().get("x-continuation-token"));

        verify(statusServiceMock, times(1)).getFileIdList(
                eq("brokertest"),
                eq("77777777777"),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(100),
                isNull(),
                eq(ServiceType.GPD)
        );
    }

    @Test
    void shouldPassContinuationTokenToService() {
    	
        FileIdListResponse stub = FileIdListResponse.builder()
                .fileIds(List.of("id1"))
                .size(1)
                .hasMore(false)
                .continuationToken(null)
                .build();

        when(statusServiceMock.getFileIdList(anyString(), anyString(),
                any(LocalDate.class), any(LocalDate.class),
                anyInt(), any(), any(ServiceType.class)))
                .thenReturn(stub);

        String inToken = "opaque-token-xyz";
        String url = BASE + "?from=2025-09-01&to=2025-09-06&size=100";
        HttpRequest<?> req = HttpRequest.GET(url)
                .header("x-continuation-token", inToken)
                .contentType(MediaType.APPLICATION_JSON);

        HttpResponse<FileIdListResponse> resp =
                client.toBlocking().exchange(req, FileIdListResponse.class);

        // Assert
        assertEquals(HttpStatus.OK, resp.getStatus());

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(statusServiceMock).getFileIdList(
                eq("brokertest"),
                eq("77777777777"),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(100),
                tokenCaptor.capture(),
                eq(ServiceType.GPD)
        );
        assertEquals(inToken, tokenCaptor.getValue());
    }

    @Test
    void shouldReturn400WhenRangeTooLarge() {
        // 10 days > 7
        String url = BASE + "?from=2025-09-01&to=2025-09-10&size=100";
        HttpRequest<?> req = HttpRequest.GET(url).contentType(MediaType.APPLICATION_JSON);

        HttpClientResponseException ex = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(req, FileIdListResponse.class)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        ProblemJson problem = ex.getResponse().getBody(ProblemJson.class).orElse(null);
        assertNotNull(problem);
        assertEquals(400, problem.getStatus());
        assertTrue(problem.getDetail().toLowerCase().contains("invalid range"));
        verifyNoInteractions(statusServiceMock);
    }

    @Test
    void shouldReturn400WhenSizeOutOfBounds() {
        // size < 100
        String url = BASE + "?from=2025-09-01&to=2025-09-06&size=50";
        HttpRequest<?> req = HttpRequest.GET(url).contentType(MediaType.APPLICATION_JSON);

        HttpClientResponseException ex = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(req, FileIdListResponse.class)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        ProblemJson problem = ex.getResponse().getBody(ProblemJson.class).orElse(null);
        assertNotNull(problem);
        assertEquals(400, problem.getStatus());
        assertTrue(problem.getDetail().toLowerCase().contains("invalid size"));
        verifyNoInteractions(statusServiceMock);
    }
}