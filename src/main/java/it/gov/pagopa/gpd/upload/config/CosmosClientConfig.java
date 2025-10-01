package it.gov.pagopa.gpd.upload.config;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;

@Factory
public class CosmosClientConfig {
    @Bean
    CosmosContainer cosmosContainerBean(
            @Value("${cosmos.uri}") String cosmosURI,
            @Value("${cosmos.key}") String cosmosKey,
            @Value("${cosmos.database.name}") String databaseName,
            @Value("${cosmos.container.name}") String containerName
    ) {
        CosmosClient cosmosClient = new CosmosClientBuilder()
                .endpoint(cosmosURI)
                .key(cosmosKey)
                .buildClient();
        return cosmosClient.getDatabase(databaseName).getContainer(containerName);
    }
}
