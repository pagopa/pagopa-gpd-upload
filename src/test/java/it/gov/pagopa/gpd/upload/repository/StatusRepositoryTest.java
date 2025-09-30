package it.gov.pagopa.gpd.upload.repository;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.*;
import com.azure.cosmos.util.CosmosPagedIterable;
import io.micronaut.http.HttpStatus;
import it.gov.pagopa.gpd.upload.entity.Status;
import it.gov.pagopa.gpd.upload.exception.AppException;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StatusRepositoryTest {

    private static final String STATUS_ID = "statusID";
    private static final String FISCAL_CODE = "fiscalCode";
    private static final String CONTINUATION_TOKEN = "continuationToken";
    private static final String NEXT_TOKEN = "nextToken";
    private static final int PAGE_SIZE = 10;
    private static final String FILE_ID = "fileId";
    private static final String BROKER_CODE = "brokerCode";
    private static final String ORG_FISCAL_CODE = "orgFiscalCode";
    private static final SqlQuerySpec QUERY_SPEC = new SqlQuerySpec();
    private static final CosmosQueryRequestOptions QUERY_REQUEST_OPTIONS = new CosmosQueryRequestOptions();

    CosmosContainer cosmosContainerMock = mock(CosmosContainer.class);
    StatusRepository statusRepository = new StatusRepository(cosmosContainerMock);

    @Test
    void saveStatus_OK() {
        Status status = Status.builder().build();
        CosmosItemResponse cosmosItemResponse = mock(CosmosItemResponse.class);
        when(cosmosItemResponse.getItem()).thenReturn(status);
        when(cosmosContainerMock.createItem(any())).thenReturn(cosmosItemResponse);

        assertDoesNotThrow(() -> statusRepository.saveStatus(status));
        verify(cosmosContainerMock, times(1)).createItem(any());
        verify(cosmosItemResponse, times(1)).getItem();
    }

    @Test
    void saveStatus_ConflictThenReturnStatus_OK() {
        Status status = Status.builder().id(STATUS_ID).fiscalCode(FISCAL_CODE).build();
        CosmosException exception = mock(CosmosException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.CONFLICT.getCode());
        when(cosmosContainerMock.createItem(any())).thenThrow(exception);

        CosmosItemResponse cosmosItemResponse = mock(CosmosItemResponse.class);
        when(cosmosItemResponse.getItem()).thenReturn(status);
        when(cosmosContainerMock.readItem(STATUS_ID, new PartitionKey(FISCAL_CODE), Status.class)).thenReturn(cosmosItemResponse);

        assertDoesNotThrow(() -> statusRepository.saveStatus(status));
        verify(cosmosContainerMock, times(1)).createItem(any());
        verify(cosmosContainerMock, times(1)).readItem(STATUS_ID, new PartitionKey(FISCAL_CODE), Status.class);
        verify(cosmosItemResponse, times(1)).getItem();
    }

    @Test
    void saveStatus_ConflictThenReturnStatus_INTERNAL_SERVER_ERROR_KO() {
        Status status = Status.builder().id(STATUS_ID).fiscalCode(FISCAL_CODE).build();
        CosmosException exception = mock(CosmosException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.CONFLICT.getCode());
        when(cosmosContainerMock.createItem(any())).thenThrow(exception);

        CosmosItemResponse cosmosItemResponse = mock(CosmosItemResponse.class);
        when(cosmosItemResponse.getItem()).thenReturn(status);
        CosmosException exceptionFindStatusById = mock(CosmosException.class);
        when(exceptionFindStatusById.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
        when(cosmosContainerMock.readItem(STATUS_ID, new PartitionKey(FISCAL_CODE), Status.class)).thenThrow(exceptionFindStatusById);

        assertThrows(AppException.class, () -> statusRepository.saveStatus(status));
        verify(cosmosContainerMock, times(1)).createItem(any());
    }

    @Test
    void saveStatus_ConflictThenReturnStatus_NOT_FOUND_KO() {
        Status status = Status.builder().id(STATUS_ID).fiscalCode(FISCAL_CODE).build();
        CosmosException exception = mock(CosmosException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.CONFLICT.getCode());
        when(cosmosContainerMock.createItem(any())).thenThrow(exception);

        CosmosItemResponse cosmosItemResponse = mock(CosmosItemResponse.class);
        when(cosmosItemResponse.getItem()).thenReturn(status);
        CosmosException exceptionFindStatusById = mock(CosmosException.class);
        when(exceptionFindStatusById.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND.getCode());
        when(cosmosContainerMock.readItem(STATUS_ID, new PartitionKey(FISCAL_CODE), Status.class)).thenThrow(exceptionFindStatusById);

        assertThrows(AppException.class, () -> statusRepository.saveStatus(status));
        verify(cosmosContainerMock, times(1)).createItem(any());
    }

    @Test
    void saveStatus_INTERNAL_SERVER_ERROR_KO() {
        Status status = Status.builder().id(STATUS_ID).fiscalCode(FISCAL_CODE).build();
        CosmosException exception = mock(CosmosException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
        when(cosmosContainerMock.createItem(any())).thenThrow(exception);

        assertThrows(AppException.class, () -> statusRepository.saveStatus(status));
        verify(cosmosContainerMock, times(1)).createItem(any());
    }

    @Test
    void saveStatus_GENERIC_ERROR_KO() {
        Status status = Status.builder().id(STATUS_ID).fiscalCode(FISCAL_CODE).build();
        CosmosException exception = mock(CosmosException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.BAD_GATEWAY.getCode());
        when(cosmosContainerMock.createItem(any())).thenThrow(exception);

        assertThrows(AppException.class, () -> statusRepository.saveStatus(status));
        verify(cosmosContainerMock, times(1)).createItem(any());
    }

    @Test
    void upsert_OK() {
        Status status = Status.builder().build();
        CosmosItemResponse cosmosItemResponse = mock(CosmosItemResponse.class);
        when(cosmosItemResponse.getItem()).thenReturn(status);
        when(cosmosContainerMock.upsertItem(any())).thenReturn(cosmosItemResponse);

        assertDoesNotThrow(() -> statusRepository.upsert(status));
        verify(cosmosContainerMock, times(1)).upsertItem(any());
        verify(cosmosItemResponse, times(1)).getItem();

    }

    @Test
    void upsert_ConflictThenReturnStatus_OK() {
        Status status = Status.builder().id(STATUS_ID).fiscalCode(FISCAL_CODE).build();
        CosmosException exception = mock(CosmosException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.CONFLICT.getCode());
        when(cosmosContainerMock.upsertItem(any())).thenThrow(exception);

        CosmosItemResponse cosmosItemResponse = mock(CosmosItemResponse.class);
        when(cosmosItemResponse.getItem()).thenReturn(status);
        when(cosmosContainerMock.readItem(STATUS_ID, new PartitionKey(FISCAL_CODE), Status.class)).thenReturn(cosmosItemResponse);

        assertDoesNotThrow(() -> statusRepository.upsert(status));
        verify(cosmosContainerMock, times(1)).upsertItem(any());
        verify(cosmosContainerMock, times(1)).readItem(STATUS_ID, new PartitionKey(FISCAL_CODE), Status.class);
        verify(cosmosItemResponse, times(1)).getItem();
    }

    @Test
    void upsert_ConflictThenReturnStatus_INTERNAL_SERVER_ERROR_KO() {
        Status status = Status.builder().id(STATUS_ID).fiscalCode(FISCAL_CODE).build();
        CosmosException exception = mock(CosmosException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.CONFLICT.getCode());
        when(cosmosContainerMock.upsertItem(any())).thenThrow(exception);

        CosmosItemResponse cosmosItemResponse = mock(CosmosItemResponse.class);
        when(cosmosItemResponse.getItem()).thenReturn(status);
        CosmosException exceptionFindStatusById = mock(CosmosException.class);
        when(exceptionFindStatusById.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
        when(cosmosContainerMock.readItem(STATUS_ID, new PartitionKey(FISCAL_CODE), Status.class)).thenThrow(exceptionFindStatusById);

        assertThrows(AppException.class, () -> statusRepository.upsert(status));
        verify(cosmosContainerMock, times(1)).upsertItem(any());
    }

    @Test
    void upsert_ConflictThenReturnStatus_NOT_FOUND_KO() {
        Status status = Status.builder().id(STATUS_ID).fiscalCode(FISCAL_CODE).build();
        CosmosException exception = mock(CosmosException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.CONFLICT.getCode());
        when(cosmosContainerMock.upsertItem(any())).thenThrow(exception);

        CosmosItemResponse cosmosItemResponse = mock(CosmosItemResponse.class);
        when(cosmosItemResponse.getItem()).thenReturn(status);
        CosmosException exceptionFindStatusById = mock(CosmosException.class);
        when(exceptionFindStatusById.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND.getCode());
        when(cosmosContainerMock.readItem(STATUS_ID, new PartitionKey(FISCAL_CODE), Status.class)).thenThrow(exceptionFindStatusById);

        assertThrows(AppException.class, () -> statusRepository.upsert(status));
        verify(cosmosContainerMock, times(1)).upsertItem(any());
    }

    @Test
    void upsert_INTERNAL_SERVER_ERROR_KO() {
        Status status = Status.builder().id(STATUS_ID).fiscalCode(FISCAL_CODE).build();
        CosmosException exception = mock(CosmosException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
        when(cosmosContainerMock.upsertItem(any())).thenThrow(exception);

        assertThrows(AppException.class, () -> statusRepository.upsert(status));
        verify(cosmosContainerMock, times(1)).upsertItem(any());
    }

    @Test
    void upsert_GENERIC_ERROR_KO() {
        Status status = Status.builder().id(STATUS_ID).fiscalCode(FISCAL_CODE).build();
        CosmosException exception = mock(CosmosException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.BAD_GATEWAY.getCode());
        when(cosmosContainerMock.upsertItem(any())).thenThrow(exception);

        assertThrows(AppException.class, () -> statusRepository.upsert(status));
        verify(cosmosContainerMock, times(1)).upsertItem(any());
    }

    @Test
    void find_OK() {
        Status status = Status.builder().build();
        CosmosPagedIterable cosmosPagedResponse = mock(CosmosPagedIterable.class);
        when(cosmosPagedResponse.stream()).thenReturn(Stream.of(status));
        when(cosmosContainerMock.queryItems(any(SqlQuerySpec.class), any(), any())).thenReturn(cosmosPagedResponse);

        assertDoesNotThrow(() -> statusRepository.find("query"));
        verify(cosmosContainerMock, times(1)).queryItems(any(SqlQuerySpec.class), any(), any());
    }

    @Test
    void find_NOT_FOUND_KO() {
        CosmosException exception = mock(CosmosException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND.getCode());
        when(cosmosContainerMock.queryItems(any(SqlQuerySpec.class), any(), any())).thenThrow(exception);

        assertThrows(AppException.class, () -> statusRepository.find("query"));
        verify(cosmosContainerMock, times(1)).queryItems(any(SqlQuerySpec.class), any(), any());
    }

    @Test
    void find_INTERNAL_SERVER_ERROR_KO() {
        CosmosException exception = mock(CosmosException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
        when(cosmosContainerMock.queryItems(any(SqlQuerySpec.class), any(), any())).thenThrow(exception);

        assertThrows(AppException.class, () -> statusRepository.find("query"));
        verify(cosmosContainerMock, times(1)).queryItems(any(SqlQuerySpec.class), any(), any());
    }

    @Test
    void find_WithOption_OK() {
        Status status = Status.builder().build();
        CosmosPagedIterable cosmosPagedResponse = mock(CosmosPagedIterable.class);
        when(cosmosPagedResponse.stream()).thenReturn(Stream.of(status));
        when(cosmosContainerMock.queryItems(any(SqlQuerySpec.class), any(), any())).thenReturn(cosmosPagedResponse);

        assertDoesNotThrow(() -> statusRepository.find(QUERY_SPEC, QUERY_REQUEST_OPTIONS));
        verify(cosmosContainerMock, times(1)).queryItems(any(SqlQuerySpec.class), any(), any());
    }

    @Test
    void find_WithOption_NOT_FOUND_KO() {
        CosmosException exception = mock(CosmosException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND.getCode());
        when(cosmosContainerMock.queryItems(any(SqlQuerySpec.class), any(), any())).thenThrow(exception);

        assertThrows(AppException.class, () -> statusRepository.find(QUERY_SPEC, QUERY_REQUEST_OPTIONS));
        verify(cosmosContainerMock, times(1)).queryItems(any(SqlQuerySpec.class), any(), any());
    }

    @Test
    void find_WithOption_INTERNAL_SERVER_ERROR_KO() {
        CosmosException exception = mock(CosmosException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
        when(cosmosContainerMock.queryItems(any(SqlQuerySpec.class), any(), any())).thenThrow(exception);

        assertThrows(AppException.class, () -> statusRepository.find(QUERY_SPEC, QUERY_REQUEST_OPTIONS));
        verify(cosmosContainerMock, times(1)).queryItems(any(SqlQuerySpec.class), any(), any());
    }

    @Test
    void findFileIdsPage_OK() {
        Iterable<FeedResponse<String>> page = mock(Iterable.class);
        Iterator<FeedResponse<String>> iterator = mock(Iterator.class);
        when(page.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(true);
        FeedResponse<String> feedResponse = mock(FeedResponse.class);
        when(feedResponse.getResults()).thenReturn(List.of(FILE_ID));
        when(feedResponse.getContinuationToken()).thenReturn(NEXT_TOKEN);
        when(iterator.next()).thenReturn(feedResponse);

        CosmosPagedIterable cosmosPagedResponse = mock(CosmosPagedIterable.class);
        when(cosmosPagedResponse.iterableByPage(CONTINUATION_TOKEN, PAGE_SIZE)).thenReturn(page);
        when(cosmosContainerMock.queryItems(any(SqlQuerySpec.class), any(), any())).thenReturn(cosmosPagedResponse);

        StatusRepository.FileIdsPage response = assertDoesNotThrow(() -> statusRepository.findFileIdsPage(BROKER_CODE, ORG_FISCAL_CODE, LocalDateTime.now(), LocalDateTime.now(), PAGE_SIZE, CONTINUATION_TOKEN, ServiceType.GPD));
        verify(cosmosContainerMock, times(1)).queryItems(any(SqlQuerySpec.class), any(), any());
        assertTrue(response.getFileIds().contains(FILE_ID));
        assertEquals(NEXT_TOKEN, response.getContinuationToken());
    }

    @Test
    void findFileIdsPage_INTERNAL_SERVER_ERROR_KO() {
        CosmosException exception = mock(CosmosException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR.getCode());

        when(cosmosContainerMock.queryItems(any(SqlQuerySpec.class), any(), any())).thenThrow(exception);

        assertThrows(AppException.class, () -> statusRepository.findFileIdsPage(BROKER_CODE, ORG_FISCAL_CODE, LocalDateTime.now(), LocalDateTime.now(), PAGE_SIZE, CONTINUATION_TOKEN, ServiceType.GPD));
        verify(cosmosContainerMock, times(1)).queryItems(any(SqlQuerySpec.class), any(), any());
    }

    @Test
    void findFileIdsPage_GENERIC_ERROR_KO() {
        CosmosException exception = mock(CosmosException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.BAD_GATEWAY.getCode());

        when(cosmosContainerMock.queryItems(any(SqlQuerySpec.class), any(), any())).thenThrow(exception);

        assertThrows(AppException.class, () -> statusRepository.findFileIdsPage(BROKER_CODE, ORG_FISCAL_CODE, LocalDateTime.now(), LocalDateTime.now(), PAGE_SIZE, CONTINUATION_TOKEN, ServiceType.GPD));
        verify(cosmosContainerMock, times(1)).queryItems(any(SqlQuerySpec.class), any(), any());
    }

}