package it.gov.pagopa.gpd.upload.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.multipart.CompletedFileUpload;
import it.gov.pagopa.gpd.upload.exception.AppError;
import it.gov.pagopa.gpd.upload.exception.AppException;
import it.gov.pagopa.gpd.upload.model.pd.PaymentPositionModel;
import it.gov.pagopa.gpd.upload.model.pd.PaymentPositionsModel;
import it.gov.pagopa.gpd.upload.repository.BlobStorageRepository;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.*;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Context
@Slf4j
public class BlobService {
    @Value("${zip.content.size}")
    private int zipMaxSize; // Max size of zip file content
    @Value("${zip.entries}")
    private int zipMaxEntries; // Maximum number of entries allowed in the zip file
    private static final List<String> ALLOWABLE_EXTENSIONS = List.of("json");
    private static final List<String> VALID_UPLOAD_EXTENSION = List.of("zip"); ;
    private ObjectMapper objectMapper;
    private BlobStorageRepository blobStorageRepository;
    private StatusService statusService;
    private Validator validator;

    @Inject
    public BlobService(BlobStorageRepository blobStorageRepository, StatusService statusService, Validator validator) {
        this.blobStorageRepository = blobStorageRepository;
        this.statusService = statusService;
        this.validator = validator;
    }

    @PostConstruct
    public void init() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    public String upload(String broker, String organizationFiscalCode, CompletedFileUpload fileUpload) {
        File file = unzip(fileUpload);
        log.debug("File with name " + file.getName() + " has been unzipped");
        PaymentPositionsModel paymentPositionsModel = null;
        try {
            paymentPositionsModel = objectMapper.readValue(new FileInputStream(file), PaymentPositionsModel.class);
            if(!isValid(file.getName(), paymentPositionsModel)) {
                log.error("Debt Positions validation failed for file " + file.getName());
                throw new AppException(HttpStatus.BAD_REQUEST, "INVALID DEBT POSITIONS", "The format of the debt positions in the uploaded file is invalid.");
            }
            String fileId = null;
            fileId = blobStorageRepository.upload(broker, organizationFiscalCode, file);

            statusService.createUploadStatus(organizationFiscalCode, fileId, paymentPositionsModel);

            return fileId;
        } catch (IOException e) {
            throw new AppException(AppError.INTERNAL_ERROR);
        }
    }

    private boolean isValid(String id, PaymentPositionsModel paymentPositionsModel) throws IOException {
        log.debug("Starting validation for object related to" + id);
        Set<ConstraintViolation<PaymentPositionsModel>> constraintViolations;

        for(PaymentPositionModel paymentPositionModel : paymentPositionsModel.getPaymentPositions()) {
            constraintViolations = validator.validate(paymentPositionsModel);
            if(!constraintViolations.isEmpty()) {
                log.error("Validation error for object related to " + id + ": " + paymentPositionModel);
                for(ConstraintViolation<PaymentPositionsModel> cv : constraintViolations) {
                    log.error("Invalid value: " + cv.getInvalidValue());
                    log.error("Invalid value message: " + cv.getMessage());
                    log.error("Invalid value descriptor: " + cv.getConstraintDescriptor());
                }
                return false;
            }
        }
        return true;
    }

    private File unzip(CompletedFileUpload file) {
        String destinationDirectory = "./";
        int zipFiles = 0;
        int zipSize = 0;
        File outputFile = null;
        final byte[] buffer = new byte[1024];

        if(!VALID_UPLOAD_EXTENSION.contains(getFileExtension(file.getFilename()))) {
            log.error("The file " + file.getFilename() + " with extension " + getFileExtension(file.getFilename()) + " is not a zip file ");
            throw new AppException(HttpStatus.BAD_REQUEST, "NOT A ZIP FILE", "Only zip file can be uploaded.");
        }

        try {
            ZipInputStream zis = new ZipInputStream(file.getInputStream());

            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                zipFiles++;
                if (zipFiles > zipMaxEntries) {
                    zis.closeEntry();
                    zis.close();
                    log.error("Zip content has too many entries");
                    throw new AppException(HttpStatus.BAD_REQUEST, "INVALID FILE", "Zip content has too many entries (check for hidden files)");
                }

                if (!entry.isDirectory()) {
                    String fileName = entry.getName();

                    // Validate file extension against whitelist
                    if (!ALLOWABLE_EXTENSIONS.contains(getFileExtension(fileName))) {
                        continue;
                    }

                    // Sanitize file name to remove potentially malicious characters
                    String sanitizedFileName = sanitizeFileName(fileName);
                    String sanitizedOutputPath = destinationDirectory + File.separator + sanitizedFileName;

                    // Disable auto-execution for extracted files
                    outputFile = new File(sanitizedOutputPath);
                    boolean executableOff = outputFile.setExecutable(false);
                    if(!executableOff) {
                        log.error("The underlying file system does not implement an execution permission and the operation failed.");
                    }

                    final FileOutputStream fos = new FileOutputStream(outputFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        zipSize += len;
                        if(zipSize > zipMaxSize) {
                            zis.closeEntry();
                            zis.close();
                            fos.close();
                            log.error("Zip content too large");
                            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID FILE", "Zip content too large");
                        }

                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                entry = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();

            return outputFile;
        } catch (IOException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL SERVER ERROR", "Internal server error", e);
        }
    }

    private static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1) {
            return "";
        }

        return fileName.substring(dotIndex + 1);
    }

    private static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        // Replace invalid characters with underscores
        fileName = fileName.replaceAll("[^\\w._-]", "_");

        // Normalize file name to lower case
        fileName = fileName.toLowerCase();

        return fileName;
    }
}
