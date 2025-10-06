package it.gov.pagopa.gpd.upload.service;

import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import io.micronaut.context.annotation.Value;
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
import it.gov.pagopa.gpd.upload.repository.StatusRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Singleton
@Slf4j
public class SupportService {

    private StatusRepository statusRepository;

    private final StatusService statusService;
    private final BlobService blobService;
    private final GPDClient gpdClient;
    private final SlackService slackService;

    @Value("${env}")
    private String env;

    private ZoneId zone = ZoneId.of("Europe/Rome");


    @Inject
    public SupportService(StatusRepository statusRepository, StatusService statusService, BlobService blobService, GPDClient gpdClient, SlackService slackService) {
        this.statusRepository = statusRepository;
        this.statusService = statusService;
        this.blobService = blobService;
        this.gpdClient = gpdClient;
        this.slackService = slackService;
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

    public ProblemJson monitoring(LocalDateTime from, LocalDateTime to) {
        String sqlQuery = "SELECT * FROM c WHERE c._ts >= @fromTs AND c._ts <= @toTs AND c.upload.current != c.upload.total";
        SqlQuerySpec querySpec = new SqlQuerySpec(
                sqlQuery,
                Arrays.asList(
                        new SqlParameter("@fromTs", from.atZone(zone).toEpochSecond()),
                        new SqlParameter("@toTs", to.atZone(zone).toEpochSecond())
                )
        );

        CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();
        options.setPartitionKey(new PartitionKey("fiscalCode"));

        List<Status> statusList = statusRepository.find(querySpec,  new CosmosQueryRequestOptions());

        final String responseTitle = String.format("Monitoring Alert: %s -> %s", from.atZone(zone).toLocalDateTime(), to.atZone(zone).toLocalDateTime());
        if (!statusList.isEmpty()) {
            // Send webhook notification
            if (env.equalsIgnoreCase("prod")) {
                try {
                    File tempFile = generateCsvContent(statusList);
                    slackService.uploadCsv(
                            ":warning: ACA/GPD Caricamento Massivo",
                            "Lista di elaborazioni non concluse",
                            tempFile
                    );
                    Files.delete(tempFile.toPath());
                } catch (Exception e) {
                    log.error("Error sending CSV file to Slack", e);
                }
            }

            return ProblemJson.builder()
                    .title(responseTitle)
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.getCode())
                    .detail("There are " + statusList.size() + " pending massive operations in the given window. " +
                            "Report is published in the Slack channel PagoPA-Pagamenti-Alert if in PROD environment")
                    .build();
        }

        return ProblemJson.builder()
                .title(responseTitle)
                .status(HttpStatus.OK.getCode())
                .detail("No pending massive operation in the given window")
                .build();
    }

    public File generateCsvContent(List<Status> statusList) throws IOException {
        Path tempPath = Files.createTempFile("gpd_massive_operation_pending_", ".csv");
        File tempFile = tempPath.toFile();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.append("FileId,Broker,Organization,Start,Current/Total\n");
            for (Status status : statusList) {
                writer.append(String.format("%s,%s,%s,%s,%s\n",
                        status.getId(),
                        status.getBrokerID(),
                        status.getFiscalCode(),
                        status.upload.getStart().atZone(zone).toLocalDate().toString(),
                        String.format("%d/%d", status.upload.getCurrent(), status.upload.getTotal())
                ));
            }
        }
        return tempFile;
    }

    private boolean recover(String organizationFiscalCode, String uploadId, List<String> inputIUPD, HttpStatus toGetFromGPD, HttpStatus toWrite) {
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