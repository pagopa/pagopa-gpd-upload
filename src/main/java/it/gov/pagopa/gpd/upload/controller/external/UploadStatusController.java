package it.gov.pagopa.gpd.upload.controller.external;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.pagopa.gpd.upload.exception.AppException;
import it.gov.pagopa.gpd.upload.model.ProblemJson;
import it.gov.pagopa.gpd.upload.model.UploadReport;
import it.gov.pagopa.gpd.upload.service.BlobService;
import it.gov.pagopa.gpd.upload.service.StatusService;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;

import static io.micronaut.http.HttpStatus.NOT_FOUND;

@Tag(name = "Upload Status API")
@ExecuteOn(TaskExecutors.IO)
@Controller()
@Slf4j
@SecurityScheme(name = "Ocp-Apim-Subscription-Key", type = SecuritySchemeType.APIKEY,  in = SecuritySchemeIn.HEADER)
public class UploadStatusController {
    @Inject
    BlobService blobService;
    @Inject
    StatusService statusService;
    private static final String BASE_PATH = "brokers/{broker-code}/organizations/{organization-fiscal-code}/debtpositions/file";

    @Operation(summary = "Returns the debt positions upload status.", security = {@SecurityRequirement(name = "ApiKey"), @SecurityRequirement(name = "Authorization")}, operationId = "get-debt-positions-upload-status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Upload found.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = it.gov.pagopa.gpd.upload.model.UploadStatus.class))),
            @ApiResponse(responseCode = "400", description = "Malformed request.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "401", description = "Wrong or missing function key.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Upload not found.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "429", description = "Too many requests.", content = @Content(mediaType = MediaType.TEXT_JSON)),
            @ApiResponse(responseCode = "500", description = "Service unavailable.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class)))})
    @Get(value = BASE_PATH + "/{file-id}/status",
            produces = MediaType.APPLICATION_JSON)
    HttpResponse<it.gov.pagopa.gpd.upload.model.UploadStatus> getUploadStatus(
            @Parameter(description = "The broker code", required = true)
            @NotBlank @PathVariable(name = "broker-code") String brokerCode,
            @Parameter(description = "The organization fiscal code", required = true)
            @NotBlank @PathVariable(name = "organization-fiscal-code") String organizationFiscalCode,
            @Parameter(description = "The unique identifier for file upload", required = true)
            @NotBlank @PathVariable(name = "file-id") String fileID) {

        it.gov.pagopa.gpd.upload.model.UploadStatus uploadStatus = statusService.getUploadStatus(fileID, organizationFiscalCode);

        return HttpResponse.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(uploadStatus);
    }

    @Operation(summary = "Returns the debt positions upload report.", security = {@SecurityRequirement(name = "ApiKey"), @SecurityRequirement(name = "Authorization")}, operationId = "get-debt-positions-upload-report")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Upload report found.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UploadReport.class))),
            @ApiResponse(responseCode = "400", description = "Malformed request.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "401", description = "Wrong or missing function key.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Upload report not found.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "429", description = "Too many requests.", content = @Content(mediaType = MediaType.TEXT_JSON)),
            @ApiResponse(responseCode = "500", description = "Service unavailable.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class)))})
    @Get(value = BASE_PATH + "/{file-id}/report",
            produces = MediaType.APPLICATION_JSON)
    HttpResponse<UploadReport> getUploadOutput(
            @Parameter(description = "The broker code", required = true)
            @NotBlank @PathVariable(name = "broker-code") String brokerCode,
            @Parameter(description = "The organization fiscal code", required = true)
            @NotBlank @PathVariable(name = "organization-fiscal-code") String organizationFiscalCode,
            @Parameter(description = "The unique identifier for file upload", required = true)
            @NotBlank @PathVariable(name = "file-id") String fileID) {

        UploadReport uploadReport = null;
        try {
            uploadReport = statusService.getReport(organizationFiscalCode, fileID);
        } catch (AppException e) {
            if(e.getHttpStatus() == NOT_FOUND) {
                uploadReport = blobService.getReport(brokerCode, organizationFiscalCode, fileID);
                if(uploadReport == null)
                    throw e;
            }
        }

        return HttpResponse.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(uploadReport);
    }
}
