package it.gov.pagopa.gpd.upload.service;

import io.micronaut.http.multipart.CompletedFileUpload;
import it.gov.pagopa.gpd.upload.repository.BlobStorageRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Singleton
public class FileUploadService {

    @Inject
    BlobStorageRepository blobStorageRepository;

    public String upload(CompletedFileUpload file) throws IOException {
        blobStorageRepository.upload(file);
        return null;
    }
}
