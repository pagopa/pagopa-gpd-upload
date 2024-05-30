package it.gov.pagopa.gpd.upload.service;

import it.gov.pagopa.gpd.upload.entity.ResponseEntry;
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
import java.util.ArrayList;
import java.util.List;

import static io.micronaut.http.HttpStatus.NOT_FOUND;

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
        log.info("[getStatus] status: " + status.getId());
        if(status == null)
            throw new AppException(NOT_FOUND, "STATUS NOT FOUND", "The Status for given fileId "+ fileId + " does not exist");
        return map(status);
    }

    public UploadReport getReport(String fileId, String organizationFiscalCode) {
        Status status = statusRepository.findStatusById(fileId, organizationFiscalCode);
        if(status == null)
            throw new AppException(NOT_FOUND, "STATUS NOT FOUND", "The Status for given fileId "+ fileId + " does not exist");
        return mapReport(status);
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
                .responses(getResponseEntries(status.upload))
                .startTime(status.upload.getStart())
                .endTime(status.upload.getEnd())
                .build();
    }

    private static List<ResponseEntry> getResponseEntries(Upload upload) {
        ResponseEntry ok = upload.getOk();
        ResponseEntry created = upload.getCreated();
        ResponseEntry badRequest = upload.getBadRequest();
        ResponseEntry notFound = upload.getNotFound();
        ResponseEntry conflict = upload.getConflict();
        List<ResponseEntry> responses = new ArrayList<>();

        if(!ok.getRequestIDs().isEmpty())
            responses.add(ok);
        if(!created.getRequestIDs().isEmpty())
            responses.add(created);
        if(!badRequest.getRequestIDs().isEmpty())
            responses.add(badRequest);
        if(!notFound.getRequestIDs().isEmpty())
            responses.add(notFound);
        if(!conflict.getRequestIDs().isEmpty())
            responses.add(conflict);
        return responses;
    }
}
