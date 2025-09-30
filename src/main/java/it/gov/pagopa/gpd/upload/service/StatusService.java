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
import it.gov.pagopa.gpd.upload.model.v1.UploadReport;
import it.gov.pagopa.gpd.upload.model.v1.UploadStatus;
import it.gov.pagopa.gpd.upload.model.v2.UploadStatusDTO;
import it.gov.pagopa.gpd.upload.model.v2.enumeration.OperationStatus;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import it.gov.pagopa.gpd.upload.model.v2.UploadReportDTO;
import it.gov.pagopa.gpd.upload.repository.impl.StatusRepositoryImpl;
import it.gov.pagopa.gpd.upload.utils.ResponseEntryDTOMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.time.LocalTime;
import java.util.Objects;
import java.util.function.Function;

import static io.micronaut.http.HttpStatus.NOT_FOUND;

@Singleton
@Slf4j
public class StatusService {
    private StatusRepositoryImpl statusRepository;

    private ResponseEntryDTOMapper responseEntryDTOMapper;

    @Inject
    public StatusService(StatusRepositoryImpl statusRepository,
                         ResponseEntryDTOMapper responseEntryDTOMapper) {
        this.statusRepository = statusRepository;
        this.responseEntryDTOMapper = responseEntryDTOMapper;
    }

    public UploadStatus getUploadStatus(String brokerId, String fileId, String organizationFiscalCode, ServiceType serviceType) {
        return getData(brokerId, organizationFiscalCode, fileId, serviceType, this::mapStatusV1);
    }

    public UploadStatusDTO getUploadStatusV2(String brokerId, String fileId, String organizationFiscalCode, ServiceType serviceType) {
        return getData(brokerId, organizationFiscalCode, fileId, serviceType, this::mapStatusV2);
    }

    public UploadReport getReportV1(String brokerCode, String orgFiscalCode, String fileId, ServiceType serviceType) {
        return getData(brokerCode, orgFiscalCode, fileId, serviceType, this::mapReport);
    }

    public UploadReportDTO getReportV2(String brokerCode, String orgFiscalCode, String fileId, ServiceType serviceType) {
        return getData(brokerCode, orgFiscalCode, fileId, serviceType, this::mapReportV2);
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

    private <R> R getData(String brokerCode, String orgFiscalCode, String fileId, ServiceType serviceType, Function<Status, R> mapper) {

        Status status = getStatus(brokerCode, orgFiscalCode, fileId, serviceType);

        boolean isValid = status.getServiceType() == null && serviceType.equals(ServiceType.GPD) ||
                Objects.equals(serviceType, status.getServiceType());

        if (isValid) {
            // apply the specific mapping function (mapReport or mapReportV2)
            return mapper.apply(status);
        }

        throw new AppException(NOT_FOUND, "STATUS NOT FOUND", String.format("The data for given fileId %s does not exist for %s", fileId, serviceType.name()));
    }


    private Status getStatus(String brokerCode, String orgFiscalCode, String fileId, ServiceType serviceType) {
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
            return statusList.get(0);
        }
        throw new AppException(NOT_FOUND, "STATUS NOT FOUND", String.format("The status for given fileId %s does not exist for %s", fileId, serviceType.name()));
    }

    private UploadStatus mapStatusV1(Status status) {
        return UploadStatus.builder()
                .uploadID(status.getId())
                .processedItem(status.upload.getCurrent())
                .submittedItem(status.upload.getTotal())
                .startTime(status.upload.getStart())
                .build();
    }

    private UploadStatusDTO mapStatusV2(Status status) {
        return UploadStatusDTO.builder()
                .fileId(status.getId())
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

        StatusRepositoryImpl.FileIdsPage page = statusRepository.findFileIdsPage(
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