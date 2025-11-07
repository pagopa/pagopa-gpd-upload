package it.gov.pagopa.gpd.upload;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.servers.ServerVariable;
import it.gov.pagopa.gpd.upload.model.pd.PaymentPositionsModel;


@OpenAPIDefinition(
        info = @Info(
                title = "${info.application.title}",
                version =  "${openapi.application.version}",
                description = "Microservice to manage PagoPA GPD Upload",
                termsOfService = "https://www.pagopa.gov.it/"
        ),
        servers = {
                @Server(
                        url = "http://localhost:8080"
                ),
                @Server(
                        url = "https://{host}{basePath}",
                        variables = {
                                @ServerVariable(
                                        name = "basePath",
                                        defaultValue = "/upload/gpd/debt-positions-service/v1",
                                        allowableValues = {
                                                "/upload/gpd/debt-positions-service/v1",
                                                "/upload/aca/debt-positions-service/v1"
                                        }
                                ),
                                @ServerVariable(
                                        name = "host",
                                        defaultValue = "api.dev.platform.pagopa.it",
                                        allowableValues = {
                                                "api.dev.platform.pagopa.it",
                                                "api.uat.platform.pagopa.it",
                                                "api.platform.pagopa.it"
                                        }
                                )
                        }
                )
        }
)
public class Application {

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }

//    @Schema(name = "PaymentPositionsModel", description = "Structure of debt positions model", implementation = PaymentPositionsModel.class)
//    static class IncludePaymentPositionsModel {}
}
