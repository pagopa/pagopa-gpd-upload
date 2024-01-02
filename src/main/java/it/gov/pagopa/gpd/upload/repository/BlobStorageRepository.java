package it.gov.pagopa.gpd.upload.repository;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobContainerItem;
import com.azure.storage.blob.specialized.BlockBlobClient;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpStatus;
import it.gov.pagopa.gpd.upload.exception.AppException;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;

@Context
@Singleton
@Slf4j
public class BlobStorageRepository implements FileRepository {

    @Value("${blob.sas.url}")
    private String blobURL;

    @Value("${blob.sas.token}")
    private String blobToken;

    @Value("${blob.container.input}")
    private String inputContainer;

    private BlobServiceClient blobServiceClient;

    @PostConstruct
    public void init() {
        blobServiceClient = new BlobServiceClientBuilder()
                .endpoint(blobURL)
                .sasToken(blobToken)
                .buildClient();
    }

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
    public String upload(String fiscalCode, File file) throws FileNotFoundException {
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(inputContainer + "/" + fiscalCode);
        String key = this.createRandomName(fiscalCode);
        BlobClient blobClient = container.getBlobClient(key);
        // retry in case of pseudo random collision
        while (blobClient.exists()) {
            key = this.createRandomName(fiscalCode);
            blobClient = container.getBlobClient(key);
        }

        BlockBlobClient blockBlobClient = blobClient.getBlockBlobClient();

        try {
            this.uploadFileBlocksAsBlockBlob(blockBlobClient, file);
            return key;
        } catch (IOException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Internal server error", e);
        }
    }

    private String createRandomName(String namePrefix) {
        return namePrefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private void uploadFileBlocksAsBlockBlob(BlockBlobClient blockBlob, File file) throws IOException {
        InputStream inputStream = new FileInputStream(file);
        ByteArrayInputStream byteInputStream = null;
        byte[] bytes = null;
        int blockSize = 1024 * 1024;
        try {
            // Split the file into 1 MB blocks (block size deliberately kept small for the demo) and upload all the blocks
            int blockNum = 0;
            String blockId = null;
            String blockIdEncoded = null;
            ArrayList<String> blockList = new ArrayList<String>();
            bytes = inputStream.readNBytes(blockSize);
            while (bytes.length == blockSize) {
                byteInputStream = new ByteArrayInputStream(bytes);
                blockId = String.format("%05d", blockNum); // 5-digit number
                blockIdEncoded = Base64.getEncoder().encodeToString(blockId.getBytes());
                blockBlob.stageBlock(blockIdEncoded, byteInputStream, blockSize);
                blockList.add(blockIdEncoded);
                blockNum++;
                bytes = inputStream.readNBytes(blockSize);
            }
            blockId = String.format("%05d", blockNum);
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
    }
}
