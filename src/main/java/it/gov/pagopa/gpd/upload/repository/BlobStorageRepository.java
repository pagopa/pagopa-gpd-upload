package it.gov.pagopa.gpd.upload.repository;

import com.azure.core.util.BinaryData;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;

import java.io.IOException;
import java.io.InputStream;

public interface BlobStorageRepository {
    String upload(String container, String directory, InputStream inputStream, ServiceType serviceType) throws IOException;
    BinaryData downloadContent(String broker, String uploadKey, String blobPath, ServiceType serviceType);
}
