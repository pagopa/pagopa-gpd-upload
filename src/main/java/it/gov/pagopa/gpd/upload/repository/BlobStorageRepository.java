package it.gov.pagopa.gpd.upload.repository;

import io.micronaut.http.multipart.CompletedFileUpload;
import jakarta.inject.Singleton;

import java.net.URL;

@Singleton
public class BlobStorageRepository implements FileRepository {
    @Override
    public boolean doesObjectExists(String key) {
        return false;
    }

    @Override
    public void delete(String key) {

    }

    @Override
    public URL findURLbyKey(String key) {
        return null;
    }

    @Override
    public String upload(CompletedFileUpload file) {
        return null;
    }
}
