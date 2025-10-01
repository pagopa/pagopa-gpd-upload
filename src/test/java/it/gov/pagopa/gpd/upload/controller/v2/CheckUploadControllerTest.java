package it.gov.pagopa.gpd.upload.controller.v2;

import io.micronaut.http.*;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import it.gov.pagopa.gpd.upload.exception.AppException;
import it.gov.pagopa.gpd.upload.model.FileIdListResponse;
import it.gov.pagopa.gpd.upload.model.ProblemJson;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import it.gov.pagopa.gpd.upload.model.v2.UploadReportDTO;
import it.gov.pagopa.gpd.upload.model.v2.UploadStatusDTO;
import it.gov.pagopa.gpd.upload.service.BlobService;
import it.gov.pagopa.gpd.upload.service.StatusService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static io.micronaut.http.HttpStatus.NOT_FOUND;
import static io.micronaut.http.HttpStatus.OK;
import static it.gov.pagopa.gpd.upload.utils.TestConstants.QUERY_PARAM_SERVICE_TYPE_GPD;
import static it.gov.pagopa.gpd.upload.utils.TestConstants.URI_V2;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@MicronautTest(environments = "test")
class CheckUploadControllerTest {

    public static final String FILE_ID = "fileID";
    public static final String BROKER_ID = "broker-ID";
    public static final String ORG_FISCAL_CODE = "fiscal-code";

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    StatusService statusServiceMock;
    @Inject
    BlobService blobServiceMock;

    @BeforeEach
    void beforeEach() {
        reset(statusServiceMock, blobServiceMock);
    }

    @Test
    void getUploadStatus_OK() {
        when(statusServiceMock.getUploadStatusV2(BROKER_ID, FILE_ID, ORG_FISCAL_CODE, ServiceType.GPD))
                .thenReturn(UploadStatusDTO.builder().fileId(FILE_ID).build());

        HttpRequest httpRequest = HttpRequest.create(HttpMethod.GET, URI_V2 + "/" + FILE_ID + "/status" + QUERY_PARAM_SERVICE_TYPE_GPD);
        HttpResponse<UploadStatusDTO> response = client.toBlocking().exchange(httpRequest);

        verify(statusServiceMock, times(1)).getUploadStatusV2(
                BROKER_ID,
                FILE_ID,
                ORG_FISCAL_CODE,
                ServiceType.GPD
        );
        assertNotNull(response);
        assertEquals(OK, response.getStatus());
        assertNotNull(response.getBody(UploadStatusDTO.class));
        assertEquals(FILE_ID, response.getBody(UploadStatusDTO.class).get().getFileId());
    }

    @Test
    void getUploadStatus_KO() {
        when(statusServiceMock.getUploadStatusV2(
                BROKER_ID,
                FILE_ID,
                ORG_FISCAL_CODE,
                ServiceType.GPD
        )).thenThrow(AppException.class);

        HttpRequest httpRequest = HttpRequest.create(HttpMethod.GET, URI_V2 + "/" + FILE_ID + "/status" + QUERY_PARAM_SERVICE_TYPE_GPD);
        BlockingHttpClient blockingClient = client.toBlocking();
        assertThrows(HttpClientResponseException.class, () -> blockingClient.exchange(httpRequest));

        verify(statusServiceMock, times(1)).getUploadStatusV2(
                BROKER_ID,
                FILE_ID,
                ORG_FISCAL_CODE,
                ServiceType.GPD
        );
    }

    @Test
    void getUploadReport_withStatus_OK() {
        when(statusServiceMock.getReportV2(BROKER_ID, ORG_FISCAL_CODE, FILE_ID, ServiceType.GPD))
                .thenReturn(UploadReportDTO.builder().fileId(FILE_ID).endTime(LocalDateTime.now()).build());

        HttpRequest httpRequest = HttpRequest.create(HttpMethod.GET, URI_V2 + "/" + FILE_ID + "/report" + QUERY_PARAM_SERVICE_TYPE_GPD);
        HttpResponse<UploadStatusDTO> response = client.toBlocking().exchange(httpRequest);

        verify(statusServiceMock, times(1)).getReportV2(BROKER_ID, ORG_FISCAL_CODE, FILE_ID, ServiceType.GPD);
        verify(blobServiceMock, never()).getReportV2(
                any(),
                any(),
                any(),
                any());
        assertNotNull(response);
        assertEquals(OK, response.getStatus());
        assertNotNull(response.getBody(UploadReportDTO.class));
        assertEquals(FILE_ID, response.getBody(UploadReportDTO.class).get().getFileId());
    }

