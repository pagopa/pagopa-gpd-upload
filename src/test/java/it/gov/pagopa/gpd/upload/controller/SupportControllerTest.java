package it.gov.pagopa.gpd.upload.controller;

import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import it.gov.pagopa.gpd.upload.model.ProblemJson;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import it.gov.pagopa.gpd.upload.model.v1.UploadReport;
import it.gov.pagopa.gpd.upload.service.SupportService;
import it.gov.pagopa.gpd.upload.service.StatusService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;

import static io.micronaut.http.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@MicronautTest
class SupportControllerTest {

    private static final String uri = "support/uploads/broker_organization_uid/status/refresh";
    private static final String badUri = "support/uploads/broker-organization-uid/status/refresh";
    private static final String monitoringUri = "support/monitoring";

    @Inject
    @Client("/")
    HttpClient client;
    @Inject
    StatusService statusService;
    @Inject
    SupportService supportService;

    @BeforeEach
    void beforeEach() {
        Mockito.when(statusService.getReportV1(anyString(), anyString(), anyString(), any())).thenReturn(UploadReport.builder().build());
        Mockito.when(supportService.recover(anyString(), anyString(), anyString(), any(ServiceType.class))).thenReturn(true);
        Mockito.when(supportService.monitoring(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(ProblemJson.builder().build());
    }

    @Test
    void recoverStatus_OK() {
        HttpRequest<?> httpRequest = HttpRequest.create(HttpMethod.GET, uri);
        HttpResponse<?> response = client.toBlocking().exchange(httpRequest);

        assertNotNull(response);
        assertEquals(OK, response.getStatus());
    }

    @Test
    void recoverStatus_KO() {
        HttpRequest<?> httpRequest = HttpRequest.create(HttpMethod.GET, badUri);
        assertThrows(HttpClientException.class, () -> client.toBlocking().exchange(httpRequest));
    }

    @Test
    void monitoring_okNoParams() {
        HttpRequest<?> httpRequest = HttpRequest.create(HttpMethod.GET, monitoringUri);
        HttpResponse<?> response = client.toBlocking().exchange(httpRequest);

        assertNotNull(response);
        assertEquals(OK, response.getStatus());
    }

    @Test
    void monitoring_okFromTo() {
        HttpRequest<?> httpRequest = HttpRequest.create(HttpMethod.GET, monitoringUri + "?from=2024-01-01&to=2024-01-02");
        HttpResponse<?> response = client.toBlocking().exchange(httpRequest);

        assertNotNull(response);
        assertEquals(OK, response.getStatus());
    }

    @Test
    void monitoring_okFrom() {
        HttpRequest<?> httpRequest = HttpRequest.create(HttpMethod.GET, monitoringUri + "?from=2024-01-01");
        HttpResponse<?> response = client.toBlocking().exchange(httpRequest);

        assertNotNull(response);
        assertEquals(OK, response.getStatus());
    }

    @Test
    void monitoring_okTo() {
        HttpRequest<?> httpRequest = HttpRequest.create(HttpMethod.GET, monitoringUri + "?to=2099-01-01");
        HttpResponse<?> response = client.toBlocking().exchange(httpRequest);

        assertNotNull(response);
        assertEquals(OK, response.getStatus());
    }

    @Test
    void monitoring_ko() {
        HttpRequest<?> httpRequest = HttpRequest.create(HttpMethod.GET, monitoringUri + "?to=2024-01-01");
        assertThrows(HttpClientResponseException.class, () -> client.toBlocking().exchange(httpRequest));
    }
}
