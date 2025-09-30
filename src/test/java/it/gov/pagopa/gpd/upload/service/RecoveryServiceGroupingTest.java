package it.gov.pagopa.gpd.upload.service;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Primary;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import it.gov.pagopa.gpd.upload.client.GPDClient;
import it.gov.pagopa.gpd.upload.entity.ResponseEntry;
import it.gov.pagopa.gpd.upload.entity.Status;
import it.gov.pagopa.gpd.upload.entity.Upload;
import it.gov.pagopa.gpd.upload.model.ProblemJson;
import it.gov.pagopa.gpd.upload.model.UploadInput;
import it.gov.pagopa.gpd.upload.model.UploadOperation;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import it.gov.pagopa.gpd.upload.model.pd.PaymentPositionModel;
import it.gov.pagopa.gpd.upload.repository.BlobStorageRepository;
import it.gov.pagopa.gpd.upload.repository.StatusRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;

@MicronautTest
class RecoveryServiceGroupingTest {

    private static final String CREATE_UPLOAD_ID = "upload-id-create-grouping";

    @Inject
    RecoveryService recoveryService;

    @Inject
    StatusService statusService;

    @Test
    void recover_CREATE_groupsByStatusAndMessage() {
        // Act
        boolean result = recoveryService.recover("broker", "organization", CREATE_UPLOAD_ID, ServiceType.GPD);
        Assertions.assertTrue(result, "Recovery should return true when upsert succeeds");
        
        // Sanity-check: ensure the mock in this test is the one used by Micronaut
        Mockito.verify(statusService, Mockito.atLeastOnce()).getStatus(anyString(), eq(CREATE_UPLOAD_ID));

        // Capture the Status passed to upsert(...)
        ArgumentCaptor<Status> captor = ArgumentCaptor.forClass(Status.class);
        Mockito.verify(statusService, Mockito.atLeastOnce()).upsert(captor.capture());
        Status saved = captor.getValue();

        // Basic checks
        Assertions.assertNotNull(saved);
        Assertions.assertNotNull(saved.getUpload());
        List<ResponseEntry> responses = saved.getUpload().getResponses();
        Assertions.assertNotNull(responses);

        // We expect 5 groups:
        // 1) 201 CREATED  -> 1 ID (IUPD_OK_A)
        // 2) 400 BAD_REQUEST (message A) -> 2 IDs (IUPD_400_A1, IUPD_400_A2)
        // 3) 400 BAD_REQUEST (message B) -> 1 ID (IUPD_400_B1)
        // 4) 409 CONFLICT (no body -> standard message) -> 1 ID (IUPD_409)
        // 5) 404 NOT_FOUND (no body -> standard message) -> 1 ID (IUPD_404)
        Assertions.assertEquals(5, responses.size(), "Should produce exactly 5 grouped response entries");

        // 201 CREATED must use the standard message
        ResponseEntry created = responses.stream()
                .filter(r -> r.getStatusCode() == HttpStatus.CREATED.getCode())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing 201 CREATED group"));
        Assertions.assertEquals("Debt position CREATED", created.getStatusMessage());
        Assertions.assertEquals(1, created.getRequestIDs().size());
        Assertions.assertTrue(created.getRequestIDs().contains("IUPD_OK_A"));

        // 400 - message A (2023-04-24)
        ResponseEntry badReqA = responses.stream()
                .filter(r -> r.getStatusCode() == 400 && r.getStatusMessage().contains("2023-04-24"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing 400 group with message A"));
        Assertions.assertTrue(badReqA.getRequestIDs().containsAll(List.of("IUPD_400_A1", "IUPD_400_A2")));
        Assertions.assertEquals(2, badReqA.getRequestIDs().size());

        // 400 - message B (2023-07-31)
        ResponseEntry badReqB = responses.stream()
                .filter(r -> r.getStatusCode() == 400 && r.getStatusMessage().contains("2023-07-31"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing 400 group with message B"));
        Assertions.assertTrue(badReqB.getRequestIDs().contains("IUPD_400_B1"));
        Assertions.assertEquals(1, badReqB.getRequestIDs().size());

        // 409 - standard message fallback (no body)
        ResponseEntry conflict = responses.stream()
                .filter(r -> r.getStatusCode() == 409)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing 409 group"));
        Assertions.assertEquals("Already exists a debt position for the Organization Fiscal Code",
                conflict.getStatusMessage());
        Assertions.assertTrue(conflict.getRequestIDs().contains("IUPD_409"));

        // 404 - standard message fallback (no body)
        ResponseEntry notFound = responses.stream()
                .filter(r -> r.getStatusCode() == 404)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing 404 group"));
        Assertions.assertEquals("Debt position NOT FOUND", notFound.getStatusMessage());
        Assertions.assertTrue(notFound.getRequestIDs().contains("IUPD_404"));
    }

    // -------------------------------------------------------------------------
    // Test-local mocks
    // -------------------------------------------------------------------------

    @Bean
    @Primary
    static BlobStorageRepository blobStorageRepository() {
        return Mockito.mock(BlobStorageRepository.class);
    }

    @Bean
    @Primary
    static StatusRepository statusRepository() {
        return Mockito.mock(StatusRepository.class);
    }
    
    @MockBean(BlobService.class)
    BlobService blobService() {
        BlobService blobService = Mockito.mock(BlobService.class);

        // CREATE input with 6 IUPDs to exercise grouping
        UploadInput uploadInputCreate = UploadInput.builder()
                .uploadOperation(UploadOperation.CREATE)
                .paymentPositions(List.of(
                        PaymentPositionModel.builder().iupd("IUPD_OK_A").build(),
                        PaymentPositionModel.builder().iupd("IUPD_400_A1").build(),
                        PaymentPositionModel.builder().iupd("IUPD_400_A2").build(),
                        PaymentPositionModel.builder().iupd("IUPD_400_B1").build(),
                        PaymentPositionModel.builder().iupd("IUPD_409").build(),
                        PaymentPositionModel.builder().iupd("IUPD_404").build()
                ))
                .build();

        Mockito.when(blobService.getUploadInput(anyString(), anyString(), eq(CREATE_UPLOAD_ID), any(ServiceType.class)))
                .thenReturn(uploadInputCreate);
        return blobService;
    }

    @MockBean(StatusService.class)
    StatusService statusService() {
        StatusService statusServiceMock = Mockito.mock(StatusService.class);

        // Initial status for the CREATE flow: current=0, total=6, responses=[], end=null
        Mockito.when(statusServiceMock.getStatus(anyString(), eq(CREATE_UPLOAD_ID))).thenReturn(
                Status.builder()
                        .id(CREATE_UPLOAD_ID)
                        .brokerID("broker")
                        .fiscalCode("organization")
                        .upload(Upload.builder()
                                .current(0)
                                .total(6)                
                                .responses(new ArrayList<>())
                                .start(LocalDateTime.now().minusMinutes(1))
                                .end(null)                // <-- end=null to avoid exiting immediately
                                .build())
                        .build()
        );

        // upsert(...) returns the same Status passed in (so recover(...) can return true)
        Mockito.when(statusServiceMock.upsert(any())).thenAnswer(inv -> inv.getArgument(0));
        
        
        Mockito.when(statusServiceMock.getDetail(HttpStatus.CREATED))
               .thenReturn("Debt position CREATED");
        Mockito.when(statusServiceMock.getDetail(HttpStatus.OK))
               .thenReturn("Debt position operation OK");
        Mockito.when(statusServiceMock.getDetail(HttpStatus.NOT_FOUND))
               .thenReturn("Debt position NOT FOUND");
        Mockito.when(statusServiceMock.getDetail(HttpStatus.CONFLICT))
               .thenReturn("Already exists a debt position for the Organization Fiscal Code");
        
        return statusServiceMock;
    }
    
    @MockBean(GPDClient.class)
    GPDClient gpdClient() {
        GPDClient gpdClient = Mockito.mock(GPDClient.class);

        // Return different HTTP responses per IUPD to exercise grouping and message rules
        Mockito.when(gpdClient.getDebtPosition(anyString(), anyString()))
                .thenAnswer(inv -> {
                    String iupd = inv.getArgument(1, String.class);

                    if ("IUPD_OK_A".equals(iupd)) {
                        return HttpResponse.status(HttpStatus.CREATED);
                    }

                    if ("IUPD_400_A1".equals(iupd) || "IUPD_400_A2".equals(iupd)) {
                    	ProblemJson pj = ProblemJson.builder()
                    		    .title("Bad Request")
                    		    .status(400)
                    		    .detail("Dates congruence error: validity_date must be >= current_date [validity_date=2023-04-24 12:00:00; current_date=…]")
                    		    .build();
                        return HttpResponse.badRequest(pj).contentType(MediaType.APPLICATION_JSON_TYPE);
                    }

                    if ("IUPD_400_B1".equals(iupd)) {
                    	ProblemJson pj = ProblemJson.builder()
                    		    .title("Bad Request")
                    		    .status(400)
                    		    .detail("Dates congruence error: validity_date must be >= current_date [validity_date=2023-07-31 12:00:00; current_date=…]")
                    		    .build();
                        return HttpResponse.badRequest(pj).contentType(MediaType.APPLICATION_JSON_TYPE);
                    }

                    if ("IUPD_409".equals(iupd)) {
                        return HttpResponse.status(HttpStatus.CONFLICT);
                    }
                    if ("IUPD_404".equals(iupd)) {
                        return HttpResponse.notFound();
                    }

                    return HttpResponse.ok();
                });

        return gpdClient;
    }
}