package it.gov.pagopa.gpd.upload.service;

import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.multipart.CompletedFileUpload;
import it.gov.pagopa.gpd.upload.entity.ResponseEntry;
import it.gov.pagopa.gpd.upload.exception.AppException;
import it.gov.pagopa.gpd.upload.model.UploadInput;
import it.gov.pagopa.gpd.upload.model.UploadOperation;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import it.gov.pagopa.gpd.upload.model.pd.MultipleIUPDModel;
import it.gov.pagopa.gpd.upload.model.pd.PaymentPositionsModel;
import it.gov.pagopa.gpd.upload.model.v1.UploadReport;
import it.gov.pagopa.gpd.upload.model.v2.UploadReportDTO;
import it.gov.pagopa.gpd.upload.repository.BlobStorageRepository;
import it.gov.pagopa.gpd.upload.utils.FileUtils;
import it.gov.pagopa.gpd.upload.utils.GPDValidator;
import it.gov.pagopa.gpd.upload.utils.ResponseEntryDTOMapperImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static it.gov.pagopa.gpd.upload.utils.Constants.INPUT_DIRECTORY;
import static it.gov.pagopa.gpd.upload.utils.Constants.OUTPUT_DIRECTORY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class BlobServiceTest {
    private static final String FISCAL_CODE = "fiscal-code";
    private static final String BROKER_CODE = "broker-code";
    private static final String FILE_ID = "fileId";
    private static final String IUPD_1 = "IUPD1";
    private static final String IUPD_2 = "IUPD2";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final BinaryData binaryData = mock(BinaryData.class);
    private static final BlobStorageRepository blobStorageRepository = mock(BlobStorageRepository.class);
    private static final StatusService statusService = mock(StatusService.class);
    private static final BlobService blobService = new BlobService(blobStorageRepository, statusService, new GPDValidator<PaymentPositionsModel>(), new GPDValidator<MultipleIUPDModel>(), new ResponseEntryDTOMapperImpl());

    @BeforeAll
    static void beforeAll() throws NoSuchFieldException, IllegalAccessException {
        Field zipMaxSizeField = BlobService.class.getDeclaredField("zipMaxSize");
        zipMaxSizeField.setAccessible(true);
        zipMaxSizeField.setInt(blobService, 1048576);

        Field zipMaxEntriesField = BlobService.class.getDeclaredField("zipMaxEntries");
        zipMaxEntriesField.setAccessible(true);
        zipMaxEntriesField.setInt(blobService, 2);

        blobService.init();
    }

    @BeforeEach
    void beforeEach(){
        reset(blobStorageRepository, statusService);
    }

    @Test
    void upsert_OK() throws IOException {
        when(blobStorageRepository.upload(anyString(), anyString(), any(), any())).thenReturn(FISCAL_CODE);
        CompletedFileUpload file = FileUtils.getUpsertFile();
        String uploadKey = blobService.upsert(BROKER_CODE, FISCAL_CODE, UploadOperation.CREATE, file, ServiceType.GPD);

        Assertions.assertEquals(FISCAL_CODE, uploadKey);
    }

    @Test
    void upsert_InvalidPaymentPosition_KO() throws IOException {
        when(blobStorageRepository.upload(anyString(), anyString(), any(), any())).thenReturn(FISCAL_CODE);
        CompletedFileUpload file = FileUtils.getUpsertFileInvalidPaymentPosition();

        assertThrows(AppException.class, () -> blobService.upsert(BROKER_CODE, FISCAL_CODE, UploadOperation.CREATE, file, ServiceType.GPD));
    }

    @Test
    void upsert_InvalidFile_KO() throws IOException {
        when(blobStorageRepository.upload(anyString(), anyString(), any(), any())).thenReturn(FISCAL_CODE);
        CompletedFileUpload file = FileUtils.getDeleteFile();

        assertThrows(AppException.class, () -> blobService.upsert(BROKER_CODE, FISCAL_CODE, UploadOperation.CREATE, file, ServiceType.GPD));
    }

    @Test
    void delete_OK() throws IOException {
        when(blobStorageRepository.upload(anyString(), anyString(), any(), any())).thenReturn(FISCAL_CODE);
        CompletedFileUpload file = FileUtils.getDeleteFile();
        String uploadKey = blobService.delete(BROKER_CODE, FISCAL_CODE, UploadOperation.CREATE, file, ServiceType.GPD);

        Assertions.assertEquals(FISCAL_CODE, uploadKey);
    }

    @Test
    void delete_InvalidMultipleIupd_KO() throws IOException {
        when(blobStorageRepository.upload(anyString(), anyString(), any(), any())).thenReturn(FISCAL_CODE);
        CompletedFileUpload file = FileUtils.getDeleteFileInvalidMultipleIUPD();
        assertThrows(AppException.class, () -> blobService.delete(BROKER_CODE, FISCAL_CODE, UploadOperation.CREATE, file, ServiceType.GPD));
    }

    @Test
    void delete_InvalidFile_KO() throws IOException {
        when(blobStorageRepository.upload(anyString(), anyString(), any(), any())).thenReturn(FISCAL_CODE);
        CompletedFileUpload file = FileUtils.getUpsertFile();

        assertThrows(AppException.class, () -> blobService.delete(BROKER_CODE, FISCAL_CODE, UploadOperation.CREATE, file, ServiceType.GPD));
    }

    @Test
    void getReportV1_OK() throws JsonProcessingException {
        UploadReport uploadReport = UploadReport.builder().uploadID(FILE_ID).build();
        when(binaryData.toString()).thenReturn(objectMapper.writeValueAsString(uploadReport));

        when(blobStorageRepository.downloadContent(BROKER_CODE, FILE_ID, String.format("/%s/%s/report%s.json", FISCAL_CODE, OUTPUT_DIRECTORY, FILE_ID), ServiceType.GPD)).thenReturn(binaryData);

        UploadReport response = blobService.getReportV1(BROKER_CODE, FISCAL_CODE, FILE_ID, ServiceType.GPD);
        assertEquals(uploadReport.getUploadID(), response.getUploadID());
        verify(blobStorageRepository, times(1)).downloadContent(BROKER_CODE, FILE_ID, String.format("/%s/%s/report%s.json", FISCAL_CODE, OUTPUT_DIRECTORY, FILE_ID), ServiceType.GPD);
    }

    @Test
    void getReportV2_OK() throws JsonProcessingException {
        UploadReport uploadReport = UploadReport.builder()
                .uploadID(FILE_ID)
                .responses(new ArrayList<>(List.of(
                        ResponseEntry.builder().requestIDs(Collections.singletonList(IUPD_1)).statusCode(HttpStatus.OK.getCode()).build(),
                        ResponseEntry.builder().requestIDs(Collections.singletonList(IUPD_2)).statusCode(HttpStatus.INTERNAL_SERVER_ERROR.getCode()).build()
                )))
                .build();
        when(binaryData.toString()).thenReturn(objectMapper.writeValueAsString(uploadReport));

        when(blobStorageRepository.downloadContent(BROKER_CODE, FILE_ID, String.format("/%s/%s/report%s.json", FISCAL_CODE, OUTPUT_DIRECTORY, FILE_ID), ServiceType.GPD)).thenReturn(binaryData);

        UploadReportDTO response = blobService.getReportV2(BROKER_CODE, FISCAL_CODE, FILE_ID, ServiceType.GPD);
        assertEquals(uploadReport.getUploadID(), response.getFileId());
        Assertions.assertTrue(response.getResponses().get(0).getIupds().contains(IUPD_1));
        Assertions.assertTrue(response.getResponses().get(1).getIupds().contains(IUPD_2));
        verify(blobStorageRepository, times(1)).downloadContent(BROKER_CODE, FILE_ID, String.format("/%s/%s/report%s.json", FISCAL_CODE, OUTPUT_DIRECTORY, FILE_ID), ServiceType.GPD);
    }

    @Test
    void getUploadInput_OK() throws JsonProcessingException {
        UploadInput uploadInput = UploadInput.builder().paymentPositionIUPDs(List.of(IUPD_1, IUPD_2)).build();
        when(binaryData.toString()).thenReturn(objectMapper.writeValueAsString(uploadInput));

        when(blobStorageRepository.downloadContent(BROKER_CODE, FILE_ID, String.format("/%s/%s/%s.json", FISCAL_CODE, INPUT_DIRECTORY, FILE_ID), ServiceType.GPD)).thenReturn(binaryData);

        UploadInput response = blobService.getUploadInput(BROKER_CODE, FISCAL_CODE, FILE_ID, ServiceType.GPD);
        assertEquals(uploadInput.getPaymentPositionIUPDs().get(0), response.getPaymentPositionIUPDs().get(0));
        assertEquals(uploadInput.getPaymentPositionIUPDs().get(1), response.getPaymentPositionIUPDs().get(1));
        verify(blobStorageRepository, times(1)).downloadContent(BROKER_CODE, FILE_ID, String.format("/%s/%s/%s.json", FISCAL_CODE, INPUT_DIRECTORY, FILE_ID), ServiceType.GPD);
    }
}
