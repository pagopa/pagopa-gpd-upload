package it.gov.pagopa.gpd.upload.service;

import io.micronaut.http.multipart.CompletedFileUpload;
import it.gov.pagopa.gpd.upload.repository.BlobStorageRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.IOException;

@Singleton
public class FileUploadService {

    @Inject
    BlobStorageRepository blobStorageRepository;

    public String upload(String organizationFiscalCode, CompletedFileUpload file) throws IOException {
        return blobStorageRepository.upload(organizationFiscalCode, file);
    }
}
