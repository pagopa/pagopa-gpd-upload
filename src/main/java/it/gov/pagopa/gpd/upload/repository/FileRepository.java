package it.gov.pagopa.gpd.upload.repository;

import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;

import java.io.IOException;
import java.io.InputStream;

public interface FileRepository {
    String upload(String container, String directory, InputStream inputStream, ServiceType serviceType) throws IOException;
}
