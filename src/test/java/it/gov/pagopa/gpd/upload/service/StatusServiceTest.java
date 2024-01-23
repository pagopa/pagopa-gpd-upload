package it.gov.pagopa.gpd.upload.service;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Primary;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import it.gov.pagopa.gpd.upload.entity.Status;
import it.gov.pagopa.gpd.upload.entity.Upload;
import it.gov.pagopa.gpd.upload.model.UploadReport;
import it.gov.pagopa.gpd.upload.model.UploadStatus;
import it.gov.pagopa.gpd.upload.repository.BlobStorageRepository;
import it.gov.pagopa.gpd.upload.repository.StatusRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;

@MicronautTest
public class StatusServiceTest {
    private static String UPLOAD_KEY = "key";
    @Inject
    StatusService statusService;

    @Test
    void getStatus_OK() {
        UploadStatus uploadStatus = statusService.getStatus("fileId", "organizationFiscalCode");

        Assertions.assertEquals(UPLOAD_KEY, uploadStatus.getUploadID());
    }

    @Test
    void getReport_OK() {
        UploadReport uploadReport = statusService.getReport("fileId", "organizationFiscalCode");

        Assertions.assertEquals(UPLOAD_KEY, uploadReport.getUploadID());
    }

    // real repositories are out of scope for this test, @PostConstruct init routine requires connection-string
    @Bean
    @Primary
    public static BlobStorageRepository blobStorageRepository() {
        return Mockito.mock(BlobStorageRepository.class);
    }
    @Bean
    @Primary
    public static StatusRepository statusRepository() {
        StatusRepository statusRepository = Mockito.mock(StatusRepository.class);
        Status status = Status.builder()
                .id(UPLOAD_KEY)
                .upload(Upload.builder()
                        .current(0)
                        .total(0)
                        .start(LocalDateTime.now())
                        .build())
                .build();
        Mockito.when(statusRepository.findStatusById(anyString(), anyString())).thenReturn(status);
        return statusRepository;
    }
}
