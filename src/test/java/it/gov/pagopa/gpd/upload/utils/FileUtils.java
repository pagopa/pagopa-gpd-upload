package it.gov.pagopa.gpd.upload.utils;

import io.micronaut.http.MediaType;
import io.micronaut.http.multipart.CompletedFileUpload;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileUtils {

    public static CompletedFileUpload getFileUpload() throws IOException {
        GPDCompletedFileUpload f = new GPDCompletedFileUpload("test.zip", MediaType.of("application/zip"), fromJSONtoZip());
        return f;
    }

    public static byte[] fromJSONtoZip() throws IOException {
        String jsonString = PaymentPositionUtils.createPaymentPositionsJSON("77777777777", 1);
        byte[] jsonBytes = jsonString.getBytes();

        // Create a temporary directory to store the ZIP file
        String tempDir = System.getProperty("java.io.tmpdir");
        String zipFilePath = tempDir + File.separator + "test.zip";

        // Create a ZIP file and add the JSON file to it
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(baos);
        try {
            ZipEntry entry = new ZipEntry("data.json");
            zipOutputStream.putNextEntry(entry);
            zipOutputStream.write(jsonBytes);
            zipOutputStream.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] zipContent = baos.toByteArray();

        return zipContent;
    }
}
