package it.gov.pagopa.gpd.upload.service;

import io.micronaut.http.HttpStatus;
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

    public UploadStatus getUploadStatus(String fileId, String organizationFiscalCode) {
        Status status = statusRepository.findStatusById(fileId, organizationFiscalCode);
        log.debug("[getStatus] status: " + status.getId());
        return map(status);
    }

    public UploadReport getReport(String orgFiscalCode, String fileId) {
        return mapReport(statusRepository.findStatusById(fileId, orgFiscalCode));
    }

    public Status getStatus(String orgFiscalCode, String fileId) {
        return statusRepository.findStatusById(fileId, orgFiscalCode);
    }


    public void createUploadStatus(String organizationFiscalCode, String brokerId, String fileId, int totalItem) {
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

        statusRepository.saveStatus(status);
    }

    public Status upsert(Status status) {
        return statusRepository.upsert(status);
    }

    private UploadStatus map(Status status) {
        return UploadStatus.builder()
                .uploadID(status.getId())
                .processedItem(status.upload.getCurrent())
                .submittedItem(status.upload.getTotal())
                .startTime(status.upload.getStart())
                .build();
    }

    public UploadReport mapReport(Status status) {
        return UploadReport.builder()
                .uploadID(status.getId())
                .processedItem(status.upload.getCurrent())
                .submittedItem(status.upload.getTotal())
                .responses(status.upload.getResponses())
                .startTime(status.upload.getStart())
                .endTime(status.upload.getEnd())
                .build();
    }

    public String getDetail(HttpStatus status) {
        return switch (status) {
            case CREATED -> "Debt position CREATED";
            case OK -> "Debt position operation OK";
            case NOT_FOUND -> "Debt position NOT FOUND";
            case CONFLICT -> "Debt position IUPD or NAV/IUV already exists for organization code";
            case UNAUTHORIZED -> "UNAUTHORIZED";
            case FORBIDDEN -> "FORBIDDEN";
            case INTERNAL_SERVER_ERROR -> "Internal Server Error: operation not completed";
            case BAD_REQUEST -> "Bad request";
            default -> status.toString();
        };
    }
}
