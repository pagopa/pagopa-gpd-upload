package it.gov.pagopa.gpd.upload.service;

import it.gov.pagopa.gpd.upload.entity.Status;
import it.gov.pagopa.gpd.upload.entity.Upload;
import it.gov.pagopa.gpd.upload.exception.AppException;
import it.gov.pagopa.gpd.upload.model.UploadReport;
import it.gov.pagopa.gpd.upload.model.UploadStatus;
import it.gov.pagopa.gpd.upload.repository.StatusRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

import static io.micronaut.http.HttpStatus.NOT_FOUND;

@Singleton
@Slf4j
public class StatusService {
    private StatusRepository statusRepository;
    private BlobService blobService;

    @Inject
    public StatusService(StatusRepository statusRepository, BlobService blobService) {
        this.statusRepository = statusRepository;
        this.blobService = blobService;
    }

    public UploadStatus getStatus(String fileId, String organizationFiscalCode) {
        Status status = statusRepository.findStatusById(fileId, organizationFiscalCode);
        log.info("[getStatus] status: " + status.getId());
        return map(status);
    }

    public UploadReport getReport(String broker, String orgFiscalCode, String fileId) {
        Status status = statusRepository.findStatusById(fileId, orgFiscalCode);
        if(status != null)
            return mapReport(status);
        else {
            UploadReport uploadReport = blobService.getReport(broker, orgFiscalCode, fileId);
            if(uploadReport == null)
                throw new AppException(NOT_FOUND, "Report Not Found", "The Upload Report for given file id "+ fileId + " does not exist");
            else return uploadReport;
        }
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
