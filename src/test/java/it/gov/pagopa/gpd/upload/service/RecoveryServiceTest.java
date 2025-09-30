package it.gov.pagopa.gpd.upload.service;

import io.micronaut.http.HttpResponse;
import it.gov.pagopa.gpd.upload.client.GPDClient;
import it.gov.pagopa.gpd.upload.entity.Status;
import it.gov.pagopa.gpd.upload.entity.Upload;
import it.gov.pagopa.gpd.upload.model.UploadInput;
import it.gov.pagopa.gpd.upload.model.UploadOperation;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import it.gov.pagopa.gpd.upload.model.pd.PaymentPositionModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;

class RecoveryServiceTest {

    static final String CREATE_UPLOAD_ID = "upload-id-create";
    static final String DELETE_UPLOAD_ID = "upload-id-delete";

    StatusService statusService = mock(StatusService.class);
    BlobService blobService = mock(BlobService.class);
    GPDClient gpdClient = mock(GPDClient.class);
    RecoveryService recoveryService = new RecoveryService(statusService, blobService, gpdClient);

    @BeforeEach
    public void beforeEach() {
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
        Mockito.when(blobService.getUploadInput(anyString(), anyString(), eq(CREATE_UPLOAD_ID), any(ServiceType.class))).thenReturn(uploadInputCreate);
        Mockito.when(blobService.getUploadInput(anyString(), anyString(), eq(DELETE_UPLOAD_ID), any(ServiceType.class))).thenReturn(uploadInputDelete);

        // Mock StatusService
        Mockito.when(statusService.getStatus(anyString(), eq(DELETE_UPLOAD_ID))).thenReturn(
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
        Mockito.when(statusService.getStatus(anyString(), eq(CREATE_UPLOAD_ID))).thenReturn(
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
        Mockito.when(statusService.upsert(any())).thenReturn(
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
        Mockito.when(gpdClient.getDebtPosition(anyString(), anyString())).thenReturn(response);
    }

    @Test
    void recover_CREATE_UPLOAD_OK() {
        Assertions.assertTrue(
                recoveryService.recover("broker", "organizaition", CREATE_UPLOAD_ID, ServiceType.GPD)
        );
    }

    @Test
    void recover_DELETE_UPLOAD_OK() {
        Assertions.assertTrue(
                recoveryService.recover("broker", "organizaition", DELETE_UPLOAD_ID, ServiceType.GPD)
        );
    }
}
