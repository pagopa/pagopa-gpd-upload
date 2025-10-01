package it.gov.pagopa.gpd.upload.service;

import io.micronaut.http.HttpStatus;
import it.gov.pagopa.gpd.upload.entity.ResponseEntry;
import it.gov.pagopa.gpd.upload.entity.Status;
import it.gov.pagopa.gpd.upload.entity.Upload;
import it.gov.pagopa.gpd.upload.exception.AppException;
import it.gov.pagopa.gpd.upload.model.FileIdListResponse;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import it.gov.pagopa.gpd.upload.model.v1.UploadReport;
import it.gov.pagopa.gpd.upload.model.v1.UploadStatus;
import it.gov.pagopa.gpd.upload.model.v2.UploadReportDTO;
import it.gov.pagopa.gpd.upload.model.v2.UploadStatusDTO;
import it.gov.pagopa.gpd.upload.model.v2.enumeration.OperationStatus;
import it.gov.pagopa.gpd.upload.repository.StatusRepository;
import it.gov.pagopa.gpd.upload.utils.ResponseEntryDTOMapperImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;

class StatusServiceTest {
    private static final String IUPD_1 = "IUPD1";
    private static final String IUPD_2 = "IUPD2";
    private static final String UPLOAD_KEY = "key";

    StatusRepository statusRepository = mock(StatusRepository.class);
    ResponseEntryDTOMapperImpl responseEntryDTOMapper = new ResponseEntryDTOMapperImpl();
    StatusService statusService = new StatusService(statusRepository, responseEntryDTOMapper);

    @Test
    void getUploadStatusV1_OK() {
        Status status = Status.builder()
                .id(UPLOAD_KEY)
                .serviceType(ServiceType.GPD)
                .upload(Upload.builder()
                        .current(0)
                        .total(0)
                        .start(LocalDateTime.now())
                        .build())
                .build();
        Mockito.when(statusRepository.find(any(), any())).thenReturn(List.of(status));
        UploadStatus uploadStatus = statusService.getUploadStatus("brokerCode", "fileId", "organizationFiscalCode", ServiceType.GPD);

        Assertions.assertEquals(UPLOAD_KEY, uploadStatus.getUploadID());
    }

    @Test
    void getUploadStatusV1_EmptyServiceTypeAndGPD_OK() {
        Status status = Status.builder()
                .id(UPLOAD_KEY)
                .upload(Upload.builder()
                        .current(0)
                        .total(0)
                        .start(LocalDateTime.now())
                        .build())
                .build();
        Mockito.when(statusRepository.find(any(), any())).thenReturn(List.of(status));
        UploadStatus uploadStatus = statusService.getUploadStatus("brokerCode", "fileId", "organizationFiscalCode", ServiceType.GPD);

        Assertions.assertEquals(UPLOAD_KEY, uploadStatus.getUploadID());
    }

    @Test
    void getUploadStatusV1_EmptyServiceTypeAndACA_KO() {
        Status status = Status.builder()
                .id(UPLOAD_KEY)
                .upload(Upload.builder()
                        .current(0)
                        .total(0)
                        .start(LocalDateTime.now())
                        .build())
                .build();
        Mockito.when(statusRepository.find(any(), any())).thenReturn(List.of(status));
        Assertions.assertThrows(AppException.class, () -> statusService.getUploadStatus("brokerCode", "fileId", "organizationFiscalCode", ServiceType.ACA));
    }

    @Test
    void getUploadStatusV1_DifferentServiceType_KO() {
        Status status = Status.builder()
                .id(UPLOAD_KEY)
                .serviceType(ServiceType.ACA)
                .upload(Upload.builder()
                        .current(0)
                        .total(0)
                        .start(LocalDateTime.now())
                        .build())
                .build();
        Mockito.when(statusRepository.find(any(), any())).thenReturn(List.of(status));
        Assertions.assertThrows(AppException.class, () -> statusService.getUploadStatus("brokerCode", "fileId", "organizationFiscalCode", ServiceType.GPD));
    }

    @Test
    void getUploadStatusV2_COMPLETED_OK() {
        Status status = Status.builder()
                .id(UPLOAD_KEY)
                .serviceType(ServiceType.GPD)
                .upload(Upload.builder()
                        .current(10)
                        .total(10)
                        .start(LocalDateTime.now())
                        .build())
                .build();
        Mockito.when(statusRepository.find(any(), any())).thenReturn(List.of(status));
        UploadStatusDTO uploadStatus = statusService.getUploadStatusV2("brokerCode", "fileId", "organizationFiscalCode", ServiceType.GPD);

        Assertions.assertEquals(UPLOAD_KEY, uploadStatus.getFileId());
        Assertions.assertEquals(OperationStatus.COMPLETED, uploadStatus.getOperationStatus());
    }

