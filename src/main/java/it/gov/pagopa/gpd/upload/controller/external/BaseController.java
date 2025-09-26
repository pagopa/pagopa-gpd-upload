package it.gov.pagopa.gpd.upload.controller.external;

import io.micronaut.context.annotation.AliasFor;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import it.gov.pagopa.gpd.upload.model.AppInfo;
import it.gov.pagopa.gpd.upload.model.ProblemJson;

@ExecuteOn(TaskExecutors.IO)
@Controller()
public class BaseController {
    @Value("${info.application.artifactId}")
    private String name;

    @Value("${info.application.version}")
    private String version;

    @Value("${info.properties.environment}")
    private String environment;


    @Hidden
    @Get
    public HttpResponse<?> base(){
        return HttpResponse.seeOther(UriBuilder.of("/swagger-ui/").build());
    }

    @Operation(summary = "health check", description = "Return OK if application is started", tags = {"Health check"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AppInfo.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "429", description = "Too many requests.", content = @Content(mediaType = MediaType.TEXT_JSON)),
            @ApiResponse(responseCode = "500", description = "Service unavailable", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class)))})
    @Get(value = "/info")
    public HttpResponse<AppInfo> healthCheck() {
        // Used just for health checking
        AppInfo info = AppInfo.builder()
                .name(name)
                .version(version)
                .environment(environment)
                .build();
        return HttpResponse.status(HttpStatus.OK).body(info);
    }

    @Hidden
    @Get(uris = {"/v1/info","/v2/info"})
    public HttpResponse<AppInfo> healthCheckVersions() {
        return healthCheck();
    }

}
