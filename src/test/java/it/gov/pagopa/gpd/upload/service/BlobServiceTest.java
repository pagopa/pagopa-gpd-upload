package it.gov.pagopa.gpd.upload.service;

import it.gov.pagopa.gpd.upload.exception.AppException;
import it.gov.pagopa.gpd.upload.model.UploadOperation;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import it.gov.pagopa.gpd.upload.model.pd.MultipleIUPDModel;
import it.gov.pagopa.gpd.upload.model.pd.PaymentPositionsModel;
import it.gov.pagopa.gpd.upload.repository.BlobStorageRepository;
import it.gov.pagopa.gpd.upload.utils.FileUtils;
import it.gov.pagopa.gpd.upload.utils.GPDValidator;
import it.gov.pagopa.gpd.upload.utils.ResponseEntryDTOMapperImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BlobServiceTest {
    private static final String FISCAL_CODE = "fiscal-code";
    private static final String BROKER_CODE = "broker-code";

    private static BlobStorageRepository blobStorageRepository = mock(BlobStorageRepository.class);
    private static StatusService statusService = mock(StatusService.class);
    private static BlobService blobService = new BlobService(blobStorageRepository, statusService, new GPDValidator<PaymentPositionsModel>(), new GPDValidator<MultipleIUPDModel>(), new ResponseEntryDTOMapperImpl());

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

    @Test
    void upsert_OK() throws IOException {
        when(blobStorageRepository.upload(anyString(), anyString(), any(), any())).thenReturn(FISCAL_CODE);

        String uploadKey = blobService.upsert(BROKER_CODE, FISCAL_CODE, UploadOperation.CREATE, FileUtils.getUpsertFile(), ServiceType.GPD);

        Assertions.assertEquals(FISCAL_CODE, uploadKey);
    }

    @Test
    void upsert_InvalidPaymentPosition_KO() throws FileNotFoundException {
        when(blobStorageRepository.upload(anyString(), anyString(), any(), any())).thenReturn(FISCAL_CODE);

        assertThrows(AppException.class, () -> blobService.upsert(BROKER_CODE, FISCAL_CODE, UploadOperation.CREATE, FileUtils.getUpsertFileInvalidPaymentPosition(), ServiceType.GPD)) ;
    }

    @Test
    void upsert_InvalidFile_KO() throws FileNotFoundException {
        when(blobStorageRepository.upload(anyString(), anyString(), any(), any())).thenReturn(FISCAL_CODE);

        assertThrows(AppException.class, () -> blobService.upsert(BROKER_CODE, FISCAL_CODE, UploadOperation.CREATE, FileUtils.getDeleteFile(), ServiceType.GPD)) ;
    }

    @Test
    void delete_OK() throws IOException {
        when(blobStorageRepository.upload(anyString(), anyString(), any(), any())).thenReturn(FISCAL_CODE);

        String uploadKey = blobService.delete(BROKER_CODE, FISCAL_CODE, UploadOperation.CREATE, FileUtils.getDeleteFile(), ServiceType.GPD);

        Assertions.assertEquals(FISCAL_CODE, uploadKey);
    }

    @Test
    void delete_InvalidMultipleIupd_KO() throws FileNotFoundException {
        when(blobStorageRepository.upload(anyString(), anyString(), any(), any())).thenReturn(FISCAL_CODE);

        assertThrows(AppException.class, () -> blobService.delete(BROKER_CODE, FISCAL_CODE, UploadOperation.CREATE, FileUtils.getDeleteFileInvalidMultipleIUPD(), ServiceType.GPD)) ;
    }

    @Test
    void delete_InvalidFile_KO() throws FileNotFoundException {
        when(blobStorageRepository.upload(anyString(), anyString(), any(), any())).thenReturn(FISCAL_CODE);

        assertThrows(AppException.class, () -> blobService.delete(BROKER_CODE, FISCAL_CODE, UploadOperation.CREATE, FileUtils.getUpsertFile(), ServiceType.GPD)) ;
    }
}
