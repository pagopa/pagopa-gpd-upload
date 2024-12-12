package it.gov.pagopa.gpd.upload.controller.support;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.pagopa.gpd.upload.exception.AppException;
import it.gov.pagopa.gpd.upload.model.AppInfo;
import it.gov.pagopa.gpd.upload.model.ProblemJson;
import it.gov.pagopa.gpd.upload.model.UploadReport;
import it.gov.pagopa.gpd.upload.service.RecoveryService;
import it.gov.pagopa.gpd.upload.service.StatusService;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag(name = "Support API")
@ExecuteOn(TaskExecutors.IO)
@Controller("support")
public class SupportController {

    @Inject
    RecoveryService recoveryService;

    @Inject
    StatusService statusService;

    @Operation(summary = "Support API to recover status on CREATE and DELETE operation", description = "Returns the debt positions upload report recovered.", tags = {"Support API"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AppInfo.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "429", description = "Too many requests.", content = @Content(mediaType = MediaType.TEXT_JSON)),
            @ApiResponse(responseCode = "500", description = "Service unavailable", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class)))})
    @Get(value = "uploads/{upload}/status/refresh")
    public HttpResponse<UploadReport> recoverStatus(
            @Parameter(description = "The unique identifier for file upload", required = true)
            @NotBlank @PathVariable(name = "upload") String upload) {
        String[] strings = upload.split("_");

        if(strings.length < 3)
            throw new AppException(HttpStatus.BAD_REQUEST, "Recover bad request", "The upload UUID should be formatted as <broker>_<organization>_<id>");

        String broker = strings[0];
        String organization = strings[1];
        recoveryService.recover(broker, organization, upload);
        log.info("[Support-API] Status {} recovered", upload);
        UploadReport report = statusService.getReport(organization, upload);

        return HttpResponse.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(report);
    }
}
