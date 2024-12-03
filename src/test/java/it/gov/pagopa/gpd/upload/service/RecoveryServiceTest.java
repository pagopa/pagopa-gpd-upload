package it.gov.pagopa.gpd.upload.service;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Primary;
import io.micronaut.http.HttpResponse;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import it.gov.pagopa.gpd.upload.client.GPDClient;
import it.gov.pagopa.gpd.upload.entity.Status;
import it.gov.pagopa.gpd.upload.entity.Upload;
import it.gov.pagopa.gpd.upload.model.UploadInput;
import it.gov.pagopa.gpd.upload.model.UploadOperation;
import it.gov.pagopa.gpd.upload.model.pd.PaymentPositionModel;
import it.gov.pagopa.gpd.upload.repository.BlobStorageRepository;
import it.gov.pagopa.gpd.upload.repository.StatusRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;

@MicronautTest
public class RecoveryServiceTest {

    static final String CREATE_UPLOAD_ID = "upload-id-create";
    static final String DELETE_UPLOAD_ID = "upload-id-delete";

    @Inject
    RecoveryService recoveryService;

    @Test
    void recover_CREATE_UPLOAD_OK() {
        Assertions.assertTrue(
                recoveryService.recover("broker", "organizaition", CREATE_UPLOAD_ID)
        );
    }

    @Test
    void recover_DELETE_UPLOAD_OK() {
        Assertions.assertTrue(
                recoveryService.recover("broker", "organizaition", DELETE_UPLOAD_ID)
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////MOCK//////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////

    // real repositories are out of scope for this test, @PostConstruct init routine requires connection-string
    @Bean
    @Primary
    public static BlobStorageRepository blobStorageRepository() {
        return Mockito.mock(BlobStorageRepository.class);
    }
    @Bean
    @Primary
    public static StatusRepository statusRepository() {
        return Mockito.mock(StatusRepository.class);
    }
    @Bean
    @Primary
    public static BlobService blobService() {
        BlobService blobService = Mockito.mock(BlobService.class);
        UploadInput uploadInputDelete = UploadInput.builder()
                .uploadOperation(UploadOperation.DELETE)
                .paymentPositionIUPDs(List.of("IUPD-1"))
                .build();
        UploadInput uploadInputCreate = UploadInput.builder()
                .uploadOperation(UploadOperation.CREATE)
                .paymentPositions(List.of(PaymentPositionModel.builder()
                        .iupd("IUPD-1").build()))
                .build();
        Mockito.when(blobService.getUploadInput(anyString(), anyString(), eq(CREATE_UPLOAD_ID))).thenReturn(uploadInputCreate);
        Mockito.when(blobService.getUploadInput(anyString(), anyString(), eq(DELETE_UPLOAD_ID))).thenReturn(uploadInputDelete);
        return blobService;
    }
    @Bean
    @Primary
    public static StatusService statusService() {
        StatusService statusService = Mockito.mock(StatusService.class);
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
        return statusService;
    }
    @Bean
    @Primary
    public static GPDClient gpdClient() {
        GPDClient gpdClient = Mockito.mock(GPDClient.class);
        HttpResponse<PaymentPositionModel> response = HttpResponse.notFound();
        Mockito.when(gpdClient.getDebtPosition(anyString(), anyString())).thenReturn(response);
        return gpdClient;
    }
}
