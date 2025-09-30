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
import it.gov.pagopa.gpd.upload.model.UploadInput;
import it.gov.pagopa.gpd.upload.model.UploadOperation;
import it.gov.pagopa.gpd.upload.model.v1.UploadReport;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import it.gov.pagopa.gpd.upload.model.pd.MultipleIUPDModel;
import it.gov.pagopa.gpd.upload.model.pd.PaymentPositionsModel;
import it.gov.pagopa.gpd.upload.model.v2.UploadReportDTO;
import it.gov.pagopa.gpd.upload.repository.BlobStorageRepository;
import it.gov.pagopa.gpd.upload.utils.GPDValidator;
import it.gov.pagopa.gpd.upload.utils.ResponseEntryDTOMapper;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static it.gov.pagopa.gpd.upload.utils.Constants.INPUT_DIRECTORY;
import static it.gov.pagopa.gpd.upload.utils.Constants.OUTPUT_DIRECTORY;

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
    private final ResponseEntryDTOMapper responseEntryDTOMapper;

    @Inject
    public BlobService(BlobStorageRepository blobStorageRepository,
                       StatusService statusService,
                       GPDValidator<PaymentPositionsModel> paymentPositionsValidator,
                       GPDValidator<MultipleIUPDModel> multipleIUPDValidator,
                       ResponseEntryDTOMapper responseEntryDTOMapper) {
        this.blobStorageRepository = blobStorageRepository;
        this.statusService = statusService;
        this.paymentPositionsValidator = paymentPositionsValidator;
        this.multipleIUPDValidator = multipleIUPDValidator;
        this.responseEntryDTOMapper = responseEntryDTOMapper;
    }

    @PostConstruct
    public void init() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        File directory = new File(DESTINATION_DIRECTORY);
        if (!directory.exists())
            directory.mkdir();
    }

    public String upsert(String broker, String organizationFiscalCode, UploadOperation uploadOperation, CompletedFileUpload fileUpload, ServiceType serviceType) {
        InputStream is = this.unzip(fileUpload);

        try {
            PaymentPositionsModel paymentPositionsModel = objectMapper.readValue(is, PaymentPositionsModel.class);

            paymentPositionsValidator.isValidOrElseThrow(paymentPositionsModel);

            UploadInput uploadInput = UploadInput.builder()
                    .uploadOperation(uploadOperation)
                    .paymentPositions(paymentPositionsModel.getPaymentPositions())
                    .build();

            // return upload key
            return upload(uploadInput, broker, organizationFiscalCode, paymentPositionsModel.getPaymentPositions().size(), serviceType);
        } catch (IOException e) {
            log.error("[Error][BlobService@upload] " + e.getMessage());

            if (e instanceof JsonMappingException)
                throw new AppException(HttpStatus.BAD_REQUEST, "INVALID JSON", "Given JSON is invalid for required API payload: " + e.getMessage());

            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL SERVER ERROR", "Internal server error", e.getCause());
        }
    }

    public String delete(String broker, String organizationFiscalCode, UploadOperation uploadOperation, CompletedFileUpload fileUpload, ServiceType serviceType) {
        InputStream is = this.unzip(fileUpload);

        try {
            MultipleIUPDModel multipleIUPDModel = objectMapper.readValue(is, MultipleIUPDModel.class);

            multipleIUPDValidator.isValidOrElseThrow(multipleIUPDModel);

            UploadInput uploadInput = UploadInput.builder()
                    .uploadOperation(uploadOperation)
                    .paymentPositionIUPDs(multipleIUPDModel.getPaymentPositionIUPDs())
                    .build();

            // return upload key
            return upload(uploadInput, broker, organizationFiscalCode, multipleIUPDModel.getPaymentPositionIUPDs().size(), serviceType);
        } catch (IOException e) {
            log.error("[Error][BlobService@upload] " + e.getMessage());

            if (e instanceof JsonMappingException)
                throw new AppException(HttpStatus.BAD_REQUEST, "INVALID JSON", "Given JSON is invalid for required API payload: " + e.getMessage());

            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An error occurred during delete operation", e.getCause());
        }
    }

    public UploadInput getUploadInput(String broker, String fiscalCode, String uploadId, ServiceType serviceType) {
        String blobPath = String.format("/%s/%s/%s.json", fiscalCode, INPUT_DIRECTORY, uploadId);
        BinaryData binaryDataReport = blobStorageRepository.downloadContent(broker, uploadId, blobPath, serviceType);
        try {
            return objectMapper.readValue(binaryDataReport.toString(), UploadInput.class);
        } catch (JsonProcessingException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An error occurred during upload-input deserialization", e.getCause());
        }
    }

    public UploadReport getReportV1(String broker, String fiscalCode, String uploadKey, ServiceType serviceType) {
        String blobPath = String.format("/%s/%s/report%s.json", fiscalCode, OUTPUT_DIRECTORY, uploadKey);
        BinaryData binaryDataReport = blobStorageRepository.downloadContent(broker, uploadKey, blobPath, serviceType);

        try {
            return objectMapper.readValue(binaryDataReport.toString(), UploadReport.class);
        } catch (JsonProcessingException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An error occurred during report deserialization", e.getCause());
        }
    }

    public UploadReportDTO getReportV2(String broker, String fiscalCode, String uploadKey, ServiceType serviceType) {
        UploadReport uploadReport = getReportV1(broker, fiscalCode, uploadKey, serviceType);
        return UploadReportDTO.builder()
                .fileId(uploadReport.uploadID)
                .startTime(uploadReport.startTime)
                .endTime(uploadReport.endTime)
                .responses(responseEntryDTOMapper.toDTOs(uploadReport.responses))
                .submittedItem(uploadReport.submittedItem)
                .processedItem(uploadReport.processedItem)
                .build();
    }

    public String upload(UploadInput uploadInput, String broker, String organizationFiscalCode, int totalItem, ServiceType serviceType) {
        try {
            log.debug(String.format("Upload operation %s was launched for broker %s and organization fiscal code %s",
                    uploadInput.getUploadOperation(), broker, organizationFiscalCode));

            // from UploadInput Object to ByteArrayInputStream
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(objectMapper.writeValueAsBytes(uploadInput));

            // upload blob
            String fileId = blobStorageRepository.upload(broker, organizationFiscalCode, inputStream, serviceType);
            statusService.createUploadStatus(organizationFiscalCode, broker, fileId, totalItem, serviceType);

            return fileId;
        } catch (IOException e) {
            log.error("[Error][BlobService@upload] " + e.getMessage());
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL SERVER ERROR", "Internal server error", e.getCause());
        }
    }

    private InputStream unzip(CompletedFileUpload file) {
        int zipFiles = 0;
        int zipSize = 0;

        if (!VALID_UPLOAD_EXTENSION.contains(getFileExtension(file.getFilename()))) {
            log.error("[Error][BlobService@unzip] Invalid extension: " + file.getFilename());
            throw new AppException(HttpStatus.BAD_REQUEST, "NOT A ZIP FILE", "Only ZIP files can be uploaded.");
        }

        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                zipFiles++;
                if (zipFiles > zipMaxEntries) {
                    log.error("[Error][BlobService@unzip] Too many entries in ZIP");
                    throw new AppException(HttpStatus.BAD_REQUEST, "INVALID FILE", "Too many entries in ZIP file.");
                }

                if (entry.isDirectory()) {
                    continue; // Skip folders
                }

                String entryName = entry.getName();
                if (!ALLOWABLE_EXTENSIONS.contains(getFileExtension(entryName))) {
                    log.error("[Error][BlobService@unzip] Disallowed file type: " + entryName);
                    throw new AppException(HttpStatus.BAD_REQUEST, "INVALID FILE", "ZIP contains unsupported file type.");
                }

                // Read entry content into memory
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int len;

                while ((len = zis.read(buffer)) > 0) {
                    zipSize += len;
                    if (zipSize > zipMaxSize) {
                        zis.closeEntry();
                        zis.close();
                        log.error("[Error][BlobService@unzip] ZIP file too large");
                        throw new AppException(HttpStatus.BAD_REQUEST, "INVALID FILE", "Unzipped content exceeds size limit.");
                    }
                    baos.write(buffer, 0, len);
                }

                // Return the first valid file as InputStream
                zis.closeEntry();
                zis.close();
                log.debug("File with name " + file.getName() + " has been unzipped");
                return new ByteArrayInputStream(baos.toByteArray());
            }

            log.error("[Error][BlobService@unzip] No valid file in ZIP");
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID FILE", "No valid file found in ZIP.");
        }
        catch (EOFException e) {
            log.error("[Error][BlobService@unzip] Client input error: " + e.getMessage(), e);
            throw new AppException(HttpStatus.BAD_REQUEST, "UNZIP ERROR", "Could not unzip file");
        }
        catch (IOException e) {
            log.error("[Error][BlobService@unzip] " + e.getMessage(), e);
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "UNZIP ERROR", "Problem to manage zip file", e);
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
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        fileName = fileName.replace(".", uuid + ".");

        return fileName;
    }
}
