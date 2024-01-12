package it.gov.pagopa.gpd.upload.service;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Primary;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
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
public class FileUploadServiceTest {
    private static String FISCAL_CODE = "fiscal-code";
    @Inject
    FileUploadService fileUploadService;

    @Test
    void upload_OK() throws IOException {
        String uploadKey = fileUploadService.upload(FISCAL_CODE, FileUtils.getFileUpload());

        Assertions.assertEquals(FISCAL_CODE, uploadKey);
    }

    // real repositories are out of scope for this test, @PostConstruct init routine requires connection-string
    @Bean
    @Primary
    public static BlobStorageRepository blobStorageRepository() throws FileNotFoundException {
        BlobStorageRepository blobStorageRepository = Mockito.mock(BlobStorageRepository.class);
        Mockito.when(blobStorageRepository.upload(anyString(), any())).thenReturn(FISCAL_CODE);
        return blobStorageRepository;
    }
    @Bean
    @Primary
    public static StatusRepository statusRepository() {
        return Mockito.mock(StatusRepository.class);
    }
}
