package it.gov.pagopa.gpd.upload.service;

import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import it.gov.pagopa.gpd.upload.client.GPDClient;
import it.gov.pagopa.gpd.upload.entity.Status;
import it.gov.pagopa.gpd.upload.entity.Upload;
import it.gov.pagopa.gpd.upload.model.ProblemJson;
import it.gov.pagopa.gpd.upload.model.UploadInput;
import it.gov.pagopa.gpd.upload.model.UploadOperation;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import it.gov.pagopa.gpd.upload.model.pd.PaymentPositionModel;
import it.gov.pagopa.gpd.upload.repository.StatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Spy;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SupportServiceTest {

    @Spy
    private ZoneId zone = ZoneId.of("Europe/Rome");

    static final String CREATE_UPLOAD_ID = "upload-id-create";
    static final String DELETE_UPLOAD_ID = "upload-id-delete";
    private final LocalDateTime fromTime = LocalDateTime.of(2023, 10, 1, 0, 0);
    private final LocalDateTime toTime = LocalDateTime.of(2023, 10, 2, 0, 0);

    private File tempFileCreated = null;

    StatusRepository statusRepository = mock(StatusRepository.class);
    StatusService statusService = mock(StatusService.class);
    BlobService blobService = mock(BlobService.class);
    GPDClient gpdClient = mock(GPDClient.class);
    SlackService slackService = mock(SlackService.class);
    SupportService supportService = new SupportService(statusRepository, statusService, blobService, gpdClient, slackService);

    @BeforeEach
    void beforeEach() {
        // Mock BlobService
        UploadInput uploadInputDelete = UploadInput.builder()
                .uploadOperation(UploadOperation.DELETE)
                .paymentPositionIUPDs(List.of("IUPD-1"))
                .build();
        UploadInput uploadInputCreate = UploadInput.builder()
                .uploadOperation(UploadOperation.CREATE)
                .paymentPositions(List.of(PaymentPositionModel.builder()
                        .iupd("IUPD-1").build()))
                .build();
        when(blobService.getUploadInput(anyString(), anyString(), eq(CREATE_UPLOAD_ID), any(ServiceType.class))).thenReturn(uploadInputCreate);
        when(blobService.getUploadInput(anyString(), anyString(), eq(DELETE_UPLOAD_ID), any(ServiceType.class))).thenReturn(uploadInputDelete);

        // Mock StatusService
        when(statusService.getStatus(anyString(), eq(DELETE_UPLOAD_ID))).thenReturn(
                Status.builder()
                        .id("UPLOAD_KEY")
                        .brokerID("broker")
                        .fiscalCode("organization")
                        .upload(Upload.builder()
                                .current(0)
                                .total(1)
                                .responses(new ArrayList<>())
                                .start(LocalDateTime.now().minusHours(1))
                                .end(LocalDateTime.now())
                                .build())
                        .build()
        );
        when(statusService.getStatus(anyString(), eq(CREATE_UPLOAD_ID))).thenReturn(
                Status.builder()
                        .id("UPLOAD_KEY")
                        .brokerID("broker")
                        .fiscalCode("organization")
                        .upload(Upload.builder()
                                .current(0)
                                .total(1)
                                .responses(new ArrayList<>())
                                .start(LocalDateTime.now().minusHours(1))
                                .end(LocalDateTime.now())
                                .build())
                        .build()
        );
        when(statusService.upsert(any())).thenReturn(
                Status.builder()
                        .id("UPLOAD_KEY")
                        .brokerID("broker")
                        .fiscalCode("organization")
                        .upload(Upload.builder()
                                .current(1)
                                .total(1)
                                .start(LocalDateTime.now().minusHours(1))
                                .end(LocalDateTime.now())
                                .build())
                        .build()
        );

        // Mock gpd client
        HttpResponse<PaymentPositionModel> response = HttpResponse.notFound();
        when(gpdClient.getDebtPosition(anyString(), anyString())).thenReturn(response);
    }

    @Test
    void recover_CREATE_UPLOAD_OK() {
        assertTrue(
                supportService.recover("broker", "organizaition", CREATE_UPLOAD_ID, ServiceType.GPD)
        );
    }

    @Test
    void recover_DELETE_UPLOAD_OK() {
        assertTrue(
                supportService.recover("broker", "organizaition", DELETE_UPLOAD_ID, ServiceType.GPD)
        );
    }

    @Test
    void monitoring_shouldReturnOkWhenNoPendingOperations() {
        when(statusRepository.find(any(SqlQuerySpec.class), any(CosmosQueryRequestOptions.class)))
                .thenReturn(Collections.emptyList());

        ProblemJson result = supportService.monitoring(fromTime, toTime);

        assertEquals(HttpStatus.OK.getCode(), result.getStatus());
        assertEquals("No pending massive operation in the given window", result.getDetail());

        verifyNoInteractions(slackService);
    }

    @Test
    void monitoring_shouldReturnErrorAndNotifyInProd() throws Exception {
        Field envField = supportService.getClass().getDeclaredField("env");
        envField.setAccessible(true);
        envField.set(supportService, "prod");

        List<Status> pendingList = Arrays.asList(
                createPendingStatus("1", 5, 10),
                createPendingStatus("2", 1, 100)
        );

        when(statusRepository.find(any(SqlQuerySpec.class), any(CosmosQueryRequestOptions.class)))
                .thenReturn(pendingList);

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);

        ProblemJson result = supportService.monitoring(fromTime, toTime);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.getCode(), result.getStatus());
        assertTrue(result.getDetail().contains("There are 2 pending massive operations"));

        verify(slackService).uploadCsv(
                eq(":warning: ACA/GPD Caricamento Massivo"),
                eq("Lista di elaborazioni non concluse"),
                fileCaptor.capture()
        );

        tempFileCreated = fileCaptor.getValue();
        assertFalse(tempFileCreated.exists(), "Il file temporaneo dovrebbe essere stato cancellato dopo l'upload.");
    }

    @Test
    void monitoring_shouldReturnErrorAndNoNotificationInDev() throws Exception {
        Field envField = supportService.getClass().getDeclaredField("env");
        envField.setAccessible(true);
        envField.set(supportService, "dev");

        List<Status> pendingList = Collections.singletonList(createPendingStatus("1", 5, 10));

        when(statusRepository.find(any(SqlQuerySpec.class), any(CosmosQueryRequestOptions.class)))
                .thenReturn(pendingList);

        ProblemJson result = supportService.monitoring(fromTime, toTime);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.getCode(), result.getStatus());
        assertTrue(result.getDetail().contains("There are 1 pending massive operations"));

        verifyNoInteractions(slackService);
    }

    @Test
    void monitoring_shouldHandleSlackException() throws Exception {
        Field envField = supportService.getClass().getDeclaredField("env");
        envField.setAccessible(true);
        envField.set(supportService, "prod");
        List<Status> pendingList = Collections.singletonList(createPendingStatus("1", 5, 10));

        when(statusRepository.find(any(SqlQuerySpec.class), any(CosmosQueryRequestOptions.class)))
                .thenReturn(pendingList);

        doThrow(new IOException("Simulated Slack Error")).when(slackService).uploadCsv(any(), any(), any());

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);

        ProblemJson result = supportService.monitoring(fromTime, toTime);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.getCode(), result.getStatus());

        verify(slackService).uploadCsv(any(), any(), fileCaptor.capture());
    }

    @Test
    void monitoring_shouldCallRepositoryWithCorrectQueryAndOptions() throws  Exception {
        Field envField = supportService.getClass().getDeclaredField("env");
        envField.setAccessible(true);
        envField.set(supportService, "dev");
        when(statusRepository.find(any(SqlQuerySpec.class), any(CosmosQueryRequestOptions.class)))
                .thenReturn(Collections.emptyList());

        supportService.monitoring(fromTime, toTime);

        ArgumentCaptor<SqlQuerySpec> queryCaptor = ArgumentCaptor.forClass(SqlQuerySpec.class);
        ArgumentCaptor<CosmosQueryRequestOptions> optionsCaptor = ArgumentCaptor.forClass(CosmosQueryRequestOptions.class);

        verify(statusRepository).find(queryCaptor.capture(), optionsCaptor.capture());

        SqlQuerySpec capturedQuery = queryCaptor.getValue();
        assertEquals("SELECT * FROM c WHERE c._ts >= @fromTs AND c._ts <= @toTs AND c.upload.current != c.upload.total", capturedQuery.getQueryText());

        Long expectedFromTs = fromTime.atZone(zone).toEpochSecond();
        Long expectedToTs = toTime.atZone(zone).toEpochSecond();

        SqlParameter fromParam = (SqlParameter) queryCaptor.getValue().getParameters().get(0);
        SqlParameter toParam = (SqlParameter) queryCaptor.getValue().getParameters().get(1);

        assertEquals(expectedFromTs, fromParam.getValue(Long.class));
        assertEquals(expectedToTs, toParam.getValue(Long.class));

        CosmosQueryRequestOptions capturedOptions = optionsCaptor.getValue();
        assertNull(capturedOptions.getPartitionKey(), "La PartitionKey non dovrebbe essere impostata nelle opzioni passate al find");
    }

    @Test
    void generateCsvContent_shouldCreateCsvFileWithCorrectContent() throws IOException {
        Status s1 = createPendingStatus("A1", 10, 20);
        Status s2 = createPendingStatus("B2", 50, 500);
        List<Status> statusList = Arrays.asList(s1, s2);

        tempFileCreated = supportService.generateCsvContent(statusList);

        assertTrue(tempFileCreated.exists());
        assertTrue(tempFileCreated.getName().endsWith(".csv"));

        String content = Files.readString(tempFileCreated.toPath());

        String date1 = s1.upload.getStart().toLocalDate().toString();
        String date2 = s2.upload.getStart().toLocalDate().toString();

        String expectedContent = "FileId,Broker,Organization,Start,Current/Total\n" +
                "A1,BROKER_A1,FC_A1," + date1 + ",10/20\n" +
                "B2,BROKER_B2,FC_B2," + date2 + ",50/500\n";

        assertEquals(expectedContent, content);
    }

    private Status createPendingStatus(String id, int current, int total) {
        return Status.builder()
                .id(id)
                .brokerID("BROKER_" + id)
                .fiscalCode("FC_" + id)
                .upload(Upload.builder()
                        .current(current)
                        .total(total)
                        .start(fromTime.minusDays(1))
                        .build())
                .build();
    }
}
