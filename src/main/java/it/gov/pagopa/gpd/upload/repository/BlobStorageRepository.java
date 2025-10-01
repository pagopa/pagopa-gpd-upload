package it.gov.pagopa.gpd.upload.repository;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.specialized.BlockBlobClient;
import io.micronaut.context.annotation.Context;
import io.micronaut.http.HttpStatus;
import it.gov.pagopa.gpd.upload.exception.AppError;
import it.gov.pagopa.gpd.upload.exception.AppException;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static it.gov.pagopa.gpd.upload.utils.Constants.INPUT_DIRECTORY;
import static it.gov.pagopa.gpd.upload.utils.Constants.SERVICE_TYPE_METADATA;

@Context
@Singleton
@Slf4j
public class BlobStorageRepository {
    private final BlobServiceClient blobServiceClient;
    @Inject
    public BlobStorageRepository(BlobServiceClient blobServiceClient) {
        this.blobServiceClient = blobServiceClient;
    }

    public String upload(String broker, String fiscalCode, InputStream inputStream, ServiceType serviceType) {
        blobServiceClient.createBlobContainerIfNotExists(broker);
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(broker + "/" + fiscalCode + "/" + INPUT_DIRECTORY);
        String key = this.createRandomName(broker + "_" + fiscalCode);
        Map<String, String> metadata = Map.of(SERVICE_TYPE_METADATA, serviceType.name());

        BlobClient blobClient = container.getBlobClient(key + ".json");
        // retry in case of pseudo random collision
        while (blobClient.exists()) {
            key = this.createRandomName(fiscalCode);
            blobClient = container.getBlobClient(key);
        }

        BlockBlobClient blockBlobClient = blobClient.getBlockBlobClient();

        CompletableFuture<String> uploadFuture = uploadFileAsync(blockBlobClient, inputStream);

        uploadFuture.thenAccept(blobName -> {
            // Handle the result asynchronously
            String[] blobNameSplit = blobName.split("/");
            String fileName = blobNameSplit[blobNameSplit.length - 1];
            container.getBlobClient(fileName).setMetadata(metadata);
            log.debug("Asynchronous upload completed for blob {}", blobName);
        }).exceptionally(ex -> {
            log.error("[Error][BlobStorageRepository@upload] Exception while uploading file asynchronously: {}", ex.getMessage());
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Error uploading file asynchronously", ex);
        });

        return key;
    }

    private CompletableFuture<String> uploadFileAsync(BlockBlobClient blockBlobClient, InputStream inputStream) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return this.uploadFileBlocksAsBlockBlob(blockBlobClient, inputStream);
            } catch (IOException e) {
                throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Error uploading file asynchronously", e);
            }
        });
    }

    private String createRandomName(String namePrefix) {
        return namePrefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String uploadFileBlocksAsBlockBlob(BlockBlobClient blockBlob, InputStream inputStream) throws IOException {
        ByteArrayInputStream byteInputStream = null;
        int blockSize = 1024 * 1024;

        try {
            // Split the file into 1 MB blocks (block size deliberately kept small for the demo) and upload all the blocks
            int blockNum = 0;
            String blockId = null;
            String blockIdEncoded = null;
            ArrayList<String> blockList = new ArrayList<>();
            byte[] bytes = inputStream.readNBytes(blockSize);
            while (bytes.length == blockSize) {
                byteInputStream = new ByteArrayInputStream(bytes);
                blockId = String.format("%05d", blockNum); // 5-digit number
                blockIdEncoded = Base64.getEncoder().encodeToString(blockId.getBytes());
                blockBlob.stageBlock(blockIdEncoded, byteInputStream, blockSize);
                blockList.add(blockIdEncoded);
                blockNum++;
                bytes = inputStream.readNBytes(blockSize);
            }
            blockId = String.format("%05d", blockNum); // 5-digit number
            blockIdEncoded = Base64.getEncoder().encodeToString(blockId.getBytes());
            byteInputStream = new ByteArrayInputStream(bytes); // add last block based on remaining bytes
            blockBlob.stageBlock(blockIdEncoded, byteInputStream, bytes.length);
            blockList.add(blockIdEncoded);

            // Commit the blocks
            blockBlob.commitBlockList(blockList);
        } finally {
            // Close the file output stream writer
            if (inputStream != null) {
                inputStream.close();
            }
            if (byteInputStream != null) {
                byteInputStream.close();
            }
        }
        return blockBlob.getBlobName();
    }

    public BinaryData downloadContent(String broker, String uploadKey, String blobPath, ServiceType serviceType) {
        BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(broker);

        if (!blobContainerClient.exists()) {
            log.error(String.format("[Error][BlobStorageRepository@downloadContent] Container %s doesn't exist for upload %s in path %s", broker, uploadKey, blobPath));
            throw new AppException(AppError.BLOB_NOT_FOUND, uploadKey, serviceType);
        }

        BlobClient blobClient = blobContainerClient.getBlobClient(blobPath);

        if (Boolean.FALSE.equals(blobClient.exists())) {
            log.error(String.format("[Error][BlobStorageRepository@downloadContent] Blob %s doesn't exist in path %s", uploadKey, blobPath));
            throw new AppException(AppError.BLOB_NOT_FOUND, uploadKey, serviceType);
        }

        BlobProperties properties = blobClient.getProperties();
        ServiceType serviceTypeMetadata = ServiceType.valueOf(properties.getMetadata().getOrDefault(SERVICE_TYPE_METADATA, ServiceType.GPD.name()));

        if (serviceTypeMetadata != serviceType) {
            log.error(String.format("[Error][BlobStorageRepository@downloadContent] Blob %s doesn't exist for %s in path %s, it was uploaded for %s", uploadKey, serviceType, serviceTypeMetadata, blobPath));
            throw new AppException(AppError.BLOB_NOT_FOUND, uploadKey, serviceType);
        }

        return blobClient.downloadContent();
    }
}
