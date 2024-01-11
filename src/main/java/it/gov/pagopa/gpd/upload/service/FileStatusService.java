package it.gov.pagopa.gpd.upload.service;

import it.gov.pagopa.gpd.upload.entity.Status;
import it.gov.pagopa.gpd.upload.entity.Upload;
import it.gov.pagopa.gpd.upload.model.FileStatus;
import it.gov.pagopa.gpd.upload.model.pd.PaymentPositionsModel;
import it.gov.pagopa.gpd.upload.repository.StatusRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class FileStatusService {
    @Inject
    StatusRepository statusRepository;

    public FileStatus getStatus(String fileId, String organizationFiscalCode) {
        return map(statusRepository.findStatusById(fileId, organizationFiscalCode));
    }

    public Status createUploadStatus(String organizationFiscalCode, String fileId, PaymentPositionsModel paymentPositionsModel) {
        Upload upload = Upload.builder()
                .current(0)
                .total(paymentPositionsModel.getPaymentPositions().size())
                .successIUPD(new ArrayList<>())
                .failedIUPDs(new ArrayList<>())
                .start(LocalDateTime.now())
                .build();
        Status status = Status.builder()
                .id(fileId)
                .fiscalCode(organizationFiscalCode)
                .upload(upload)
                .build();

        return statusRepository.saveStatus(status);
    }

    private FileStatus map(Status status) {
        // returns only IUPD codes because FailedIUPD could be too verbose and would generate a high size response
        ArrayList<String> failedIUPD = status.upload.getFailedIUPDs().stream()
                .flatMap(f -> f.getSkippedIUPDs().stream())
                .collect(Collectors.toCollection(ArrayList::new));

        return FileStatus.builder()
                .fileId(status.id)
                .processed(status.upload.getCurrent())
                .uploaded(status.upload.getTotal())
                .successIUPD(status.upload.getSuccessIUPD())
                .failedIUPD(failedIUPD)
                .uploadTime(status.upload.getStart())
                .build();
    }
}
