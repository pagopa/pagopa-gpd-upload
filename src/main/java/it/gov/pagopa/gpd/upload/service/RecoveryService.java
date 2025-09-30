package it.gov.pagopa.gpd.upload.service;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.HttpResponse;
import it.gov.pagopa.gpd.upload.client.GPDClient;
import it.gov.pagopa.gpd.upload.entity.ResponseEntry;
import it.gov.pagopa.gpd.upload.entity.Status;
import it.gov.pagopa.gpd.upload.exception.AppException;
import it.gov.pagopa.gpd.upload.model.ProblemJson;
import it.gov.pagopa.gpd.upload.model.UploadInput;
import it.gov.pagopa.gpd.upload.model.UploadOperation;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import it.gov.pagopa.gpd.upload.model.pd.PaymentPositionModel;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;

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

    public boolean recover(String brokerId, String organizationFiscalCode, String uploadId, ServiceType serviceType) {
        UploadInput uploadInput = blobService.getUploadInput(brokerId, organizationFiscalCode, uploadId, serviceType);
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
    	/* ORIGINAL COMMENTED CODE
        Status current = statusService.getStatus(organizationFiscalCode, uploadId);

        // check if upload is pending
        if(current.upload.getCurrent() >= current.upload.getTotal()) {
            if(current.upload.getEnd() != null)
                return false;
            // update end-upload-time if it is null
            current.upload.setEnd(LocalDateTime.now());
            Status updated = statusService.upsert(current);
            return updated != null;
        }

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
        if(!result.nonMatchingIUPD().isEmpty()) {
            current.upload.addResponse(ResponseEntry.builder()
                    .requestIDs(result.nonMatchingIUPD())
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.getCode())
                    .statusMessage(statusService.getDetail(toWrite))
                    .build());
        }
        current.upload.setEnd(LocalDateTime.now());

        Status updated = statusService.upsert(current);
        return updated != null;
    	 */

    	Status current = statusService.getStatus(organizationFiscalCode, uploadId);

    	// check if upload is pending
    	if (current.upload.getCurrent() >= current.upload.getTotal()) {
    		if (current.upload.getEnd() != null) {
    			return false;
    		}
    		// update end-upload-time if it is null
    		current.upload.setEnd(LocalDateTime.now());
    		Status updated = statusService.upsert(current);
    		return updated != null;
    	}

    	// extract debt position id list
    	List<String> processedIUPD = new ArrayList<>();
    	if (current.upload.getResponses() != null) {
    		current.upload.getResponses().forEach(res -> processedIUPD.addAll(res.getRequestIDs()));
    	}

    	// Filter IUPDs to verify with GPD --> toCheck contains only IUPDs that do not appear in processedIUPD
    	Set<String> already = new HashSet<>(processedIUPD);
    	List<String> toCheck = inputIUPD.stream()
    	    .distinct()
    	    .filter(iupd -> !already.contains(iupd))
    	    .toList();

    	// Call to GPD and group by (status, message)
    	Map<StatusKey, List<String>> groups = matchByStatusAndMessage(organizationFiscalCode, toCheck);

    	groups.forEach((key, ids) -> {
    		HttpStatus gpdStatus = key.status();
    		String gpdMessage = key.message();

    		// expected: GPD status is "expected"
    		boolean expected = gpdStatus.equals(toGetFromGPD);

    		// If expected -> write the "toWrite" status
    		// Otherwise, write the GPD status as is.
    		HttpStatus outStatus = expected ? toWrite : gpdStatus;

    		// Message rule:
    		// - If expected -> standard toWrite message
    		// - If gpdStatus is 200/201 -> ALWAYS standard message
    		// - Otherwise -> if gpdMessage is not empty, use that, otherwise the standard gpdStatus message
    		String outMessage;
    		if (expected) {
    			outMessage = statusService.getDetail(toWrite);
    		} else if (gpdStatus.equals(HttpStatus.OK) || gpdStatus.equals(HttpStatus.CREATED)) {
    			outMessage = statusService.getDetail(gpdStatus);
    		} else {
    			outMessage = (gpdMessage != null && !gpdMessage.isBlank())
    					? gpdMessage
    							: statusService.getDetail(gpdStatus);
    		}

    		current.upload.addResponse(ResponseEntry.builder()
    				.requestIDs(ids)
    				.statusCode(outStatus.getCode())
    				.statusMessage(outMessage)
    				.build());
    	});

    	current.upload.setEnd(LocalDateTime.now());

    	Status updated = statusService.upsert(current);
    	return updated != null;

    }
    
    /* ORIGINAL COMMENTED CODE
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
    */
    
    // Makes GPD calls and groups by (HttpStatus, message).
    // Message is "" when unavailable; LinkedHashMap to preserve arrival order.
    private Map<StatusKey, List<String>> matchByStatusAndMessage(String organizationFiscalCode, List<String> toCheck) {
    	Map<StatusKey, List<String>> grouped = new LinkedHashMap<>();
    	for (String id : toCheck) {
    		var response = gpdClient.getDebtPosition(organizationFiscalCode, id); // es. HttpResponse<?>
    		HttpStatus httpStatus = response.getStatus();
    		String gpdMessage = extractGpdMessage(response);
    		StatusKey key = new StatusKey(httpStatus, gpdMessage == null ? "" : gpdMessage);
    		grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(id);
    	}
    	return grouped;
    }
    
    // extracts the text message from the GPD response body (if present)
    private String extractGpdMessage(Object gpdResponse) {
        if (!(gpdResponse instanceof HttpResponse<?> resp)) {
            return null;
        }
        try {
            return resp.getBody(ProblemJson.class)
                    .map(ProblemJson::getDetail)
                    .filter(this::isNotBlank)
                    .or(() -> resp.getBody(String.class).filter(this::isNotBlank))
                    .or(() -> resp.getBody().map(String::valueOf).filter(this::isNotBlank))
                    .orElse(null);
        } catch (Exception e) {
            log.debug("Unable to extract GPD error message from response", e);
            return null;
        }
    }
    
    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
    
    private static record StatusKey(HttpStatus status, String message) {}
}