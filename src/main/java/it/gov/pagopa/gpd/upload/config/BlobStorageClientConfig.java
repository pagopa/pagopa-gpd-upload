package it.gov.pagopa.gpd.upload.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;

@Factory
class BlobStorageClientConfig {

    @Bean
    BlobServiceClient blobServiceClientBean(
            @Value("${blob.sas.connection}") String connectionString
    ) {
        return new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }
}
