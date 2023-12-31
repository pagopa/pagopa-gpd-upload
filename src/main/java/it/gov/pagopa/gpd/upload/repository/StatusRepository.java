package it.gov.pagopa.gpd.upload.repository;

import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
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

    private CosmosClient cosmosClient;
    private CosmosContainer container;

    @PostConstruct
    public void init() {
        cosmosClient = new CosmosClientBuilder()
                .endpoint(cosmosURI)
                .key(cosmosKey)
                .buildClient();
        container = cosmosClient.getDatabase(databaseName).getContainer(containerName);
    }

    public Status saveStatus(Status status) {
        CosmosItemResponse<Status> response = container.createItem(status);
        if (response.getStatusCode() != HttpStatus.CREATED.getCode()) {
            log.error("the Status saving was not successful: " + response);
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "COSMOS UNAVAILABLE", "the Status saving was not successful");
        }
        return response.getItem();
    }

    public Status findStatusById(String id) {
        CosmosItemResponse<Status> response = container.readItem(id, PartitionKey.NONE, Status.class);
        if (response.getStatusCode() != HttpStatus.OK.getCode()) {
            log.error("the Status retrieval was not successful: " + response);
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "COSMOS UNAVAILABLE", "the Status retrieval was not successful");
        }
        return response.getItem();
    }
}
