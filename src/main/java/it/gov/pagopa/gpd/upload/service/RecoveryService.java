package it.gov.pagopa.gpd.upload.service;

import io.micronaut.http.HttpStatus;
import it.gov.pagopa.gpd.upload.client.GPDClient;
import it.gov.pagopa.gpd.upload.entity.MatchResult;
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
import java.util.List;

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

    public boolean recover(String brokerId, String organizationFiscalCode, String uploadId) {
        UploadInput uploadInput = blobService.getUploadInput(brokerId, organizationFiscalCode, uploadId);
        List<String> inputIUPD;

        if(uploadInput.getUploadOperation().equals(UploadOperation.CREATE)) {
            inputIUPD = uploadInput.getPaymentPositions().stream().map(PaymentPositionModel::getIupd).toList();
            return recover(organizationFiscalCode, uploadId, inputIUPD, HttpStatus.OK, HttpStatus.CREATED);
        } else if(uploadInput.getUploadOperation().equals(UploadOperation.DELETE)) {
            inputIUPD = uploadInput.getPaymentPositionIUPDs();
            return recover(organizationFiscalCode, uploadId, inputIUPD, HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND);
        } else {
            throw new AppException(HttpStatus.NOT_FOUND, "Upload operation not processable",
                    String.format("Not exists CREATE or DELETE operation with upload-id %s", uploadId));
        }
    }

    private boolean recover(String organizationFiscalCode, String uploadId, List<String> inputIUPD, HttpStatus toGetFromGPD, HttpStatus toWrite) {
        Status current = statusService.getStatus(organizationFiscalCode, uploadId);

        // check if upload is pending
        if(current.upload.getCurrent() >= current.upload.getTotal())
            return false;

        // extract debt position id list
        List<String> processedIUPD = new ArrayList<>();
        current.upload.getResponses().forEach(
                res -> processedIUPD.addAll(res.getRequestIDs())
        );

        // sync with core to check if debt positions are already processed (DELETED or CREATED -> NOT_EXISTS, EXISTS)
        MatchResult result = match(organizationFiscalCode, inputIUPD, processedIUPD, toGetFromGPD);

        // update status and save
        current.upload.addResponse(ResponseEntry.builder()
                .requestIDs(result.matchingIUPD())
                .statusCode(toWrite.getCode())
                .statusMessage(statusService.getDetail(toWrite))
                .build());
        // for non-matching IUPD the code is 500
        current.upload.addResponse(ResponseEntry.builder()
                .requestIDs(result.nonMatchingIUPD())
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.getCode())
                .statusMessage(statusService.getDetail(toWrite))
                .build());
        current.upload.setEnd(LocalDateTime.now());

        Status updated = statusService.upsert(current);
        return updated != null;
    }

    private MatchResult match(String organizationFiscalCode, List<String> inputIUPD, List<String> processedIUPD, HttpStatus target) {
        List<String> differenceIUPD = inputIUPD.stream()
                .filter(iupd -> !processedIUPD.contains(iupd))
                .toList();

        List<String> matchingIUPD = new ArrayList<>();
        List<String> nonMatchingIUPD = new ArrayList<>();

        // for each check if position is processed
        differenceIUPD.forEach(id -> {
            // request to GPD
            HttpStatus httpStatus = gpdClient.getDebtPosition(organizationFiscalCode, id).getStatus();
            // if status code match the target
            if(httpStatus.equals(target)) {
                matchingIUPD.add(id);
            } else {
                nonMatchingIUPD.add(id);
            }
        });

        return new MatchResult(matchingIUPD, nonMatchingIUPD);
    }
}