package it.gov.pagopa.gpd.upload.utils;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import it.gov.pagopa.gpd.upload.repository.BlobStorageRepository;
import it.gov.pagopa.gpd.upload.repository.StatusRepository;
import it.gov.pagopa.gpd.upload.service.BlobService;
import it.gov.pagopa.gpd.upload.service.SupportService;
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
    @Replaces(SupportService.class)
    public SupportService recoveryService() {
        return mock(SupportService.class);
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

}