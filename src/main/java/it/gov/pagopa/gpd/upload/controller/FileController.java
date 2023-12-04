package it.gov.pagopa.gpd.upload.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.Parameter;
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

    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post("/organizations/{organizationfiscalcode}/debtpositions/file")
    public HttpResponse uploadDebtPositionFile(@Parameter(description = "The organization fiscal code", required = true)
                                                   @NotBlank @PathVariable String organizationfiscalcode, CompletedFileUpload file) throws IOException {
        String key = fileUploadService.upload(organizationfiscalcode, file);
        log.debug("A file with name: " + file.getFilename() + " has been uploaded");

        return HttpResponse.status(HttpStatus.OK).body(key);
    }
}
