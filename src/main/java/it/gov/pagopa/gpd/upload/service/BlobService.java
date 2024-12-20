package it.gov.pagopa.gpd.upload.service;

import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.multipart.CompletedFileUpload;
import it.gov.pagopa.gpd.upload.exception.AppException;
import it.gov.pagopa.gpd.upload.model.UploadOperation;
import it.gov.pagopa.gpd.upload.model.UploadInput;
import it.gov.pagopa.gpd.upload.model.UploadReport;
import it.gov.pagopa.gpd.upload.model.pd.MultipleIUPDModel;
import it.gov.pagopa.gpd.upload.model.pd.PaymentPositionsModel;
import it.gov.pagopa.gpd.upload.repository.BlobStorageRepository;
import it.gov.pagopa.gpd.upload.utils.GPDValidator;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.*;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
    private static final List<String> VALID_UPLOAD_EXTENSION = List.of("zip");
    private static final String DESTINATION_DIRECTORY = "upload-directory";
    private ObjectMapper objectMapper;
    private final BlobStorageRepository blobStorageRepository;
    private final StatusService statusService;
    private final GPDValidator<PaymentPositionsModel> paymentPositionsValidator;
    private final GPDValidator<MultipleIUPDModel> multipleIUPDValidator;

    @Inject
    public BlobService(BlobStorageRepository blobStorageRepository, StatusService statusService,
                       GPDValidator<PaymentPositionsModel> paymentPositionsValidator, GPDValidator<MultipleIUPDModel> multipleIUPDValidator) {
        this.blobStorageRepository = blobStorageRepository;
        this.statusService = statusService;
        this.paymentPositionsValidator = paymentPositionsValidator;
        this.multipleIUPDValidator = multipleIUPDValidator;
    }

    @PostConstruct
    public void init() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        File directory = new File(DESTINATION_DIRECTORY);
        if(!directory.exists())
            directory.mkdir();
    }

    public String upsert(String broker, String organizationFiscalCode, UploadOperation uploadOperation, CompletedFileUpload fileUpload) {
        File file = this.unzip(fileUpload);
        if (null == file)
            throw new AppException(HttpStatus.BAD_REQUEST, "EMPTY FILE", "The JSON file is missing");
        log.debug("File with name {} has been unzipped, upload operation: {}", file.getName(), uploadOperation);
        try {
            PaymentPositionsModel paymentPositionsModel = objectMapper.readValue(new FileInputStream(file), PaymentPositionsModel.class);

            if(!file.delete()) {
                log.error("[Error][BlobService@upsert] The file {} was not deleted", file.getName());
            }

            if (!paymentPositionsValidator.isValid(paymentPositionsModel)) {
                log.error("[Error][BlobService@upload] Debt-Positions validation failed for upload from broker {} and organization {}", broker, organizationFiscalCode);
                throw new AppException(HttpStatus.BAD_REQUEST, "INVALID DEBT POSITIONS", "The format of the debt positions in the uploaded file is invalid.");
            }

            UploadInput uploadInput = UploadInput.builder()
                    .uploadOperation(uploadOperation)
                    .paymentPositions(paymentPositionsModel.getPaymentPositions())
                    .build();

            // return upload key
            return upload(uploadInput, broker, organizationFiscalCode, paymentPositionsModel.getPaymentPositions().size());
        } catch (IOException e) {
            log.error("[Error][BlobService@upload] " + e.getMessage());
            if(!file.delete())
                log.error("[Error][BlobService@upsert] The file {} was not deleted", file.getName());

            if(e instanceof JsonMappingException)
                throw new AppException(HttpStatus.BAD_REQUEST, "INVALID JSON", "Given JSON is invalid for required API payload: " + e.getMessage());

            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL SERVER ERROR", "Internal server error", e.getCause());
        }
    }

    public String delete(String broker, String organizationFiscalCode, UploadOperation uploadOperation, CompletedFileUpload fileUpload) {
        File file = this.unzip(fileUpload);
        if (null == file)
            throw new AppException(HttpStatus.BAD_REQUEST, "EMPTY FILE", "The JSON file is missing");
        log.debug("File with name " + file.getName() + " has been unzipped");
        try {
            MultipleIUPDModel multipleIUPDModel = objectMapper.readValue(new FileInputStream(file), MultipleIUPDModel.class);

            if(!file.delete()) {
                log.error(String.format("[Error][BlobService@delete] The file %s was not deleted", file.getName()));
            }

            if (!multipleIUPDValidator.isValid(multipleIUPDModel)) {
                log.error(String.format("[Error][BlobService@delete] Debt-Positions validation failed for upload from broker %s and organization %s",
                        broker, organizationFiscalCode));
                throw new AppException(HttpStatus.BAD_REQUEST, "INVALID DEBT POSITIONS", "The format of the debt positions in the uploaded file is invalid.");
            }

            UploadInput uploadInput = UploadInput.builder()
                    .uploadOperation(uploadOperation)
                    .paymentPositionIUPDs(multipleIUPDModel.getPaymentPositionIUPDs())
                    .build();

            // return upload key
            return upload(uploadInput, broker, organizationFiscalCode, multipleIUPDModel.getPaymentPositionIUPDs().size());
        } catch (IOException e) {
            log.error("[Error][BlobService@upload] " + e.getMessage());
            if(!file.delete())
                log.error(String.format("[Error][BlobService@upsert] The file %s was not deleted", file.getName()));

            if(e instanceof JsonMappingException)
                throw new AppException(HttpStatus.BAD_REQUEST, "INVALID JSON", "Given JSON is invalid for required API payload: " + e.getMessage());

            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An error occurred during delete operation", e.getCause());
        }
    }

    public UploadInput getUploadInput(String broker, String fiscalCode, String uploadId) {
        BinaryData binaryDataReport = blobStorageRepository.downloadInput(broker, fiscalCode, uploadId);
        try {
            return objectMapper.readValue(binaryDataReport.toString(), UploadInput.class);
        } catch (JsonProcessingException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An error occurred during upload-input deserialization", e.getCause());
        }
    }

    public UploadReport getReport(String broker, String fiscalCode, String uploadKey) {
        BinaryData binaryDataReport = blobStorageRepository.downloadOutput(broker, fiscalCode, uploadKey);
        try {
            return objectMapper.readValue(binaryDataReport.toString(), UploadReport.class);
        } catch (JsonProcessingException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An error occurred during report deserialization", e.getCause());
        }
    }

    public String upload(UploadInput uploadInput, String broker, String organizationFiscalCode, int totalItem) {
        try {
            log.debug(String.format("Upload operation %s was launched for broker %s and organization fiscal code %s",
                    uploadInput.getUploadOperation(), broker, organizationFiscalCode));

            // from UploadInput Object to ByteArrayInputStream
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(objectMapper.writeValueAsBytes(uploadInput));

            // upload blob
            String fileId = blobStorageRepository.upload(broker, organizationFiscalCode, inputStream);
            statusService.createUploadStatus(organizationFiscalCode, broker, fileId, totalItem);

            return fileId;
        } catch (IOException e) {
            log.error("[Error][BlobService@upload] " + e.getMessage());
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL SERVER ERROR", "Internal server error", e.getCause());
        }
    }

    private File unzip(CompletedFileUpload file) {
        int zipFiles = 0;
        int zipSize = 0;
        File outputFile = null;
        final byte[] buffer = new byte[1024];

        if(!VALID_UPLOAD_EXTENSION.contains(getFileExtension(file.getFilename()))) {
            log.error("[Error][BlobService@unzip] The file " + file.getFilename() + " with extension " + getFileExtension(file.getFilename()) + " is not a zip file ");
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
                    log.error("[Error][BlobService@unzip] Zip content has too many entries");
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
                    String sanitizedOutputPath = DESTINATION_DIRECTORY + File.separator + sanitizedFileName;

                    // Disable auto-execution for extracted files
                    outputFile = new File(sanitizedOutputPath);

                    if (!outputFile.toPath().normalize().startsWith(DESTINATION_DIRECTORY)) {
                        log.error("[Error][BlobService@unzip] Bad zip entry: the normalized file path does not start with the destination directory");
                        throw new AppException(HttpStatus.BAD_REQUEST, "INVALID FILE", "Bad zip entry");
                    }

                    boolean executableOff = outputFile.setExecutable(false);
                    if(!executableOff) {
                        log.error("[Error][BlobService@unzip] The underlying file system does not implement an execution permission and the operation failed.");
                    }

                    final FileOutputStream fos = new FileOutputStream(outputFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        zipSize += len;
                        if(zipSize > zipMaxSize) {
                            zis.closeEntry();
                            zis.close();
                            fos.close();
                            log.error("[Error][BlobService@unzip] Zip content too large");
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
            log.error("[Error][BlobService@unzip] " + e.getMessage());
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL SERVER ERROR", "Internal server error", e.getCause());
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

        // Match a single character not present in the list below [^\w._-] and replace invalid characters with underscores
        fileName = fileName.replaceAll("[^\\w._-]", "_");

        // Normalize file name to lower case
        fileName = fileName.toLowerCase();
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0,8);
        fileName = fileName.replace(".", uuid + ".");

        return fileName;
    }
}