    @Test
    void getUploadReport_withoutStatusRetrieveBlobReport_OK() {
        AppException ex = new AppException(NOT_FOUND, "error", "error");
        when(statusServiceMock.getReportV2(BROKER_ID, ORG_FISCAL_CODE, FILE_ID, ServiceType.GPD)).thenThrow(ex);
        when(blobServiceMock.getReportV2(BROKER_ID, ORG_FISCAL_CODE, FILE_ID, ServiceType.GPD))
                .thenReturn(UploadReportDTO.builder().fileId(FILE_ID).endTime(LocalDateTime.now()).build());

        HttpRequest httpRequest = HttpRequest.create(HttpMethod.GET, URI_V2 + "/" + FILE_ID + "/report" + QUERY_PARAM_SERVICE_TYPE_GPD);
        HttpResponse<UploadStatusDTO> response = client.toBlocking().exchange(httpRequest);

        verify(statusServiceMock, times(1)).getReportV2(BROKER_ID, ORG_FISCAL_CODE, FILE_ID, ServiceType.GPD);
        verify(blobServiceMock, times(1)).getReportV2(BROKER_ID, ORG_FISCAL_CODE, FILE_ID, ServiceType.GPD);
        assertNotNull(response);
        assertEquals(OK, response.getStatus());
        assertNotNull(response.getBody(UploadReportDTO.class));
        assertEquals(FILE_ID, response.getBody(UploadReportDTO.class).get().getFileId());
    }

    @Test
    void getUploadReport_byStatus_KO() {
        when(statusServiceMock.getReportV2(BROKER_ID, ORG_FISCAL_CODE, FILE_ID, ServiceType.GPD)).thenThrow(AppException.class);

        HttpRequest httpRequest = HttpRequest.create(HttpMethod.GET, URI_V2 + "/" + FILE_ID + "/report" + QUERY_PARAM_SERVICE_TYPE_GPD);
        BlockingHttpClient blockingClient = client.toBlocking();
        HttpClientResponseException response = assertThrows(HttpClientResponseException.class, () -> blockingClient.exchange(httpRequest));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
        verify(statusServiceMock, times(1)).getReportV2(BROKER_ID, ORG_FISCAL_CODE, FILE_ID, ServiceType.GPD);
        verify(blobServiceMock, never()).getReportV2(
                any(),
                any(),
                any(),
                any());
    }

