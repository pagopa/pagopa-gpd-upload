package it.gov.pagopa.gpd.upload.service;

import io.micronaut.http.HttpStatus;
import it.gov.pagopa.gpd.upload.client.GPDClient;
import it.gov.pagopa.gpd.upload.entity.ResponseEntry;
import it.gov.pagopa.gpd.upload.entity.Status;
import it.gov.pagopa.gpd.upload.exception.AppException;
import it.gov.pagopa.gpd.upload.model.UploadInput;
import it.gov.pagopa.gpd.upload.model.pd.PaymentPositionModel;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
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

    public Status recover(String brokerId, String organizationFiscalCode, String uploadId) {
        Status current = statusService.getStatus(organizationFiscalCode, uploadId);
        UploadInput uploadInput = blobService.getUploadInput(brokerId, organizationFiscalCode, uploadId);

        // check if upload is pending
        if(current.upload.getCurrent() >= current.upload.getTotal())
            return current;

        // extract debt position id list
        List<String> inputIUPD = uploadInput.getPaymentPositions().stream()
                .map(PaymentPositionModel::getIupd).toList();
        List<String> processedIUPD = new ArrayList<>();
        current.upload.getResponses().forEach(
                res -> processedIUPD.addAll(res.getRequestIDs())
        );

        // sync with core to check if debt positions are already created
        List<String> createdIUPD = getAlreadyCreatedIUPD(organizationFiscalCode, inputIUPD, processedIUPD);

        // update status and save
        current.upload.addResponse(ResponseEntry.builder()
                .requestIDs(createdIUPD)
                .statusCode(HttpStatus.CREATED.getCode())
                .statusMessage(statusService.getDetail(HttpStatus.CREATED))
                .build());

        return statusService.upsert(current);
    }

    private List<String> getAlreadyCreatedIUPD(String organizationFiscalCode, List<String> inputIUPD, List<String> processedIUPD) {
        Set<String> inputIUPDSet = new HashSet<>(inputIUPD);
        Set<String> processedIUPDSet = new HashSet<>(processedIUPD);

        // diff
        if(!inputIUPDSet.removeAll(processedIUPDSet))
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Internal Server Error");

        List<String> createdIUPD = new ArrayList<>();

        // for each check if position is processed
        inputIUPDSet.forEach(id -> {
            // request to GPD
            HttpStatus httpStatus = gpdClient.getDebtPosition(organizationFiscalCode, id).getStatus();
            if(httpStatus.equals(HttpStatus.OK)) { // if request was successful
                createdIUPD.add(id);
            }
        });
        return createdIUPD;
    }
}