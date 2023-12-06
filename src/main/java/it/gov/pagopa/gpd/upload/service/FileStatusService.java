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

@Singleton
@Slf4j
public class FileStatusService {
    @Inject
    StatusRepository statusRepository;

    public FileStatus getStatus(String fileId) {
        return map(statusRepository.findStatusById(fileId));
    }

    public Status createUploadStatus(String organizationFiscalCode, String fileId, PaymentPositionsModel paymentPositionsModel) {
        Upload upload = Upload.builder()
                .current(0)
                .total(paymentPositionsModel.getPaymentPositions().size())
                .successIUPD(new ArrayList<>())
                .failedIUPD(new ArrayList<>())
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
        return FileStatus.builder()
                .fileId(status.id)
                .successIUPD(status.upload.getSuccessIUPD())
                .failedIUPD(status.upload.getFailedIUPD())
                .uploadTime(status.upload.getStart())
                .build();
    }
}
