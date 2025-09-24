package it.gov.pagopa.gpd.upload.repository;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.specialized.BlockBlobClient;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpStatus;
import it.gov.pagopa.gpd.upload.exception.AppException;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static io.micronaut.http.HttpStatus.NOT_FOUND;
import static it.gov.pagopa.gpd.upload.utils.Constants.SERVICE_TYPE_METADATA;

@Context
@Singleton
@Slf4j
public class BlobStorageRepository implements FileRepository {

    @Value("${blob.sas.connection}")
    private String connectionString;

    private static final String INPUT_DIRECTORY = "input";
    private static final String OUTPUT_DIRECTORY = "output";

    private BlobServiceClient blobServiceClient;

    @PostConstruct
    public void init() {
        blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }

    @Override
    public String upload(String broker, String fiscalCode, InputStream inputStream, ServiceType serviceType) throws FileNotFoundException {
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

    public BinaryData downloadOutput(String broker, String fiscalCode, String uploadKey) {
        BlobClient blobClient = getBlobClient(broker, fiscalCode, uploadKey, OUTPUT_DIRECTORY, "getReport");

        if(Boolean.FALSE.equals(blobClient.exists())) {
            log.error(String.format("[Error][BlobStorageRepository@getReport] Blob doesn't exist: %s", uploadKey));
            throw new AppException(NOT_FOUND, "REPORT NOT FOUND", "The Report for the given upload " + uploadKey + " does not exist");
        }

        return blobClient.downloadContent();
    }

    public BinaryData downloadInput(String broker, String fiscalCode, String uploadKey) {
        BlobClient blobClient = getBlobClient(broker, fiscalCode, uploadKey, INPUT_DIRECTORY, "getUploadBlob");

        if(Boolean.FALSE.equals(blobClient.exists())) {
            log.error(String.format("[Error][BlobStorageRepository@getUploadBlob] Blob doesn't exist: %s", uploadKey));
            throw new AppException(NOT_FOUND, "UPLOAD NOT FOUND", "The Blob-Upload for the given upload " + uploadKey + " does not exist");
        }

        return blobClient.downloadContent();
    }


    public ServiceType getBlobServiceTypeMetadata(String broker, String fiscalCode, String uploadKey){
        BlobClient blobClient = getBlobClient(broker, fiscalCode, uploadKey, OUTPUT_DIRECTORY, "getBlobServiceTypeMetadata");
        BlobProperties properties = blobClient.getProperties();
        return ServiceType.valueOf(properties.getMetadata().getOrDefault(SERVICE_TYPE_METADATA, ServiceType.GPD.name()));
    }

    private BlobClient getBlobClient(String broker, String fiscalCode, String uploadKey, String directory, String methodName){
        BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(broker);
        String blobName = uploadKey.concat(".json");

        if(!blobContainerClient.exists())
            log.error(String.format("[Error][BlobStorageRepository@%s] Container doesn't exist: %s, for upload: %s", methodName, broker, uploadKey));

        return blobContainerClient.getBlobClient("/" + fiscalCode + "/" + directory + "/" + blobName);
    }
}
