package it.gov.pagopa.gpd.upload.controller.external.v2;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
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
import it.gov.pagopa.gpd.upload.exception.AppException;
import it.gov.pagopa.gpd.upload.model.FileIdListResponse;
import it.gov.pagopa.gpd.upload.model.ProblemJson;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import it.gov.pagopa.gpd.upload.model.v1.UploadStatus;
import it.gov.pagopa.gpd.upload.model.v2.UploadReportDTO;
import it.gov.pagopa.gpd.upload.model.v2.UploadStatusDTO;
import it.gov.pagopa.gpd.upload.service.BlobService;
import it.gov.pagopa.gpd.upload.service.StatusService;
import it.gov.pagopa.gpd.upload.utils.CommonCheck;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;

import static io.micronaut.http.HttpStatus.NOT_FOUND;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Tag(name = "Massive operation observability APIs - v2")
@ExecuteOn(TaskExecutors.IO)
@Controller()
@Slf4j
@OpenAPIGroup(exclude = "external-v1")
@SecurityScheme(name = "ApiKey", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER)
public class CheckUploadController {
    @Inject
    BlobService blobService;
    @Inject
    StatusService statusService;

    private static final String BASE_PATH = "v2/brokers/{broker-code}/organizations/{organization-fiscal-code}/debtpositions/";

