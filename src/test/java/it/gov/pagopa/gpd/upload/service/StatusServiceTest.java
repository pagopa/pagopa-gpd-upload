package it.gov.pagopa.gpd.upload.service;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Primary;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import it.gov.pagopa.gpd.upload.entity.Status;
import it.gov.pagopa.gpd.upload.entity.Upload;
import it.gov.pagopa.gpd.upload.model.FileIdListResponse;
import it.gov.pagopa.gpd.upload.model.v1.UploadReport;
import it.gov.pagopa.gpd.upload.model.v1.UploadStatus;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import it.gov.pagopa.gpd.upload.repository.BlobStorageRepository;
import it.gov.pagopa.gpd.upload.repository.StatusRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

@MicronautTest
public class StatusServiceTest {
    private static String UPLOAD_KEY = "key";
    @Inject
    StatusService statusService;
    
    @Inject
    StatusRepository statusRepository;

    @Test
    void getUploadStatus_OK() {
        UploadStatus uploadStatus = statusService.getUploadStatus("brokerCode", "fileId", "organizationFiscalCode", ServiceType.GPD);

        Assertions.assertEquals(UPLOAD_KEY, uploadStatus.getUploadID());
    }

    @Test
    void getReport_OK() {
        UploadReport uploadReport = statusService.getReportV1("brokerCode", "organizationFiscalCode", "fileId", ServiceType.GPD);

        Assertions.assertEquals(UPLOAD_KEY, uploadReport.getUploadID());
    }
    
    @Test
    void getFileIdList_hasMore_and_token_propagated() {
        
        List<String> ids = List.of("id1", "id2", "id3");
        String nextToken = "ct-123";
        StatusRepository.FileIdsPage page = new StatusRepository.FileIdsPage(ids, nextToken);
        Mockito.when(statusRepository.findFileIdsPage(
                anyString(), anyString(),
                any(LocalDateTime.class), any(LocalDateTime.class),
                anyInt(), any(), any(ServiceType.class)
        )).thenReturn(page);

        LocalDate from = LocalDate.parse("2025-09-01");
        LocalDate to   = LocalDate.parse("2025-09-06");

        
        FileIdListResponse res = statusService.getFileIdList(
                "brokerA", "orgCF", from, to, 100, null, ServiceType.GPD
        );

        // Assert
        Assertions.assertNotNull(res);
        Assertions.assertEquals(ids, res.getFileIds());
        Assertions.assertEquals(ids.size(), res.getSize());
        Assertions.assertTrue(res.isHasMore());
        Assertions.assertEquals(nextToken, res.getContinuationToken());

        ArgumentCaptor<LocalDateTime> fromCap = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> toCap   = ArgumentCaptor.forClass(LocalDateTime.class);
        Mockito.verify(statusRepository).findFileIdsPage(
                Mockito.eq("brokerA"),
                Mockito.eq("orgCF"),
                fromCap.capture(),
                toCap.capture(),
                Mockito.eq(100),
                Mockito.isNull(),
                Mockito.eq(ServiceType.GPD)
        );
        // Inclusivity of the day: [00:00:00, 23:59:59.999999999]
        Assertions.assertEquals(LocalTime.MIDNIGHT, fromCap.getValue().toLocalTime());
        Assertions.assertEquals(LocalTime.MAX, toCap.getValue().toLocalTime());
    }

    @Test
    void getFileIdList_noMore_no_token() {
       
        List<String> ids = List.of("only-one");
        StatusRepository.FileIdsPage page = new StatusRepository.FileIdsPage(ids, null);
        Mockito.when(statusRepository.findFileIdsPage(
                anyString(), anyString(),
                any(LocalDateTime.class), any(LocalDateTime.class),
                anyInt(), any(), any(ServiceType.class)
        )).thenReturn(page);

       
        FileIdListResponse res = statusService.getFileIdList(
                "brokerB", "orgCF", LocalDate.parse("2025-09-01"), LocalDate.parse("2025-09-06"),
                100, null, ServiceType.ACA
        );

        // Assert
        Assertions.assertNotNull(res);
        Assertions.assertEquals(ids, res.getFileIds());
        Assertions.assertEquals(1, res.getSize());
        Assertions.assertFalse(res.isHasMore());
        Assertions.assertNull(res.getContinuationToken());
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
                .serviceType(ServiceType.GPD)
                .upload(Upload.builder()
                        .current(0)
                        .total(0)
                        .start(LocalDateTime.now())
                        .build())
                .build();
        Mockito.when(statusRepository.findStatusById(anyString(), anyString())).thenReturn(status);
        Mockito.when(statusRepository.find(any(), any())).thenReturn(List.of(status));
        return statusRepository;
    }
    
    
}