    @Test
    void getUploadReport_byBlobReport_KO() {
        AppException ex = new AppException(NOT_FOUND, "error", "error");
        when(statusServiceMock.getReportV2(BROKER_ID, ORG_FISCAL_CODE, FILE_ID, ServiceType.GPD)).thenThrow(ex);
        when(blobServiceMock.getReportV2(BROKER_ID, ORG_FISCAL_CODE, FILE_ID, ServiceType.GPD)).thenThrow(ex);

        HttpRequest httpRequest = HttpRequest.create(HttpMethod.GET, URI_V2 + "/" + FILE_ID + "/report" + QUERY_PARAM_SERVICE_TYPE_GPD);
        BlockingHttpClient blockingClient = client.toBlocking();
        HttpClientResponseException response = assertThrows(HttpClientResponseException.class, () -> blockingClient.exchange(httpRequest));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatus());
        verify(statusServiceMock, times(1)).getReportV2(BROKER_ID, ORG_FISCAL_CODE, FILE_ID, ServiceType.GPD);
        verify(blobServiceMock, times(1)).getReportV2(BROKER_ID, ORG_FISCAL_CODE, FILE_ID, ServiceType.GPD);
    }

    @Test
    void getUploadReport_TooEarly_KO() {
        when(statusServiceMock.getReportV2(BROKER_ID, ORG_FISCAL_CODE, FILE_ID, ServiceType.GPD))
                .thenReturn(UploadReportDTO.builder().fileId(FILE_ID).build());

        HttpRequest httpRequest = HttpRequest.create(HttpMethod.GET, URI_V2 + "/" + FILE_ID + "/report" + QUERY_PARAM_SERVICE_TYPE_GPD);
        BlockingHttpClient blockingClient = client.toBlocking();
        HttpClientResponseException response = assertThrows(HttpClientResponseException.class, () -> blockingClient.exchange(httpRequest));

        assertEquals(HttpStatus.TOO_EARLY, response.getStatus());
        verify(statusServiceMock, times(1)).getReportV2(BROKER_ID, ORG_FISCAL_CODE, FILE_ID, ServiceType.GPD);
    }

    @Test
    void getFileIdList_shouldReturn200WithBodyAndContinuationHeader() {
        FileIdListResponse stub = FileIdListResponse.builder()
                .fileIds(List.of("id1", "id2"))
                .size(2)
                .hasMore(true)
                .continuationToken("next-token-123")
                .build();

        when(statusServiceMock.getFileIdList(
                eq(BROKER_ID),
                eq(ORG_FISCAL_CODE),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(100),
                isNull(),
                eq(ServiceType.GPD)
        )).thenReturn(stub);

        String url = URI_V2 + "s?from=2025-09-01&to=2025-09-06&size=100";

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
                eq(BROKER_ID),
                eq(ORG_FISCAL_CODE),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(100),
                isNull(),
                eq(ServiceType.GPD)
        );
    }

    @Test
    void getFileIdList_shouldPassContinuationTokenToService() {

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
        String url = URI_V2 + "s?from=2025-09-01&to=2025-09-06&size=100";
        HttpRequest<?> req = HttpRequest.GET(url)
                .header("x-continuation-token", inToken)
                .contentType(MediaType.APPLICATION_JSON);

        HttpResponse<FileIdListResponse> resp =
                client.toBlocking().exchange(req, FileIdListResponse.class);

        // Assert
        assertEquals(HttpStatus.OK, resp.getStatus());

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(statusServiceMock).getFileIdList(
                eq(BROKER_ID),
                eq(ORG_FISCAL_CODE),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(100),
                tokenCaptor.capture(),
                eq(ServiceType.GPD)
        );
        assertEquals(inToken, tokenCaptor.getValue());
    }

    @Test
    void getFileIdList_shouldReturn400WhenRangeTooLarge() {
        // 10 days > 7
        String url = URI_V2 + "s?from=2025-09-01&to=2025-09-10&size=100";
        HttpRequest httpRequest = HttpRequest.create(HttpMethod.GET, url);

        BlockingHttpClient blockingClient = client.toBlocking();
        HttpClientResponseException ex = assertThrows(
                HttpClientResponseException.class,
                () -> blockingClient.exchange(httpRequest, FileIdListResponse.class)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        ProblemJson problem = ex.getResponse().getBody(ProblemJson.class).orElse(null);
        assertNotNull(problem);
        assertEquals(400, problem.getStatus());
        assertTrue(problem.getDetail().toLowerCase().contains("invalid range"));
        verifyNoInteractions(statusServiceMock);
    }

    @Test
    void getFileIdList_shouldReturn400WhenSizeOutOfBounds() {
        // size < 100
        String url = URI_V2 + "s?from=2025-09-01&to=2025-09-06&size=50";
        HttpRequest<?> req = HttpRequest.GET(url).contentType(MediaType.APPLICATION_JSON);

        BlockingHttpClient blockingClient = client.toBlocking();
        HttpClientResponseException ex = assertThrows(
                HttpClientResponseException.class,
                () -> blockingClient.exchange(req, FileIdListResponse.class)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        ProblemJson problem = ex.getResponse().getBody(ProblemJson.class).orElse(null);
        assertNotNull(problem);
        assertEquals(400, problem.getStatus());
        assertTrue(problem.getDetail().toLowerCase().contains("invalid size"));
        verifyNoInteractions(statusServiceMock);
    }
}
