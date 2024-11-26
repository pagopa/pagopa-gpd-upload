package it.gov.pagopa.gpd.upload.service;

import io.micronaut.http.HttpStatus;
import it.gov.pagopa.gpd.upload.client.GPDClient;
import it.gov.pagopa.gpd.upload.entity.ResponseEntry;
import it.gov.pagopa.gpd.upload.entity.Status;
import it.gov.pagopa.gpd.upload.exception.AppException;
import it.gov.pagopa.gpd.upload.model.UploadInput;
import it.gov.pagopa.gpd.upload.model.UploadOperation;
import it.gov.pagopa.gpd.upload.model.pd.PaymentPositionModel;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Singleton
@Slf4j
public class RecoveryService {
    private final StatusService statusService;
    private final BlobService blobService;
    private final GPDClient gpdClient;

    @Inject
    public RecoveryService(StatusService statusService, BlobService blobService, GPDClient gpdClient) {
        this.statusService = statusService;
        this.blobService = blobService;
        this.gpdClient = gpdClient;
    }

    public Status recoverCreated(String brokerId, String organizationFiscalCode, String uploadId) {
        UploadInput uploadInput = blobService.getUploadInput(brokerId, organizationFiscalCode, uploadId);
        if(!uploadInput.getUploadOperation().equals(UploadOperation.CREATE))
            throw new AppException(HttpStatus.NOT_FOUND,
                    "Upload operation not processable", String.format("Not exists create operation with upload-id %s", uploadId));

        List<String> inputIUPD = uploadInput.getPaymentPositionIUPDs();
        return this.recover(organizationFiscalCode, uploadId, inputIUPD, HttpStatus.OK, HttpStatus.CREATED);
    }

    public Status recoverDeleted(String brokerId, String organizationFiscalCode, String uploadId) {
        UploadInput uploadInput = blobService.getUploadInput(brokerId, organizationFiscalCode, uploadId);
        if(!uploadInput.getUploadOperation().equals(UploadOperation.DELETE))
            throw new AppException(HttpStatus.NOT_FOUND,
                    "Upload operation not processable", String.format("Not exists delete operation with upload-id %s", uploadId));
        List<String> inputIUPD = uploadInput.getPaymentPositionIUPDs();
        return this.recover(organizationFiscalCode, uploadId, inputIUPD, HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    public Status recover(String organizationFiscalCode, String uploadId, List<String> inputIUPD, HttpStatus statusToCheck, HttpStatus uploadStatus) {
        Status current = statusService.getStatus(organizationFiscalCode, uploadId);

        // check if upload is pending
        if(current.upload.getCurrent() >= current.upload.getTotal())
            return current;

        // extract debt position id list
        List<String> processedIUPD = new ArrayList<>();
        current.upload.getResponses().forEach(
                res -> processedIUPD.addAll(res.getRequestIDs())
        );

        // sync with core to check if debt positions are already processed (DELETED or CREATED -> NOT_EXISTS, EXISTS)
        List<String> deletedIUPD = getAlreadyIUPD(organizationFiscalCode, inputIUPD, processedIUPD, statusToCheck);

        // update status and save
        current.upload.addResponse(ResponseEntry.builder()
                .requestIDs(deletedIUPD)
                .statusCode(uploadStatus.getCode())
                .statusMessage(statusService.getDetail(uploadStatus))
                .build());
        current.upload.setEnd(LocalDateTime.now());

        return statusService.upsert(current);
    }

    private List<String> getAlreadyIUPD(String organizationFiscalCode, List<String> inputIUPD, List<String> processedIUPD, HttpStatus target) {
        Set<String> inputIUPDSet = new HashSet<>(inputIUPD);
        Set<String> processedIUPDSet = new HashSet<>(processedIUPD);

        // diff
        if(!inputIUPDSet.removeAll(processedIUPDSet))
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Internal Server Error");

        List<String> alreadyIUPD = new ArrayList<>();

        // for each check if position is processed
        inputIUPDSet.forEach(id -> {
            // request to GPD
            HttpStatus httpStatus = gpdClient.getDebtPosition(organizationFiscalCode, id).getStatus();
            if(httpStatus.equals(target)) { // if request was successful
                alreadyIUPD.add(id);
            }
        });
        return alreadyIUPD;
    }
}