    @Operation(summary = "Returns the debt positions upload status.", security = {@SecurityRequirement(name = "ApiKey")}, operationId = "get-debt-positions-upload-status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Upload found.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UploadStatus.class))),
            @ApiResponse(responseCode = "400", description = "Malformed request.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "401", description = "Wrong or missing function key.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Upload not found.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "429", description = "Too many requests.", content = @Content(mediaType = MediaType.TEXT_JSON)),
            @ApiResponse(responseCode = "500", description = "Service unavailable.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class)))})
    @Get(value = BASE_PATH + "file/{file-id}/status",
            produces = MediaType.APPLICATION_JSON)
    HttpResponse<UploadStatusDTO> getUploadStatus(
            @Parameter(description = "The broker code", required = true)
            @NotBlank @PathVariable(name = "broker-code") String brokerCode,
            @Parameter(description = "The organization fiscal code", required = true)
            @NotBlank @PathVariable(name = "organization-fiscal-code") String organizationFiscalCode,
            @Parameter(description = "The unique identifier for file upload", required = true)
            @NotBlank @PathVariable(name = "file-id") String fileId,
            @Parameter(description = "GPD or ACA", hidden = true) @QueryValue(defaultValue = "GPD") ServiceType serviceType
    ) {
        UploadStatusDTO uploadStatus = statusService.getUploadStatusV2(brokerCode, fileId, organizationFiscalCode, serviceType);

        return HttpResponse.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(uploadStatus);
    }

    @Operation(summary = "Returns the debt positions upload report.", security = {@SecurityRequirement(name = "ApiKey")}, operationId = "get-debt-positions-upload-report")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Upload report found.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UploadReportDTO.class))),
            @ApiResponse(responseCode = "400", description = "Malformed request.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "401", description = "Wrong or missing function key.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Upload report not found.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "429", description = "Too many requests.", content = @Content(mediaType = MediaType.TEXT_JSON)),
            @ApiResponse(responseCode = "500", description = "Service unavailable.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class)))})
    @Get(value = BASE_PATH + "file/{file-id}/report",
            produces = MediaType.APPLICATION_JSON)
    HttpResponse<UploadReportDTO> getUploadReport(
            @Parameter(description = "The broker code", required = true)
            @NotBlank @PathVariable(name = "broker-code") String brokerCode,
            @Parameter(description = "The organization fiscal code", required = true)
            @NotBlank @PathVariable(name = "organization-fiscal-code") String organizationFiscalCode,
            @Parameter(description = "The unique identifier for file upload", required = true)
            @NotBlank @PathVariable(name = "file-id") String uploadID,
            @Parameter(description = "GPD or ACA", hidden = true) @QueryValue(defaultValue = "GPD") ServiceType serviceType
    ) {
        UploadReportDTO uploadReport = null;
        try {
            uploadReport = statusService.getReportV2(brokerCode, organizationFiscalCode, uploadID, serviceType);
        } catch (AppException e) {
            if (e.getHttpStatus() == NOT_FOUND) {
                uploadReport = blobService.getReportV2(brokerCode, organizationFiscalCode, uploadID, serviceType);
            }
            if (uploadReport == null)
                throw e;
        }

        return HttpResponse.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(uploadReport);
    }

    // =========================
    // PAGOPA-3282: Get File-Id list
    // =========================
    @Operation(
            summary = "Returns the list of fileIds for a broker/organization in the given date range (max 7 days).",
            security = {@SecurityRequirement(name = "ApiKey")},
            operationId = "get-debt-positions-fileids"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "FileIds retrieved.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FileIdListResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad request.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "401", description = "Wrong or missing function key.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "410", description = "Gone", 
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "500", description = "Service unavailable.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemJson.class)))
    })

    @Get(value = BASE_PATH + "files", produces = MediaType.APPLICATION_JSON)
    public HttpResponse<FileIdListResponse> getFileIdList(
            @Parameter(description = "The broker code", required = true)
            @NotBlank @PathVariable(name = "broker-code") String brokerCode,
            @Parameter(description = "The organization fiscal code", required = true)
            @NotBlank @PathVariable(name = "organization-fiscal-code") String organizationFiscalCode,
            @Parameter(description = "Start date (YYYY-MM-DD), Europe/Rome", required = false, example = "2025-09-01")
            @QueryValue(value = "from", defaultValue = "") String fromDateStr,
            @Parameter(description = "End date (YYYY-MM-DD), Europe/Rome", required = false, example = "2025-09-06")
            @QueryValue(value = "to", defaultValue = "") String toDateStr,
            @Parameter(description = "Max items per page (default 100, min 100, max 500)", required = false)
            @QueryValue(value = "size", defaultValue = "100") Integer size,
            @Parameter(description = "Continuation token (opaque). Pass it back to get the next page.", required = false)
            @Header("x-continuation-token") @Nullable String continuationToken,
            @Parameter(description = "GPD or ACA", hidden = true) @QueryValue(defaultValue = "GPD") ServiceType serviceType
    ) {

        // Parse & defaults for dates (calendar date, Europe/Rome), inclusive range [from, to]
        final LocalDate toDate = CommonCheck.parseOrDefaultToDate(toDateStr);
        final LocalDate fromDate = CommonCheck.parseOrDefaultFromDate(fromDateStr, toDate);

        // Validate range: from <= to and (to - from + 1) <= 7
        final long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        if (fromDate.isAfter(toDate) || days > 7) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "BAD_REQUEST",
                    "Invalid range: ensure 1 ≤ (to - from + 1) ≤ 7 and from ≤ to"
            );
        }

        // Validate size: default 100, min 100, max 500
        if (size == null || size < 100 || size > 500) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "BAD_REQUEST",
                    "Invalid size: must be between 100 and 500 (default 100)"
            );
        }
        
        // Retention check: reject ranges older than (now - 60-days)
        this.enforceRetention(fromDate, toDate);

        // Service call
        FileIdListResponse res = statusService.getFileIdList(
                brokerCode, organizationFiscalCode, fromDate, toDate, size, continuationToken, serviceType
        );

        // Prepare response
        MutableHttpResponse<FileIdListResponse> response = HttpResponse.ok(res)
                .contentType(MediaType.APPLICATION_JSON);

        // Set next continuation token only if present (hasMore)
        if (res != null && res.getContinuationToken() != null && !res.getContinuationToken().isEmpty()) {
            response = response.header("x-continuation-token", res.getContinuationToken());
        }

        return response;
    }
    
    // Checks retention window.
 	// - If the whole range is older than the cutoff  -> 410 GONE
 	// - If the range partially overlaps the cutoff   -> 400 BAD_REQUEST (ask client to shift 'from')
 	private void enforceRetention(LocalDate fromDate, LocalDate toDate) {
 	    final LocalDate today = LocalDate.now(ZoneId.of("Europe/Rome"));
 	    final LocalDate cutoffInclusive = today.minusDays(60);

 	    // Entire range is older than (or equal to) cutoff -> data has been purged
 	    // Meaning: toDate <= cutoffInclusive
 	    if (!toDate.isAfter(cutoffInclusive)) {
 	        throw new AppException(
 	                HttpStatus.GONE,
 	                "GONE",
 	                String.format(
 	                        "Requested range [%s, %s] is older than the 60-day retention (cutoff %s): data has been purged and is no longer available.",
 	                        fromDate, toDate, cutoffInclusive
 	                )
 	        );
 	    }

 	    // Partial overlap: advise client to shift 'from' to the cutoff or later
 	    // Meaning: fromDate < cutoffInclusive < toDate
 	    if (fromDate.isBefore(cutoffInclusive)) {
 	        throw new AppException(
 	                HttpStatus.BAD_REQUEST,
 	                "BAD_REQUEST",
 	                String.format(
 	                        "Requested 'from' (%s) is older than the 60-day retention (cutoff %s). Use from >= %s.",
 	                        fromDate, cutoffInclusive, cutoffInclusive
 	                )
 	        );
 	    }
 	}
}
