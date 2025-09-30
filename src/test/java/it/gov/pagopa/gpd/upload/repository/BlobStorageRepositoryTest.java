package it.gov.pagopa.gpd.upload.repository;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.specialized.BlockBlobClient;
import it.gov.pagopa.gpd.upload.exception.AppException;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static it.gov.pagopa.gpd.upload.utils.Constants.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BlobStorageRepositoryTest {

    private static final String BROKER_ID = "brokerId";
    private static final String FISCAL_CODE = "fiscalCode";
    private static final String UPLOAD_CONTAINER_PATH = BROKER_ID + "/" + FISCAL_CODE + "/" + INPUT_DIRECTORY;
    private static final String DOWNLOAD_CONTAINER_PATH = BROKER_ID + "/" + FISCAL_CODE + "/" + OUTPUT_DIRECTORY;
    private static final String BLOB_NAME = "blobName";

    BlobServiceClient blobServiceClientMock = mock(BlobServiceClient.class);
    BlobStorageRepository blobStorageRepository = new BlobStorageRepository(blobServiceClientMock);

    @Test
    void upload_OK() {
        InputStream inputStream = InputStream.nullInputStream();

        BlockBlobClient blockBlobClient = mock(BlockBlobClient.class);
        doNothing().when(blockBlobClient).stageBlock(anyString(), any(), anyLong());
        when(blockBlobClient.commitBlockList(any())).thenReturn(null);
        when(blockBlobClient.getBlobName()).thenReturn(BLOB_NAME);

        BlobClient blobClient = mock(BlobClient.class);
        when(blobClient.exists()).thenReturn(false);
        when(blobClient.getBlockBlobClient()).thenReturn(blockBlobClient);
        doNothing().when(blobClient).setMetadata(Map.of(SERVICE_TYPE_METADATA, ServiceType.GPD.name()));

        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);

        Mockito.when(blobServiceClientMock.createBlobContainerIfNotExists(anyString())).thenReturn(null);
        Mockito.when(blobServiceClientMock.getBlobContainerClient(UPLOAD_CONTAINER_PATH)).thenReturn(blobContainerClient);

        assertDoesNotThrow(() -> blobStorageRepository.upload(BROKER_ID, FISCAL_CODE, inputStream, ServiceType.GPD));

        verify(blobServiceClientMock, times(1)).createBlobContainerIfNotExists(anyString());
        verify(blobServiceClientMock, times(1)).getBlobContainerClient(UPLOAD_CONTAINER_PATH);
        verify(blobContainerClient, atLeast(1)).getBlobClient(anyString());
        verify(blobClient, times(1)).exists();
        verify(blobClient, times(1)).getBlockBlobClient();
    }

    @Test
    void upload_KO_InternalServerError() throws IOException {
        InputStream inputStream = mock(InputStream.class);
        doThrow(new IOException()).when(inputStream).readNBytes(anyInt());

        BlockBlobClient blockBlobClient = mock(BlockBlobClient.class);

        BlobClient blobClient = mock(BlobClient.class);
        when(blobClient.exists()).thenReturn(false);
        when(blobClient.getBlockBlobClient()).thenReturn(blockBlobClient);
        doNothing().when(blobClient).setMetadata(Map.of(SERVICE_TYPE_METADATA, ServiceType.GPD.name()));

        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);

        Mockito.when(blobServiceClientMock.createBlobContainerIfNotExists(anyString())).thenReturn(null);
        Mockito.when(blobServiceClientMock.getBlobContainerClient(UPLOAD_CONTAINER_PATH)).thenReturn(blobContainerClient);

        blobStorageRepository.upload(BROKER_ID, FISCAL_CODE, inputStream, ServiceType.GPD);

        verify(blobServiceClientMock, times(1)).createBlobContainerIfNotExists(anyString());
        verify(blobServiceClientMock, times(1)).getBlobContainerClient(UPLOAD_CONTAINER_PATH);
        verify(blobContainerClient, times(1)).getBlobClient(anyString());
        verify(blobClient, times(1)).exists();
        verify(blobClient, times(1)).getBlockBlobClient();
        verify(blobClient, never()).setMetadata(Map.of(SERVICE_TYPE_METADATA, ServiceType.GPD.name()));
        verify(blockBlobClient, never()).stageBlock(anyString(), any(), anyLong());
        verify(blockBlobClient, never()).commitBlockList(any());
        verify(blockBlobClient, never()).getBlobName();
    }

    @Test
    void downloadContent_OK() {
        BlobProperties blobProperties = mock(BlobProperties.class);
        when(blobProperties.getMetadata()).thenReturn(Map.of(SERVICE_TYPE_METADATA, ServiceType.GPD.name()));

        BlobClient blobClient = mock(BlobClient.class);
        when(blobClient.exists()).thenReturn(true);
        when(blobClient.getProperties()).thenReturn(blobProperties);

        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        when(blobContainerClient.getBlobClient(DOWNLOAD_CONTAINER_PATH)).thenReturn(blobClient);
        when(blobContainerClient.exists()).thenReturn(true);

        Mockito.when(blobServiceClientMock.getBlobContainerClient(BROKER_ID)).thenReturn(blobContainerClient);

        assertDoesNotThrow(() -> blobStorageRepository.downloadContent(BROKER_ID, BLOB_NAME, DOWNLOAD_CONTAINER_PATH, ServiceType.GPD));

        verify(blobServiceClientMock, times(1)).getBlobContainerClient(BROKER_ID);
        verify(blobContainerClient, times(1)).getBlobClient(DOWNLOAD_CONTAINER_PATH);
        verify(blobContainerClient, times(1)).exists();
        verify(blobClient, times(1)).exists();
        verify(blobClient, times(1)).getProperties();
    }

    @Test
    void downloadContent_NoServiceType_OK() {
        BlobProperties blobProperties = mock(BlobProperties.class);
        when(blobProperties.getMetadata()).thenReturn(Map.of());

        BlobClient blobClient = mock(BlobClient.class);
        when(blobClient.exists()).thenReturn(true);
        when(blobClient.getProperties()).thenReturn(blobProperties);

        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        when(blobContainerClient.getBlobClient(DOWNLOAD_CONTAINER_PATH)).thenReturn(blobClient);
        when(blobContainerClient.exists()).thenReturn(true);

        Mockito.when(blobServiceClientMock.getBlobContainerClient(BROKER_ID)).thenReturn(blobContainerClient);

        assertDoesNotThrow(() -> blobStorageRepository.downloadContent(BROKER_ID, BLOB_NAME, DOWNLOAD_CONTAINER_PATH, ServiceType.GPD));

        verify(blobServiceClientMock, times(1)).getBlobContainerClient(BROKER_ID);
        verify(blobContainerClient, times(1)).getBlobClient(DOWNLOAD_CONTAINER_PATH);
        verify(blobContainerClient, times(1)).exists();
        verify(blobClient, times(1)).exists();
        verify(blobClient, times(1)).getProperties();
    }

    @Test
    void downloadContent_NoContainer_KO() {
        BlobClient blobClient = mock(BlobClient.class);
        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        when(blobContainerClient.exists()).thenReturn(false);

        Mockito.when(blobServiceClientMock.getBlobContainerClient(BROKER_ID)).thenReturn(blobContainerClient);

        assertThrows(AppException.class, () -> blobStorageRepository.downloadContent(BROKER_ID, BLOB_NAME, DOWNLOAD_CONTAINER_PATH, ServiceType.GPD));

        verify(blobServiceClientMock, times(1)).getBlobContainerClient(BROKER_ID);
        verify(blobContainerClient, never()).getBlobClient(DOWNLOAD_CONTAINER_PATH);
        verify(blobContainerClient, times(1)).exists();
        verify(blobClient, never()).exists();
        verify(blobClient, never()).getProperties();
    }

    @Test
    void downloadContent_NoBlob_KO() {
        BlobClient blobClient = mock(BlobClient.class);
        when(blobClient.exists()).thenReturn(false);

        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        when(blobContainerClient.getBlobClient(DOWNLOAD_CONTAINER_PATH)).thenReturn(blobClient);
        when(blobContainerClient.exists()).thenReturn(true);

        Mockito.when(blobServiceClientMock.getBlobContainerClient(BROKER_ID)).thenReturn(blobContainerClient);

        assertThrows(AppException.class, () -> blobStorageRepository.downloadContent(BROKER_ID, BLOB_NAME, DOWNLOAD_CONTAINER_PATH, ServiceType.GPD));

        verify(blobServiceClientMock, times(1)).getBlobContainerClient(BROKER_ID);
        verify(blobContainerClient, times(1)).getBlobClient(DOWNLOAD_CONTAINER_PATH);
        verify(blobContainerClient, times(1)).exists();
        verify(blobClient, times(1)).exists();
        verify(blobClient, never()).getProperties();
    }

    @Test
    void downloadContent_WrongServiceType_KO() {
        BlobProperties blobProperties = mock(BlobProperties.class);
        when(blobProperties.getMetadata()).thenReturn(Map.of(SERVICE_TYPE_METADATA, ServiceType.ACA.name()));

        BlobClient blobClient = mock(BlobClient.class);
        when(blobClient.exists()).thenReturn(true);
        when(blobClient.getProperties()).thenReturn(blobProperties);

        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        when(blobContainerClient.getBlobClient(DOWNLOAD_CONTAINER_PATH)).thenReturn(blobClient);
        when(blobContainerClient.exists()).thenReturn(true);

        Mockito.when(blobServiceClientMock.getBlobContainerClient(BROKER_ID)).thenReturn(blobContainerClient);

        assertThrows(AppException.class, () -> blobStorageRepository.downloadContent(BROKER_ID, BLOB_NAME, DOWNLOAD_CONTAINER_PATH, ServiceType.GPD));

        verify(blobServiceClientMock, times(1)).getBlobContainerClient(BROKER_ID);
        verify(blobContainerClient, times(1)).getBlobClient(DOWNLOAD_CONTAINER_PATH);
        verify(blobContainerClient, times(1)).exists();
        verify(blobClient, times(1)).exists();
        verify(blobClient, times(1)).getProperties();
    }

    @Test
    void downloadContent_NoServiceTypeAndAskedACA_KO() {
        BlobProperties blobProperties = mock(BlobProperties.class);
        when(blobProperties.getMetadata()).thenReturn(Map.of());

        BlobClient blobClient = mock(BlobClient.class);
        when(blobClient.exists()).thenReturn(true);
        when(blobClient.getProperties()).thenReturn(blobProperties);

        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        when(blobContainerClient.getBlobClient(DOWNLOAD_CONTAINER_PATH)).thenReturn(blobClient);
        when(blobContainerClient.exists()).thenReturn(true);

        Mockito.when(blobServiceClientMock.getBlobContainerClient(BROKER_ID)).thenReturn(blobContainerClient);

        assertThrows(AppException.class, () -> blobStorageRepository.downloadContent(BROKER_ID, BLOB_NAME, DOWNLOAD_CONTAINER_PATH, ServiceType.ACA));

        verify(blobServiceClientMock, times(1)).getBlobContainerClient(BROKER_ID);
        verify(blobContainerClient, times(1)).getBlobClient(DOWNLOAD_CONTAINER_PATH);
        verify(blobContainerClient, times(1)).exists();
        verify(blobClient, times(1)).exists();
        verify(blobClient, times(1)).getProperties();
    }
}