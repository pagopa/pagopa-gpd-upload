package it.gov.pagopa.gpd.upload.repository.impl;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.FeedResponse;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedIterable;
import io.micronaut.context.annotation.Context;
import io.micronaut.http.HttpStatus;
import it.gov.pagopa.gpd.upload.entity.Status;
import it.gov.pagopa.gpd.upload.exception.AppException;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import it.gov.pagopa.gpd.upload.repository.StatusRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.micronaut.http.HttpStatus.NOT_FOUND;

@Singleton
@Context
@Slf4j
public class StatusRepositoryImpl implements StatusRepository {
    @Inject
    private CosmosContainer container;

    public static final class FileIdsPage {
        private final List<String> fileIds;
        private final String continuationToken;

        public FileIdsPage(List<String> fileIds, String continuationToken) {
            this.fileIds = fileIds;
            this.continuationToken = continuationToken;
        }
        public List<String> getFileIds() { return fileIds; }
        public String getContinuationToken() { return continuationToken; }
    }

    @Override
    public Status saveStatus(Status status) {
        try {
            CosmosItemResponse<Status> response = container.createItem(status);
            return response.getItem();
        } catch (CosmosException ex) {
            log.error("[Error][StatusRepository@saveStatus] The Status saving was not successful: {}", ex.getStatusCode());
            if(ex.getStatusCode() == HttpStatus.CONFLICT.getCode())
                return findStatusById(status.getId(), status.fiscalCode); // already exists, created by blob-consumer function
            if(ex.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR.getCode())
                throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.name(), "Status saving unavailable");
            else
                throw new AppException(HttpStatus.valueOf(ex.getStatusCode()), String.valueOf(ex.getStatusCode()), "Status saving failed");
        }
    }

    @Override
    public Status upsert(Status status) {
        try {
            CosmosItemResponse<Status> response = container.upsertItem(status);
            return response.getItem();
        } catch (CosmosException ex) {
            log.error("[Error][StatusRepository@saveStatus] The Status upsert was not successful: {}", ex.getStatusCode());
            if(ex.getStatusCode() == HttpStatus.CONFLICT.getCode())
                return findStatusById(status.getId(), status.fiscalCode); // already exists, created by blob-consumer function
            if(ex.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR.getCode())
                throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.name(), "Status upsert unavailable");
            else
                throw new AppException(HttpStatus.valueOf(ex.getStatusCode()), String.valueOf(ex.getStatusCode()), "Status upsert failed");
        }
    }

    @Override
    public Status findStatusById(String id, String fiscalCode) {
        try {
            CosmosItemResponse<Status> response = container.readItem(id, new PartitionKey(fiscalCode), Status.class);
            return response.getItem();
        } catch (CosmosException ex) {
            log.error("[Error][StatusRepository@findStatusById] The Status retrieval was not successful: {}", ex.getStatusCode());
            if(ex.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR.getCode())
                throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.name(),
                        String.format("The Status for given fileId %s is not available", id));
            else if(ex.getStatusCode() == NOT_FOUND.getCode())
                throw new AppException(NOT_FOUND, "STATUS NOT FOUND", String.format("The Status for given fileId %s does not exist", id));
            else throw new AppException(HttpStatus.valueOf(ex.getStatusCode()), String.valueOf(ex.getStatusCode()), "Status retrieval failed");
        }
    }

    @Override
    public List<Status> find(String query) {
        try {
            CosmosPagedIterable<Status> response = container.queryItems(new SqlQuerySpec(query), new CosmosQueryRequestOptions(), Status.class);
            return response.stream().toList();
        } catch (CosmosException ex) {
            log.error("[Error][StatusRepository@findPending] The Status retrieval was not successful: {}", ex.getStatusCode());
            if(ex.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR.getCode())
                throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.name(), "The Status retrieval was not successful");
            else if(ex.getStatusCode() == NOT_FOUND.getCode())
                throw new AppException(NOT_FOUND, "STATUS NOT FOUND", "The Status for given query doesn't exist");
            else throw new AppException(HttpStatus.valueOf(ex.getStatusCode()), String.valueOf(ex.getStatusCode()), "Status retrieval failed");
        }
    }

    @Override
    public List<Status> find(SqlQuerySpec query, CosmosQueryRequestOptions queryRequestOptions) {
        try {
            CosmosPagedIterable<Status> response = container.queryItems(
                    query,
                    queryRequestOptions,
                    Status.class
            );
            return response.stream().toList();
        } catch (CosmosException ex) {
            log.error("[Error][StatusRepository@findPending] The Status retrieval was not successful: {}", ex.getStatusCode());
            if(ex.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR.getCode())
                throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.name(), "The Status retrieval was not successful");
            else if(ex.getStatusCode() == NOT_FOUND.getCode())
                throw new AppException(NOT_FOUND, "STATUS NOT FOUND", "The Status for given query doesn't exist");
            else throw new AppException(HttpStatus.valueOf(ex.getStatusCode()), String.valueOf(ex.getStatusCode()), "Status retrieval failed");
        }
    }

    @Override
    public FileIdsPage findFileIdsPage(
            String brokerCode,
            String organizationFiscalCode,
            LocalDateTime from,
            LocalDateTime to,
            int size,
            String continuationToken,
            ServiceType serviceType
    ) {
        try {
            // Serialize dates to ISO-8601
            final DateTimeFormatter iso = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            final String fromIso = from.format(iso);
            final String toIso   = to.format(iso);

            final List<SqlParameter> params = new ArrayList<>();
            params.add(new SqlParameter("@broker", brokerCode));
            params.add(new SqlParameter("@org", organizationFiscalCode));
            params.add(new SqlParameter("@from", fromIso));
            params.add(new SqlParameter("@to", toIso));
            params.add(new SqlParameter("@serviceType", serviceType.name())); // "GPD" or "ACA"


            // Query: Return ONLY the id, sort by start DESC (newest)
            final String sql =
            		"SELECT VALUE c.id " +
            				"FROM c " +
            				"WHERE c.brokerID = @broker " +
            				"  AND c.fiscalCode = @org " +
            				"  AND (c.serviceType = @serviceType " +
            				"       OR (@serviceType = 'GPD' AND NOT IS_DEFINED(c.serviceType))) " + // includes docs without serviceType only if @serviceType = 'GPD'
            				"  AND c.upload.start >= @from " +
            				"  AND c.upload.start <= @to " +
            				"ORDER BY c.upload.start DESC";


            final SqlQuerySpec spec = new SqlQuerySpec(sql, params);
            final CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();
            // Optional: Enable query metrics for tuning
            // options.setQueryMetricsEnabled(true);

            log.debug("[findFileIdsPage] SQL:\n{}", sql);
            log.debug("[findFileIdsPage] params: @broker={}, @org={}, @serviceType={}, @from={}, @to={}, size={}, cont.len={}",
                    brokerCode, organizationFiscalCode, serviceType.name(), fromIso, toIso, size,
                    continuationToken == null ? 0 : continuationToken.length());

            // Take ONLY the first page of the batch to comply with 'size'
            String nextToken = null;
            List<String> ids = Collections.emptyList();

            Iterable<FeedResponse<String>> pages =
                    container.queryItems(spec, options, String.class)
                             .iterableByPage(continuationToken, size);

            var it = pages.iterator();
            if (it.hasNext()) {
                FeedResponse<String> page = it.next();
                ids = page.getResults();
                nextToken = page.getContinuationToken();
            }

            return new FileIdsPage(ids, nextToken);
        } catch (CosmosException ex) {
            log.error("[Error][StatusRepository@findFileIdsPage] Retrieval failed: {}", ex.getStatusCode(), ex);
            if (ex.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR.getCode()) {
                throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR,
                        HttpStatus.INTERNAL_SERVER_ERROR.name(),
                        "FileId retrieval unavailable");
            } else {
                throw new AppException(HttpStatus.valueOf(ex.getStatusCode()),
                        String.valueOf(ex.getStatusCode()),
                        "FileId retrieval failed");
            }
        }
    }

}
