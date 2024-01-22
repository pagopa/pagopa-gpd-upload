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
import java.text.SimpleDateFormat;

@ExecuteOn(TaskExecutors.IO)
@Controller()
@Slf4j
public class FileController {

    @Inject
    BlobService blobService;

    @Inject
    StatusService statusService;

    @Value("${post.file.response.headers.retry_after.millis}")
    private int retryAfter;
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-ddHH:mm:ss.SSS"));
    }

    @SneakyThrows
    @Operation(summary = "The Organization creates the debt positions listed in the file.", security = {@SecurityRequirement(name = "ApiKey"), @SecurityRequirement(name = "Authorization")}, operationId = "upload-debt-positions-file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Request accepted."),
            @ApiResponse(responseCode = "400", description = "Malformed request.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "401", description = "Wrong or missing function key.", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "409", description = "Conflict: duplicate file found.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "500", description = "Service unavailable.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class)))})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post("brokers/{broker-code}/organizations/{organization-fiscal-code}/debtpositions/file")
    public HttpResponse uploadFile(
            @Parameter(description = "The broker code", required = true)
            @NotBlank @PathVariable(name = "broker-code") String brokerCode,
            @Parameter(description = "The organization fiscal code", required = true)
            @NotBlank @PathVariable(name = "organization-fiscal-code") String organizationFiscalCode,
            CompletedFileUpload file) {
        String key = blobService.upload(brokerCode, organizationFiscalCode, file);
        log.debug("A file with name: " + file.getFilename() + " has been uploaded");

        String uri = "brokers/" + brokerCode + "/organizations/" + organizationFiscalCode +"/debtpositions/file/" + key +"/status";
        HttpResponse response = HttpResponse.accepted(new URI(uri));

        return response.toMutableResponse().header(HttpHeaders.RETRY_AFTER, retryAfter + " ms");
    }

    @SneakyThrows
    @Operation(summary = "Returns the upload status of debt positions uploaded via file.", security = {@SecurityRequirement(name = "ApiKey"), @SecurityRequirement(name = "Authorization")}, operationId = "get-debt-positions-upload-status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Upload found.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UploadStatus.class))),
            @ApiResponse(responseCode = "400", description = "Malformed request.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "401", description = "Wrong or missing function key.", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Upload not found.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "500", description = "Service unavailable.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class)))})
    @Get(value = "brokers/{broker-code}/organizations/{organization-fiscal-code}/debtpositions/file/{fileId}/status",
            produces = MediaType.APPLICATION_JSON)
    HttpResponse<UploadStatus> getUploadStatus(
            @Parameter(description = "The broker code", required = true)
            @NotBlank @PathVariable(name = "broker-code") String brokerCode,
            @Parameter(description = "The organization fiscal code", required = true)
            @NotBlank @PathVariable(name = "organization-fiscal-code") String organizationFiscalCode,
            @Parameter(description = "The fiscal code of the Organization.", required = true)
            @NotBlank @PathVariable String fileId) {

        UploadStatus uploadStatus = statusService.getStatus(fileId, organizationFiscalCode);

        return HttpResponse.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(uploadStatus);
    }

    @Operation(summary = "Returns the result of debt positions upload.", security = {@SecurityRequirement(name = "ApiKey"), @SecurityRequirement(name = "Authorization")}, operationId = "get-debt-positions-upload-result")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Upload result found.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UploadReport.class))),
            @ApiResponse(responseCode = "400", description = "Malformed request.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "401", description = "Wrong or missing function key.", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Upload result not found.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "500", description = "Service unavailable.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class)))})
    @Get(value = "brokers/{broker-code}/organizations/{organization-fiscal-code}/debtpositions/file/{fileId}/output",
            produces = MediaType.APPLICATION_JSON)
    HttpResponse<UploadReport> getUploadOutput(
            @Parameter(description = "The broker code", required = true)
            @NotBlank @PathVariable(name = "broker-code") String brokerCode,
            @Parameter(description = "The organization fiscal code", required = true)
            @NotBlank @PathVariable(name = "organization-fiscal-code") String organizationFiscalCode,
            @Parameter(description = "The fiscal code of the Organization.", required = true)
            @NotBlank @PathVariable String fileId) {

        UploadReport uploadReport = statusService.getReport(fileId, organizationFiscalCode);

        return HttpResponse.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(uploadReport);
    }
}
