package it.gov.pagopa.gpd.upload.service;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Primary;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import it.gov.pagopa.gpd.upload.model.UploadOperation;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import it.gov.pagopa.gpd.upload.repository.BlobStorageRepository;
import it.gov.pagopa.gpd.upload.repository.StatusRepository;
import it.gov.pagopa.gpd.upload.utils.FileUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.FileNotFoundException;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@MicronautTest
public class BlobServiceTest {
    private static final String FISCAL_CODE = "fiscal-code";
    private static final String BROKER_CODE = "broker-code";
    @Inject
    BlobService blobService;

    @Test
    void upsert_OK() throws IOException {
        String uploadKey = blobService.upsert(BROKER_CODE, FISCAL_CODE, UploadOperation.CREATE, FileUtils.getUpsertFile(), ServiceType.GPD);

        Assertions.assertEquals(FISCAL_CODE, uploadKey);
    }

    @Test
    void delete_OK() throws IOException {
        String uploadKey = blobService.delete(BROKER_CODE, FISCAL_CODE, UploadOperation.CREATE, FileUtils.getDeleteFile(), ServiceType.GPD);

        Assertions.assertEquals(FISCAL_CODE, uploadKey);
    }

    // real repositories are out of scope for this test, @PostConstruct init routine requires connection-string
    @Bean
    @Primary
    public static BlobStorageRepository blobStorageRepository() throws FileNotFoundException {
        BlobStorageRepository blobStorageRepository = Mockito.mock(BlobStorageRepository.class);
        Mockito.when(blobStorageRepository.upload(anyString(), anyString(), any(), any())).thenReturn(FISCAL_CODE);
        return blobStorageRepository;
    }
    @Bean
    @Primary
    public static StatusRepository statusRepository() {
        return Mockito.mock(StatusRepository.class);
    }
}
