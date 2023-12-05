package it.gov.pagopa.gpd.upload.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.Parameter;
import it.gov.pagopa.gpd.upload.model.FileStatus;
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

    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post("/organizations/{organizationFiscalCode}/debtpositions/file")
    public HttpResponse uploadDebtPositionFile(@Parameter(description = "The organization fiscal code", required = true)
                                                   @NotBlank @PathVariable String organizationFiscalCode, CompletedFileUpload file) throws IOException {
        String key = fileUploadService.upload(organizationFiscalCode, file);
        log.debug("A file with name: " + file.getFilename() + " has been uploaded");

        return HttpResponse.status(HttpStatus.OK)
                .body(key);
    }

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