    @Test
    void getUploadStatusV2_IN_PROGRESS_OK() {
        Status status = Status.builder()
                .id(UPLOAD_KEY)
                .serviceType(ServiceType.GPD)
                .upload(Upload.builder()
                        .current(5)
                        .total(10)
                        .start(LocalDateTime.now())
                        .build())
                .build();
        Mockito.when(statusRepository.find(any(), any())).thenReturn(List.of(status));
        UploadStatusDTO uploadStatus = statusService.getUploadStatusV2("brokerCode", "fileId", "organizationFiscalCode", ServiceType.GPD);

        Assertions.assertEquals(UPLOAD_KEY, uploadStatus.getFileId());
        Assertions.assertEquals(OperationStatus.IN_PROGRESS, uploadStatus.getOperationStatus());
    }

    @Test
    void getUploadStatusV2_COMPLETED_WITH_WARNINGS_OK() {
        Status status = Status.builder()
                .id(UPLOAD_KEY)
                .serviceType(ServiceType.GPD)
                .upload(Upload.builder()
                        .current(10)
                        .total(10)
                        .start(LocalDateTime.now())
                        .responses(new ArrayList<>(List.of(
                                ResponseEntry.builder().statusCode(HttpStatus.OK.getCode()).build(),
                                ResponseEntry.builder().statusCode(HttpStatus.INTERNAL_SERVER_ERROR.getCode()).build()
                        )))
                        .build())
                .build();
        Mockito.when(statusRepository.find(any(), any())).thenReturn(List.of(status));
        UploadStatusDTO uploadStatus = statusService.getUploadStatusV2("brokerCode", "fileId", "organizationFiscalCode", ServiceType.GPD);

        Assertions.assertEquals(UPLOAD_KEY, uploadStatus.getFileId());
        Assertions.assertEquals(OperationStatus.COMPLETED_WITH_WARNINGS, uploadStatus.getOperationStatus());
    }

    @Test
    void getUploadStatusV2_COMPLETED_UNSUCCESSFULLY_OK() {
        Status status = Status.builder()
                .id(UPLOAD_KEY)
                .serviceType(ServiceType.GPD)
                .upload(Upload.builder()
                        .current(10)
                        .total(10)
                        .start(LocalDateTime.now())
                        .responses(new ArrayList<>(List.of(
                                ResponseEntry.builder().statusCode(HttpStatus.BAD_REQUEST.getCode()).build(),
                                ResponseEntry.builder().statusCode(HttpStatus.INTERNAL_SERVER_ERROR.getCode()).build()
                        )))
                        .build())
                .build();
        Mockito.when(statusRepository.find(any(), any())).thenReturn(List.of(status));
        UploadStatusDTO uploadStatus = statusService.getUploadStatusV2("brokerCode", "fileId", "organizationFiscalCode", ServiceType.GPD);

        Assertions.assertEquals(UPLOAD_KEY, uploadStatus.getFileId());
        Assertions.assertEquals(OperationStatus.COMPLETED_UNSUCCESSFULLY, uploadStatus.getOperationStatus());
    }

    @Test
    void getUploadStatusV2_EmptyServiceTypeAndGPD_OK() {
        Status status = Status.builder()
                .id(UPLOAD_KEY)
                .upload(Upload.builder()
                        .current(0)
                        .total(0)
                        .start(LocalDateTime.now())
                        .build())
                .build();
        Mockito.when(statusRepository.find(any(), any())).thenReturn(List.of(status));
        UploadStatusDTO uploadStatus = statusService.getUploadStatusV2("brokerCode", "fileId", "organizationFiscalCode", ServiceType.GPD);

        Assertions.assertEquals(UPLOAD_KEY, uploadStatus.getFileId());
    }

    @Test
    void getUploadStatusV2_EmptyServiceTypeAndACA_KO() {
        Status status = Status.builder()
                .id(UPLOAD_KEY)
                .upload(Upload.builder()
                        .current(0)
                        .total(0)
                        .start(LocalDateTime.now())
                        .build())
                .build();
        Mockito.when(statusRepository.find(any(), any())).thenReturn(List.of(status));
        Assertions.assertThrows(AppException.class, () -> statusService.getUploadStatusV2("brokerCode", "fileId", "organizationFiscalCode", ServiceType.ACA));
    }

    @Test
    void getUploadStatusV2_DifferentServiceType_KO() {
        Status status = Status.builder()
                .id(UPLOAD_KEY)
                .serviceType(ServiceType.ACA)
                .upload(Upload.builder()
                        .current(0)
                        .total(0)
                        .start(LocalDateTime.now())
                        .build())
                .build();
        Mockito.when(statusRepository.find(any(), any())).thenReturn(List.of(status));
        Assertions.assertThrows(AppException.class, () -> statusService.getUploadStatusV2("brokerCode", "fileId", "organizationFiscalCode", ServiceType.GPD));
    }

    @Test
    void getReportV1_OK() {
        Status status = Status.builder()
                .id(UPLOAD_KEY)
                .serviceType(ServiceType.GPD)
                .upload(Upload.builder()
                        .current(0)
                        .total(0)
                        .start(LocalDateTime.now())
                        .build())
                .build();
        Mockito.when(statusRepository.find(any(), any())).thenReturn(List.of(status));
        UploadReport uploadReport = statusService.getReportV1("brokerCode", "organizationFiscalCode", "fileId", ServiceType.GPD);

        Assertions.assertEquals(UPLOAD_KEY, uploadReport.getUploadID());
    }

