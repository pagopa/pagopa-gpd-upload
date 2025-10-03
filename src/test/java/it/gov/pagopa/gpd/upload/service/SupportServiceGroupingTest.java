package it.gov.pagopa.gpd.upload.service;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import it.gov.pagopa.gpd.upload.client.GPDClient;
import it.gov.pagopa.gpd.upload.entity.ResponseEntry;
import it.gov.pagopa.gpd.upload.entity.Status;
import it.gov.pagopa.gpd.upload.entity.Upload;
import it.gov.pagopa.gpd.upload.model.ProblemJson;
import it.gov.pagopa.gpd.upload.model.UploadInput;
import it.gov.pagopa.gpd.upload.model.UploadOperation;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import it.gov.pagopa.gpd.upload.model.pd.PaymentPositionModel;
import it.gov.pagopa.gpd.upload.repository.StatusRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;

class SupportServiceGroupingTest {

    private static final String CREATE_UPLOAD_ID = "upload-id-create-grouping";

    @Test
    void recover_CREATE_groupsByStatusAndMessage() {
        StatusRepository statusRepository = Mockito.mock(StatusRepository.class);
        StatusService statusService = Mockito.mock(StatusService.class);
        BlobService blobService = Mockito.mock(BlobService.class);
        GPDClient gpdClient = Mockito.mock(GPDClient.class);
        SlackService slackService = Mockito.mock(SlackService.class);

        SupportService supportService = new SupportService(statusRepository, statusService, blobService, gpdClient, slackService);

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

        // BlobService.getUploadInput(...)
        Mockito.when(blobService.getUploadInput(eq("broker"), eq("organization"), eq(CREATE_UPLOAD_ID), eq(ServiceType.GPD)))
                .thenReturn(uploadInputCreate);

        // StatusService.getStatus(orgFiscalCode, fileId)
        Mockito.when(statusService.getStatus(eq("organization"), eq(CREATE_UPLOAD_ID))).thenReturn(
                Status.builder()
                        .id(CREATE_UPLOAD_ID)
                        .brokerID("broker")
                        .fiscalCode("organization")
                        .upload(Upload.builder()
                                .current(0)
                                .total(6)
                                .responses(new ArrayList<>())
                                .start(LocalDateTime.now().minusMinutes(1))
                                .end(null) // evita uscita anticipata
                                .build())
                        .build()
        );

        // upsert(...) returns the same Status passed in (so recover(...) can return true)
        Mockito.when(statusService.upsert(any())).thenAnswer(inv -> inv.getArgument(0));

        // Standard messages for status codes --> StatusService.getDetail(...)
        Mockito.when(statusService.getDetail(HttpStatus.CREATED)).thenReturn("Debt position CREATED");
        Mockito.when(statusService.getDetail(HttpStatus.OK)).thenReturn("Debt position operation OK");
        Mockito.when(statusService.getDetail(HttpStatus.NOT_FOUND)).thenReturn("Debt position NOT FOUND");
        Mockito.when(statusService.getDetail(HttpStatus.CONFLICT))
                .thenReturn("Debt position IUPD or NAV/IUV already exists for organization code");

        Mockito.when(gpdClient.getDebtPosition(eq("organization"), anyString()))
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

        boolean result = supportService.recover("broker", "organization", CREATE_UPLOAD_ID, ServiceType.GPD);

        // --- assert ---
        Assertions.assertTrue(result, "Recovery should return true when upsert succeeds");

        ArgumentCaptor<Status> captor = ArgumentCaptor.forClass(Status.class);
        Mockito.verify(statusService, Mockito.atLeastOnce()).upsert(captor.capture());
        Status saved = captor.getValue();

        Assertions.assertNotNull(saved);
        Assertions.assertNotNull(saved.getUpload());
        List<ResponseEntry> responses = saved.getUpload().getResponses();
        Assertions.assertNotNull(responses);

        Assertions.assertEquals(5, responses.size(), "Should produce exactly 5 grouped response entries");

        // 201 CREATED -> standard message
        ResponseEntry created = responses.stream()
                .filter(r -> r.getStatusCode() == HttpStatus.CREATED.getCode())
                .findFirst().orElseThrow();
        Assertions.assertEquals("Debt position CREATED", created.getStatusMessage());
        Assertions.assertEquals(1, created.getRequestIDs().size());
        Assertions.assertTrue(created.getRequestIDs().contains("IUPD_OK_A"));

        // 400 - message A (2023-04-24)
        ResponseEntry badReqA = responses.stream()
                .filter(r -> r.getStatusCode() == 400 && r.getStatusMessage().contains("2023-04-24"))
                .findFirst().orElseThrow();
        Assertions.assertTrue(badReqA.getRequestIDs().containsAll(List.of("IUPD_400_A1", "IUPD_400_A2")));
        Assertions.assertEquals(2, badReqA.getRequestIDs().size());

        // 400 - message B (2023-07-31)
        ResponseEntry badReqB = responses.stream()
                .filter(r -> r.getStatusCode() == 400 && r.getStatusMessage().contains("2023-07-31"))
                .findFirst().orElseThrow();
        Assertions.assertTrue(badReqB.getRequestIDs().contains("IUPD_400_B1"));
        Assertions.assertEquals(1, badReqB.getRequestIDs().size());

        // 409 - fallback standard (no body)
        ResponseEntry conflict = responses.stream()
                .filter(r -> r.getStatusCode() == 409)
                .findFirst().orElseThrow();
        Assertions.assertEquals(
                "Debt position IUPD or NAV/IUV already exists for organization code",
                conflict.getStatusMessage()
        );
        Assertions.assertTrue(conflict.getRequestIDs().contains("IUPD_409"));

        // 404 - fallback standard (no body)
        ResponseEntry notFound = responses.stream()
                .filter(r -> r.getStatusCode() == 404)
                .findFirst().orElseThrow();
        Assertions.assertEquals("Debt position NOT FOUND", notFound.getStatusMessage());
        Assertions.assertTrue(notFound.getRequestIDs().contains("IUPD_404"));
    }
}