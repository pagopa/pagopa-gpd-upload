package it.gov.pagopa.gpd.upload.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpHeaders;
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
import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.pagopa.gpd.upload.model.UploadReport;
import it.gov.pagopa.gpd.upload.model.UploadStatus;
import it.gov.pagopa.gpd.upload.model.ProblemJson;
import it.gov.pagopa.gpd.upload.service.StatusService;
import it.gov.pagopa.gpd.upload.service.BlobService;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


import java.net.URI;

@Tag(name = "File Upload API")
@ExecuteOn(TaskExecutors.IO)
@Controller()
@Slf4j
public class FileController {

    @Inject
    BlobService blobService;

    @Inject
    StatusService statusService;
    private final static String BASE_PATH = "brokers/{broker_code}/organizations/{organization_fiscal_code}/debtpositions/file";
    @Value("${post.file.response.headers.retry_after.millis}")
    private int retryAfter;

    @SneakyThrows
    @Operation(summary = "The Organization creates the debt positions listed in the file.", security = {@SecurityRequirement(name = "ApiKey"), @SecurityRequirement(name = "Authorization")}, operationId = "upload-debt-positions-file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Request accepted."),
            @ApiResponse(responseCode = "400", description = "Malformed request.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "401", description = "Wrong or missing function key.", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "409", description = "Conflict: duplicate file found.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "500", description = "Service unavailable.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class)))})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post(BASE_PATH)
    public HttpResponse uploadFile(
            @Parameter(description = "The broker code", required = true)
            @NotBlank @PathVariable(name = "broker_code") String brokerCode,
            @Parameter(description = "The organization fiscal code", required = true)
            @NotBlank @PathVariable(name = "organization_fiscal_code") String organizationFiscalCode,
            CompletedFileUpload file) {
        String uploadID = blobService.upload(brokerCode, organizationFiscalCode, file);
        log.debug("A file with name: " + file.getFilename() + " has been uploaded");

        String uri = "brokers/" + brokerCode + "/organizations/" + organizationFiscalCode +"/debtpositions/file/" + uploadID +"/status";
        HttpResponse response = HttpResponse.accepted(new URI(uri));

        return response.toMutableResponse()
                .header(HttpHeaders.RETRY_AFTER, retryAfter + " ms");
    }

    @SneakyThrows
    @Operation(summary = "Returns the debt positions upload status.", security = {@SecurityRequirement(name = "ApiKey"), @SecurityRequirement(name = "Authorization")}, operationId = "get-debt-positions-upload-status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Upload found.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UploadStatus.class))),
            @ApiResponse(responseCode = "400", description = "Malformed request.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "401", description = "Wrong or missing function key.", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Upload not found.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "500", description = "Service unavailable.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class)))})
    @Get(value = BASE_PATH + "/{fileID}/status",
            produces = MediaType.APPLICATION_JSON)
    HttpResponse<UploadStatus> getUploadStatus(
            @Parameter(description = "The broker code", required = true)
            @NotBlank @PathVariable(name = "broker_code") String brokerCode,
            @Parameter(description = "The organization fiscal code", required = true)
            @NotBlank @PathVariable(name = "organization_fiscal_code") String organizationFiscalCode,
            @Parameter(description = "The fiscal code of the Organization.", required = true)
            @NotBlank @PathVariable(name = "fileID") String fileID) {

        UploadStatus uploadStatus = statusService.getStatus(fileID, organizationFiscalCode);

        return HttpResponse.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(uploadStatus);
    }

    @Operation(summary = "Returns the debt positions upload report.", security = {@SecurityRequirement(name = "ApiKey"), @SecurityRequirement(name = "Authorization")}, operationId = "get-debt-positions-upload-report")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Upload report found.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UploadReport.class))),
            @ApiResponse(responseCode = "400", description = "Malformed request.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "401", description = "Wrong or missing function key.", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Upload report not found.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "500", description = "Service unavailable.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class)))})
    @Get(value = BASE_PATH + "/{fileID}/report",
            produces = MediaType.APPLICATION_JSON)
    HttpResponse<UploadReport> getUploadOutput(
            @Parameter(description = "The broker code", required = true)
            @NotBlank @PathVariable(name = "broker_code") String brokerCode,
            @Parameter(description = "The organization fiscal code", required = true)
            @NotBlank @PathVariable(name = "organization_fiscal_code") String organizationFiscalCode,
            @Parameter(description = "The fiscal code of the Organization.", required = true)
            @NotBlank @PathVariable(name = "fileID") String fileID) {

        UploadReport uploadReport = statusService.getReport(fileID, organizationFiscalCode);

        return HttpResponse.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(uploadReport);
    }
}