    @Test
    void getReportV1_EmptyServiceTypeAndGPD_OK() {
        Status status = Status.builder()
                .id(UPLOAD_KEY)
                .upload(Upload.builder()
                        .current(0)
                        .total(0)
                        .start(LocalDateTime.now())
                        .build())
                .build();
        Mockito.when(statusRepository.find(any(), any())).thenReturn(List.of(status));
        UploadReport uploadReport = statusService.getReportV1("brokerCode", "organizationFiscalCode", "fileId", ServiceType.GPD);

        Assertions.assertEquals(UPLOAD_KEY, uploadReport.getUploadID());
    }

    @Test
    void getReportV1_EmptyServiceTypeAndACA_KO() {
        Status status = Status.builder()
                .id(UPLOAD_KEY)
                .upload(Upload.builder()
                        .current(0)
                        .total(0)
                        .start(LocalDateTime.now())
                        .build())
                .build();
        Mockito.when(statusRepository.find(any(), any())).thenReturn(List.of(status));
        Assertions.assertThrows(AppException.class, () -> statusService.getReportV1("brokerCode", "organizationFiscalCode", "fileId", ServiceType.ACA));
    }

    @Test
    void getReportV1_DifferentServiceType_KO() {
        Status status = Status.builder()
                .id(UPLOAD_KEY)
                .serviceType(ServiceType.ACA)
                .upload(Upload.builder()
                        .current(0)
                        .total(0)
                        .start(LocalDateTime.now())
                        .build())
                .build();
        Mockito.when(statusRepository.find(any(), any())).thenReturn(List.of(status));
        Assertions.assertThrows(AppException.class, () -> statusService.getReportV1("brokerCode", "organizationFiscalCode", "fileId", ServiceType.GPD));
    }

    @Test
    void getReportV2_OK() {
        Status status = Status.builder()
                .id(UPLOAD_KEY)
                .serviceType(ServiceType.GPD)
                .upload(Upload.builder()
                        .current(10)
                        .total(10)
                        .start(LocalDateTime.now())
                        .responses(new ArrayList<>(List.of(
                                ResponseEntry.builder().requestIDs(Collections.singletonList(IUPD_1)).statusCode(HttpStatus.OK.getCode()).build(),
                                ResponseEntry.builder().requestIDs(Collections.singletonList(IUPD_2)).statusCode(HttpStatus.INTERNAL_SERVER_ERROR.getCode()).build()
                        )))
                        .build())
                .build();
        Mockito.when(statusRepository.find(any(), any())).thenReturn(List.of(status));
        UploadReportDTO uploadReport = statusService.getReportV2("brokerCode", "organizationFiscalCode", "fileId", ServiceType.GPD);

        Assertions.assertEquals(UPLOAD_KEY, uploadReport.getFileId());
        Assertions.assertTrue(uploadReport.getResponses().get(0).getIupds().contains(IUPD_1));
        Assertions.assertTrue(uploadReport.getResponses().get(1).getIupds().contains(IUPD_2));
    }

    @Test
    void getReportV2_EmptyServiceTypeAndGPD_OK() {
        Status status = Status.builder()
                .id(UPLOAD_KEY)
                .upload(Upload.builder()
                        .current(0)
                        .total(0)
                        .start(LocalDateTime.now())
                        .build())
                .build();
        Mockito.when(statusRepository.find(any(), any())).thenReturn(List.of(status));
        UploadReportDTO uploadReport = statusService.getReportV2("brokerCode", "organizationFiscalCode", "fileId", ServiceType.GPD);

        Assertions.assertEquals(UPLOAD_KEY, uploadReport.getFileId());
    }

    @Test
    void getReportV2_EmptyServiceTypeAndACA_KO() {
        Status status = Status.builder()
                .id(UPLOAD_KEY)
                .upload(Upload.builder()
                        .current(0)
                        .total(0)
                        .start(LocalDateTime.now())
                        .build())
                .build();
        Mockito.when(statusRepository.find(any(), any())).thenReturn(List.of(status));
        Assertions.assertThrows(AppException.class, () -> statusService.getReportV2("brokerCode", "organizationFiscalCode", "fileId", ServiceType.ACA));
    }

    @Test
    void getReportV2_DifferentServiceType_KO() {
        Status status = Status.builder()
                .id(UPLOAD_KEY)
                .serviceType(ServiceType.ACA)
                .upload(Upload.builder()
                        .current(0)
                        .total(0)
                        .start(LocalDateTime.now())
                        .build())
                .build();
        Mockito.when(statusRepository.find(any(), any())).thenReturn(List.of(status));
        Assertions.assertThrows(AppException.class, () -> statusService.getReportV2("brokerCode", "organizationFiscalCode", "fileId", ServiceType.GPD));
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
        LocalDate to = LocalDate.parse("2025-09-06");


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
        ArgumentCaptor<LocalDateTime> toCap = ArgumentCaptor.forClass(LocalDateTime.class);
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
}
