package it.gov.pagopa.gpd.upload.repository;

import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.PartitionKey;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpStatus;
import it.gov.pagopa.gpd.upload.entity.Status;
import it.gov.pagopa.gpd.upload.exception.AppException;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import static io.micronaut.http.HttpStatus.NOT_FOUND;

@Singleton
@Context
@Slf4j
public class StatusRepository {

    @Value("${cosmos.uri}")
    private String cosmosURI;

    @Value("${cosmos.key}")
    private String cosmosKey;

    @Value("${cosmos.database.name}")
    private String databaseName;

    @Value("${cosmos.container.name}")
    private String containerName;

    private CosmosContainer container;

    @PostConstruct
    public void init() {
        CosmosClient cosmosClient = new CosmosClientBuilder()
                .endpoint(cosmosURI)
                .key(cosmosKey)
                .buildClient();
        container = cosmosClient.getDatabase(databaseName).getContainer(containerName);
    }

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

    public Status findStatusById(String id, String fiscalCode) {
        try {
            CosmosItemResponse<Status> response = container.readItem(id, new PartitionKey(fiscalCode), Status.class);
            return response.getItem();
        } catch (CosmosException ex) {
            log.error("[Error][StatusRepository@findStatusById] The Status retrieval was not successful: {}", ex.getStatusCode());
            if(ex.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR.getCode())
                throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.name(), "The Status for given fileId " + id + " is not available");
            else if(ex.getStatusCode() == NOT_FOUND.getCode())
                throw new AppException(NOT_FOUND, "STATUS NOT FOUND", "The Status for given fileId " + id + " does not exist");
            else throw new AppException(HttpStatus.valueOf(ex.getStatusCode()), String.valueOf(ex.getStatusCode()), "Status retrieval failed");
        }
    }
}
