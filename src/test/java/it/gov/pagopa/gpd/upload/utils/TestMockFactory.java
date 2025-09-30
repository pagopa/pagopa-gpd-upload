package it.gov.pagopa.gpd.upload.utils;

import com.azure.cosmos.CosmosContainer;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import it.gov.pagopa.gpd.upload.repository.BlobStorageRepository;
import it.gov.pagopa.gpd.upload.repository.StatusRepository;
import it.gov.pagopa.gpd.upload.service.BlobService;
import it.gov.pagopa.gpd.upload.service.RecoveryService;
import it.gov.pagopa.gpd.upload.service.StatusService;
import jakarta.inject.Singleton;

import static org.mockito.Mockito.mock;

@Factory
class TestMockFactory {

    @Singleton
    @Replaces(StatusService.class)
    StatusService statusServiceMock() {
        return mock(StatusService.class);
    }

    @Singleton
    @Replaces(BlobService.class)
    BlobService blobServiceMock() {
        return mock(BlobService.class);
    }

    @Singleton
    @Replaces(RecoveryService.class)
    public RecoveryService recoveryService() {
        return mock(RecoveryService.class);
    }

    @Singleton
    @Replaces(StatusRepository.class)
    StatusRepository statusRepositoryMock() {
        return mock(StatusRepository.class);
    }

    @Singleton
    @Replaces(BlobStorageRepository.class)
    BlobStorageRepository blobStorageRepositoryMock() {
        return mock(BlobStorageRepository.class);
    }

    // Mock del CosmosContainer per evitare che Micronaut inizi il client reale durante il wiring
    @Singleton
    @Replaces(CosmosContainer.class)
    CosmosContainer cosmosContainerMock() {
        return mock(CosmosContainer.class);
    }
}