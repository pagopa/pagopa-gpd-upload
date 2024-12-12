package it.gov.pagopa.gpd.upload.repository;

import java.io.IOException;
import java.io.InputStream;

public interface FileRepository {
    String upload(String container, String directory, InputStream inputStream) throws IOException;
}
