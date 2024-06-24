package it.gov.pagopa.gpd.upload.repository;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.specialized.BlockBlobClient;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpStatus;
import it.gov.pagopa.gpd.upload.exception.AppException;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static io.micronaut.http.HttpStatus.NOT_FOUND;

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
    public String upload(String broker, String fiscalCode, InputStream inputStream) throws FileNotFoundException {
        blobServiceClient.createBlobContainerIfNotExists(broker);
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(broker + "/" + fiscalCode + "/" + INPUT_DIRECTORY);
        String key = this.createRandomName(broker + "_" + fiscalCode);

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
            log.info(String.format("Asynchronous upload completed for blob %s", blobName));
        }).exceptionally(ex -> {
            log.error(String.format("[Error][BlobStorageRepository@upload] Exception while uploading file asynchronously: %s", ex.getMessage()));
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
        BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(broker);
        String blobName = uploadKey.concat(".json");

        if(!blobContainerClient.exists())
            log.error(String.format("[Error][BlobStorageRepository@getReport] Container doesn't exist: %s, for upload: %s", broker, uploadKey));

        BlobClient blobClient = blobContainerClient.getBlobClient("/" + fiscalCode + "/" + OUTPUT_DIRECTORY + "/report" + blobName);

        if(Boolean.FALSE.equals(blobClient.exists())) {
            log.error(String.format("[Error][BlobStorageRepository@getReport] Blob doesn't exist: %s", uploadKey));
            throw new AppException(NOT_FOUND, "REPORT NOT FOUND", "The Report for the given upload " + uploadKey + " does not exist");
        }

        return blobClient.downloadContent();
    }
}
