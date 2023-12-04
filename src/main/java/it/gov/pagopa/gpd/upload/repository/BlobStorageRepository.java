package it.gov.pagopa.gpd.upload.repository;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.specialized.BlockBlobClient;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.multipart.CompletedFileUpload;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;

@Context
@Singleton
public class BlobStorageRepository implements FileRepository {

    @Value("${blob.sas.url}")
    private String blobURL;

    @Value("${blob.sas.token}")
    private String blobToken;

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
    public String upload(String directory, CompletedFileUpload file) throws FileNotFoundException {
        String key = this.createRandomName(directory);
        BlobContainerClient container = blobServiceClient.getBlobContainerClient("input/inbox");
        BlockBlobClient blockBlobClient = container
                                            .getBlobClient(key)
                                            .getBlockBlobClient();

        try {
            this.uploadFileBlocksAsBlockBlob(blockBlobClient, file);
            return key;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String createRandomName(String namePrefix) {
        return namePrefix + UUID.randomUUID().toString().replace("-", "");
    }

    private void uploadFileBlocksAsBlockBlob(BlockBlobClient blockBlob, CompletedFileUpload file) throws IOException {
        InputStream inputStream = file.getInputStream();
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
                blockId = String.format("%05d", blockNum);
                blockIdEncoded = Base64.getEncoder().encodeToString(blockId.getBytes());
                blockBlob.stageBlock(blockIdEncoded, byteInputStream, blockSize);
                blockList.add(blockIdEncoded);
                blockNum++;
                bytes = inputStream.readNBytes(blockSize);
            }
            blockId = String.format("%05d", blockNum);
            blockIdEncoded = Base64.getEncoder().encodeToString(blockId.getBytes());
            byteInputStream = new ByteArrayInputStream(bytes);
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
