package it.gov.pagopa.gpd.upload.client;

import io.micronaut.core.async.annotation.SingleResult;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.client.annotation.Client;
import it.gov.pagopa.gpd.upload.model.pd.PaymentPositionModel;

import static io.micronaut.http.HttpHeaders.ACCEPT;
import static io.micronaut.http.HttpHeaders.USER_AGENT;

@Client(id = "${gpd.client.url}")
@Header(name = USER_AGENT, value = "Micronaut-GPD-Upload")
@Header(name = ACCEPT, value = "application/json")
public interface GPDClient {

    @Get("/organizations/{organizationfiscalcode}/debtpositions/{iupd}")
    @SingleResult
    HttpResponse<PaymentPositionModel> getDebtPosition(
            @PathVariable String organizationfiscalcode,
            @PathVariable String iupd);
}