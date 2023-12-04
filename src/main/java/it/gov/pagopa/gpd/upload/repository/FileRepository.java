package it.gov.pagopa.gpd.upload.repository;

import io.micronaut.http.multipart.CompletedFileUpload;

import java.io.IOException;
import java.net.URL;

public interface FileRepository {

    boolean doesObjectExists(String key);

    void delete(String key);

    URL findURLbyKey(String key);

    String upload(String directory, CompletedFileUpload file) throws IOException;
}
