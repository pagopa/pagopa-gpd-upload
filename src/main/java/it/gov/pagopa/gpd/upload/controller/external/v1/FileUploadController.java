package it.gov.pagopa.gpd.upload.controller.external.v1;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.openapi.annotation.OpenAPIGroup;
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
import it.gov.pagopa.gpd.upload.exception.AppError;
import it.gov.pagopa.gpd.upload.exception.AppException;
import it.gov.pagopa.gpd.upload.model.ProblemJson;
import it.gov.pagopa.gpd.upload.model.UploadOperation;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import it.gov.pagopa.gpd.upload.service.BlobService;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;

@Tag(name = "Massive operation APIs for Debt Positions - v1")
@ExecuteOn(TaskExecutors.IO)
@Controller()
@Slf4j
@OpenAPIGroup(exclude = "external-v2")
@SecurityScheme(name = "ApiKey", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER)
public class FileUploadController {
    @Inject
    BlobService blobService;
    private static final String BASE_PATH = "brokers/{broker-code}/organizations/{organization-fiscal-code}/debtpositions/file";
    @Value("${post.file.response.headers.retry_after.millis}")
    private int retryAfter;

    @Operation(summary = "The Organization creates the debt positions listed in the file.", security = {@SecurityRequirement(name = "ApiKey")}, operationId = "create-debt-positions-by-file-upload")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Request accepted."),
            @ApiResponse(responseCode = "400", description = "Malformed request.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "401", description = "Wrong or missing function key.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "409", description = "Conflict: duplicate file found.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "429", description = "Too many requests.", content = @Content(mediaType = MediaType.TEXT_JSON)),
            @ApiResponse(responseCode = "500", description = "Service unavailable.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class)))})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post(BASE_PATH)
    public HttpResponse createDebtPositionsByFileUpload(
            @Parameter(description = "The broker code", required = true)
            @NotBlank @PathVariable(name = "broker-code") String brokerCode,
            @Parameter(description = "The organization fiscal code", required = true)
            @NotBlank @PathVariable(name = "organization-fiscal-code") String organizationFiscalCode,
            @Parameter(
                    description = "File to be uploaded",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM)
            ) CompletedFileUpload file,
            @Parameter(description = "GPD or ACA", hidden = true) @QueryValue(defaultValue = "GPD") ServiceType serviceType
    ) {
        if (null == file)
            throw new AppException(HttpStatus.BAD_REQUEST, "EMPTY FILE", "The zip file is missing");
        String uploadID = blobService.upsert(brokerCode, organizationFiscalCode, UploadOperation.CREATE, file, serviceType);
        log.debug("[CREATE by file UPLOAD] A file with name: " + file.getFilename() + " has been uploaded");
        String uri = "brokers/" + brokerCode + "/organizations/" + organizationFiscalCode + "/debtpositions/file/" + uploadID + "/status";

        try {
            HttpResponse response = HttpResponse.accepted(new URI(uri));
            return response.toMutableResponse()
                    .header(HttpHeaders.RETRY_AFTER, retryAfter + " ms");
        } catch (URISyntaxException e) {
            throw new AppException(AppError.INTERNAL_ERROR);
        }
    }

    @Operation(summary = "The Organization updates the debt positions listed in the file.", security = {@SecurityRequirement(name = "ApiKey")}, operationId = "update-debt-positions-by-file-upload")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Request accepted."),
            @ApiResponse(responseCode = "400", description = "Malformed request.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "401", description = "Wrong or missing function key.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "409", description = "Conflict: duplicate file found.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "429", description = "Too many requests.", content = @Content(mediaType = MediaType.TEXT_JSON)),
            @ApiResponse(responseCode = "500", description = "Service unavailable.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class)))})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Put(BASE_PATH)
    public HttpResponse updateDebtPositionsByFileUpload(
            @Parameter(description = "The broker code", required = true)
            @NotBlank @PathVariable(name = "broker-code") String brokerCode,
            @Parameter(description = "The organization fiscal code", required = true)
            @NotBlank @PathVariable(name = "organization-fiscal-code") String organizationFiscalCode,
            @Parameter(
                    description = "File to be uploaded",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM)
            ) CompletedFileUpload file,
            @Parameter(description = "GPD or ACA", hidden = true) @QueryValue(defaultValue = "GPD") ServiceType serviceType
    ) {
        if (null == file)
            throw new AppException(HttpStatus.BAD_REQUEST, "EMPTY FILE", "The zip file is missing");
        String uploadID = blobService.upsert(brokerCode, organizationFiscalCode, UploadOperation.UPDATE, file, serviceType);
        log.debug("[UPDATE by file UPLOAD] A file with name: " + file.getFilename() + " has been uploaded");
        String uri = "brokers/" + brokerCode + "/organizations/" + organizationFiscalCode + "/debtpositions/file/" + uploadID + "/status";

        try {
            HttpResponse response = HttpResponse.accepted(new URI(uri));
            return response.toMutableResponse()
                    .header(HttpHeaders.RETRY_AFTER, retryAfter + " ms");
        } catch (URISyntaxException e) {
            throw new AppException(AppError.INTERNAL_ERROR);
        }
    }

    @Operation(summary = "The Organization deletes the debt positions based on IUPD listed in the file.", security = {@SecurityRequirement(name = "ApiKey")}, operationId = "delete-debt-positions-by-file-upload")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Request accepted."),
            @ApiResponse(responseCode = "400", description = "Malformed request.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "401", description = "Wrong or missing function key.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "409", description = "Conflict: duplicate file found.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "429", description = "Too many requests.", content = @Content(mediaType = MediaType.TEXT_JSON)),
            @ApiResponse(responseCode = "500", description = "Service unavailable.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class)))})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Delete(BASE_PATH)
    public HttpResponse deleteDebtPositionsByFileUpload(
            @Parameter(description = "The broker code", required = true)
            @NotBlank @PathVariable(name = "broker-code") String brokerCode,
            @Parameter(description = "The organization fiscal code", required = true)
            @NotBlank @PathVariable(name = "organization-fiscal-code") String organizationFiscalCode,
            @Parameter(
                    description = "File to be uploaded",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM)
            ) CompletedFileUpload file,
            @Parameter(description = "GPD or ACA", hidden = true) @QueryValue(defaultValue = "GPD") ServiceType serviceType
    ) {
        if (null == file)
            throw new AppException(HttpStatus.BAD_REQUEST, "EMPTY FILE", "The zip file is missing");
        String uploadID = blobService.delete(brokerCode, organizationFiscalCode, UploadOperation.DELETE, file, serviceType);
        log.debug("[DELETE by file UPLOAD] A file with name: " + file.getFilename() + " has been uploaded");
        String uri = "brokers/" + brokerCode + "/organizations/" + organizationFiscalCode + "/debtpositions/file/" + uploadID + "/status";

        try {
            HttpResponse response = HttpResponse.accepted(new URI(uri));
            return response.toMutableResponse()
                    .header(HttpHeaders.RETRY_AFTER, retryAfter + " ms");
        } catch (URISyntaxException e) {
            throw new AppException(AppError.INTERNAL_ERROR);
        }
    }
}
