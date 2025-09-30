package it.gov.pagopa.gpd.upload.controller;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Primary;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import it.gov.pagopa.gpd.upload.model.v1.UploadReport;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import it.gov.pagopa.gpd.upload.repository.BlobStorageRepository;
import it.gov.pagopa.gpd.upload.repository.StatusRepository;
import it.gov.pagopa.gpd.upload.service.RecoveryService;
import it.gov.pagopa.gpd.upload.service.StatusService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static io.micronaut.http.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@MicronautTest
class SupportControllerTest {

    private static final String URI = "support/uploads/broker_organization_uid/status/refresh";
    private static final String BAD_URI = "support/uploads/broker-organization-uid/status/refresh";

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void recoverStatus_OK() {
        HttpRequest<?> httpRequest = HttpRequest.create(HttpMethod.GET, URI);
        HttpResponse<?> response = client.toBlocking().exchange(httpRequest);

        assertNotNull(response);
        assertEquals(OK, response.getStatus());
    }

    @Test
    void recoverStatus_KO() {
        HttpRequest<?> httpRequest = HttpRequest.create(HttpMethod.GET, BAD_URI);
        assertThrows(HttpClientException.class, () -> client.toBlocking().exchange(httpRequest));
    }

    ////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////MOCK//////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////

    @Bean
    @Primary
    public StatusService statusService() throws IOException {
        StatusService statusService = Mockito.mock(StatusService.class);
        Mockito.when(statusService.getReportV1(anyString(), anyString(), anyString(), any())).thenReturn(UploadReport.builder().build());
        return statusService;
    }

    @Bean
    @Primary
    public RecoveryService recoveryService() {
        RecoveryService recoveryService = Mockito.mock(RecoveryService.class);
        Mockito.when(recoveryService.recover(anyString(), anyString(), anyString(), any(ServiceType.class))).thenReturn(true);
        return recoveryService;
    }

    // real repositories are out of scope for this test, @PostConstruct init routine requires connection-string
    @Bean
    @Primary
    public StatusRepository statusRepository() {
        return Mockito.mock(StatusRepository.class);
    }
    @Bean
    @Primary
    public BlobStorageRepository blobStorageRepository() {
        return Mockito.mock(BlobStorageRepository.class);
    }
}
