package it.gov.pagopa.gpd.upload.repository;

import java.io.File;
import java.io.IOException;

public interface FileRepository {
    String upload(String container, String directory, File file) throws IOException;
}
