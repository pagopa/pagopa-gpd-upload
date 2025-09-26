package it.gov.pagopa.gpd.upload.service;

import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import io.micronaut.http.HttpStatus;
import it.gov.pagopa.gpd.upload.entity.Status;
import it.gov.pagopa.gpd.upload.entity.Upload;
import it.gov.pagopa.gpd.upload.exception.AppException;
import it.gov.pagopa.gpd.upload.model.FileIdListResponse;
import it.gov.pagopa.gpd.upload.model.UploadReport;
import it.gov.pagopa.gpd.upload.model.v2.UploadStatusV2;
import it.gov.pagopa.gpd.upload.model.v2.enumeration.OperationStatus;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import it.gov.pagopa.gpd.upload.model.v2.UploadReportDTO;
import it.gov.pagopa.gpd.upload.repository.StatusRepository;
import it.gov.pagopa.gpd.upload.utils.ResponseEntryDTOMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

import static io.micronaut.http.HttpStatus.NOT_FOUND;

@Singleton
@Slf4j
public class StatusService {
    private StatusRepository statusRepository;

    private ResponseEntryDTOMapper responseEntryDTOMapper;

    @Inject
    public StatusService(StatusRepository statusRepository,
                         ResponseEntryDTOMapper responseEntryDTOMapper) {
        this.statusRepository = statusRepository;
        this.responseEntryDTOMapper = responseEntryDTOMapper;
    }

    public UploadStatusV2 getUploadStatus(String uploadId, String organizationFiscalCode, ServiceType serviceType) {
        Status status = statusRepository.findStatusById(uploadId, organizationFiscalCode);
        log.debug("[getStatus] status: " + status.getId());

        if(status.getServiceType() == null && serviceType.equals(ServiceType.GPD) || Objects.equals(serviceType, status.getServiceType())){
            return map(status);
        }
        throw new AppException(NOT_FOUND, "STATUS NOT FOUND", String.format("The Status for given uploadId %s does not exist for %s", uploadId, serviceType.name()));
    }

    public UploadReport getReport(String orgFiscalCode, String fileId, ServiceType serviceType) {
        Status status = statusRepository.findStatusById(fileId, orgFiscalCode);

        if(status.getServiceType() == null && serviceType.equals(ServiceType.GPD) || Objects.equals(serviceType, status.getServiceType())){
            return mapReport(status);
        }
        throw new AppException(NOT_FOUND, "STATUS NOT FOUND", String.format("The Status for given fileId %s does not exist for %s", fileId, serviceType.name()));
    }

    public UploadReportDTO getReport(String brokerCode, String orgFiscalCode, String fileId, ServiceType serviceType) {
        String sqlQuery = "SELECT * FROM c WHERE c.id = @fileId AND c.fiscalCode = @orgFiscalCode AND c.brokerID = @brokerCode";
        SqlQuerySpec querySpec = new SqlQuerySpec(
                sqlQuery,
                Arrays.asList(
                        new SqlParameter("@fileId", fileId),
                        new SqlParameter("@orgFiscalCode", orgFiscalCode),
                        new SqlParameter("@brokerCode", brokerCode)
                )
        );
        CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();
        options.setPartitionKey(new PartitionKey(orgFiscalCode));

        List<Status> statusList = statusRepository.find(querySpec, options);
        if (!statusList.isEmpty()) {

            Status status = statusList.get(0);

            if(status.getServiceType() == null || Objects.equals(serviceType, status.getServiceType())){
                return mapReportV2(status);
            }
        }

        throw new AppException(NOT_FOUND, "REPORT NOT FOUND", String.format("The report for given fileId %s does not exist for %s", fileId, serviceType.name()));
    }

    public Status getStatus(String orgFiscalCode, String fileId) {
        return statusRepository.findStatusById(fileId, orgFiscalCode);
    }

    public void createUploadStatus(String organizationFiscalCode, String brokerId, String fileId, int totalItem, ServiceType serviceType) {
        Upload upload = Upload.builder()
                .current(0)
                .total(totalItem)
                .start(LocalDateTime.now())
                .build();
        Status status = Status.builder()
                .id(fileId)
                .brokerID(brokerId)
                .fiscalCode(organizationFiscalCode)
                .serviceType(serviceType)
                .upload(upload)
                .build();

        statusRepository.saveStatus(status);
    }

    public Status upsert(Status status) {
        return statusRepository.upsert(status);
    }

    private UploadStatusV2 map(Status status) {
        return UploadStatusV2.builder()
                .uploadID(status.getId())
                .processedItem(status.upload.getCurrent())
                .submittedItem(status.upload.getTotal())
                .startTime(status.upload.getStart())
                .operationStatus(getOperationStatus(status))
                .build();
    }

    public OperationStatus getOperationStatus(Status status){
        if(status.getUpload().getCurrent() == status.getUpload().getTotal()){
            if(status.getUpload().getResponses() != null){
                if(status.getUpload().getResponses().stream().allMatch(el -> el.getStatusCode() >= 400)){
                    return OperationStatus.COMPLETED_UNSUCCESSFULLY;
                } else if(status.getUpload().getResponses().stream().anyMatch(el -> el.getStatusCode() >= 400)){
                    return OperationStatus.COMPLETED_WITH_WARNINGS;
                }
            }

            return OperationStatus.COMPLETED;
        }

        return OperationStatus.IN_PROGRESS;
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

    public UploadReportDTO mapReportV2(Status status) {
        return UploadReportDTO.builder()
                .fileId(status.getId())
                .processedItem(status.upload.getCurrent())
                .submittedItem(status.upload.getTotal())
                .responses(responseEntryDTOMapper.toDTOs(status.upload.getResponses()))
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

    public FileIdListResponse getFileIdList(
            String brokerCode,
            String organizationFiscalCode,
            LocalDate from,
            LocalDate to,
            int size,
            String continuationToken,
            ServiceType serviceType
    ) {
        // Convert to LocalDateTime inclusive of day (00:00:00.000 -> 23:59:59.999)
        final var fromDateTime = from.atStartOfDay();
        final var toDateTime   = to.atTime(LocalTime.MAX); // 23:59:59.999999999

        StatusRepository.FileIdsPage page = statusRepository.findFileIdsPage(
                brokerCode,
                organizationFiscalCode,
                fromDateTime,
                toDateTime,
                size,
                continuationToken,
                serviceType
        );

        List<String> ids = page.getFileIds();
        String nextToken = page.getContinuationToken();

        return FileIdListResponse.builder()
                .fileIds(ids)
                .size(ids != null ? ids.size() : 0)
                .hasMore(nextToken != null && !nextToken.isBlank())
                // It is placed in the body for the controller, which will then move it to the x-continuation-token header
                .continuationToken(nextToken)
                .build();
    }
}