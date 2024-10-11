package it.gov.pagopa.gpd.upload.service;

import it.gov.pagopa.gpd.upload.entity.Status;
import it.gov.pagopa.gpd.upload.entity.Upload;
import it.gov.pagopa.gpd.upload.model.UploadReport;
import it.gov.pagopa.gpd.upload.model.UploadStatus;
import it.gov.pagopa.gpd.upload.repository.StatusRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;


@Singleton
@Slf4j
public class StatusService {
    private StatusRepository statusRepository;

    @Inject
    public StatusService(StatusRepository statusRepository) {
        this.statusRepository = statusRepository;
    }

    public UploadStatus getStatus(String fileId, String organizationFiscalCode) {
        Status status = statusRepository.findStatusById(fileId, organizationFiscalCode);
        log.debug("[getStatus] status: " + status.getId());
        return map(status);
    }

    public UploadReport getReport(String orgFiscalCode, String fileId) {
        return mapReport(statusRepository.findStatusById(fileId, orgFiscalCode));
    }

    public Status createUploadStatus(String organizationFiscalCode, String brokerId, String fileId, int totalItem) {
        Upload upload = Upload.builder()
                .current(0)
                .total(totalItem)
                .start(LocalDateTime.now())
                .build();
        Status status = Status.builder()
                .id(fileId)
                .brokerID(brokerId)
                .fiscalCode(organizationFiscalCode)
                .upload(upload)
                .build();

        return statusRepository.saveStatus(status);
    }

    private UploadStatus map(Status status) {
        return UploadStatus.builder()
                .uploadID(status.getId())
                .processedItem(status.upload.getCurrent())
                .submittedItem(status.upload.getTotal())
                .startTime(status.upload.getStart())
                .build();
    }

    private UploadReport mapReport(Status status) {
        return UploadReport.builder()
                .uploadID(status.getId())
                .processedItem(status.upload.getCurrent())
                .submittedItem(status.upload.getTotal())
                .responses(status.upload.getResponses())
                .startTime(status.upload.getStart())
                .endTime(status.upload.getEnd())
                .build();
    }
}
