package it.gov.pagopa.gpd.upload.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import it.gov.pagopa.gpd.upload.model.FileStatus;
import it.gov.pagopa.gpd.upload.model.ProblemJson;
import it.gov.pagopa.gpd.upload.service.FileStatusService;
import it.gov.pagopa.gpd.upload.service.FileUploadService;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;


import java.io.IOException;

@ExecuteOn(TaskExecutors.IO)
@Controller()
@Slf4j
public class FileController {

    @Inject
    FileUploadService fileUploadService;

    @Inject
    FileStatusService fileStatusService;

    @Operation(summary = "The Organization creates the debt positions listed in the file.", security = {@SecurityRequirement(name = "ApiKey"), @SecurityRequirement(name = "Authorization")}, operationId = "createMassivePositions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Request created."),
            @ApiResponse(responseCode = "400", description = "Malformed request.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "401", description = "Wrong or missing function key.", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "409", description = "Conflict: duplicate debt position found.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "500", description = "Service unavailable.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class)))})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post("/organizations/{organizationFiscalCode}/debtpositions/file")
    public HttpResponse uploadDebtPositionFile(@Parameter(description = "The organization fiscal code", required = true)
                                                   @NotBlank @PathVariable String organizationFiscalCode, CompletedFileUpload file) throws IOException {
        String key = fileUploadService.upload(organizationFiscalCode, file);
        log.debug("A file with name: " + file.getFilename() + " has been uploaded");

        return HttpResponse.status(HttpStatus.OK)
                .body(key);
    }

    @Operation(summary = "Returns the upload status of debt positions uploaded via file.", security = {@SecurityRequirement(name = "ApiKey"), @SecurityRequirement(name = "Authorization")}, operationId = "createMassivePositions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Request created."),
            @ApiResponse(responseCode = "400", description = "Malformed request.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "401", description = "Wrong or missing function key.", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "409", description = "Conflict: duplicate debt position found.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "500", description = "Service unavailable.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class)))})
    @Get(value = "/organizations/{organizationFiscalCode}/debtpositions/file/{fileId}/status",
            produces = MediaType.APPLICATION_JSON)
    HttpResponse<FileStatus> getFileSatus(
            @Parameter(description = "The organization fiscal code", required = true)
            @NotBlank @PathVariable String organizationFiscalCode,
            @Parameter(description = "The fiscal code of the Organization.", required = true)
            @NotBlank @PathVariable String fileId) {

        FileStatus fileStatus = fileStatusService.getStatus(fileId);
        log.debug("The Status related to file-id " + fileId + " has been found");

        return HttpResponse.status(HttpStatus.OK)
                .body(fileStatus);
    }

}
