package it.gov.pagopa.gpd.upload.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import it.gov.pagopa.gpd.upload.repository.BlobStorageRepository;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;


import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

@ExecuteOn(TaskExecutors.IO)
@Controller()
@Slf4j
public class FileController {

    @Inject
    BlobStorageRepository blobStorageRepository;

    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post("/debtpositions/file")
    public HttpResponse uploadDebtPositionFile(CompletedFileUpload file) throws IOException {
        log.info("A file with name: " + file.getFilename() + " has been uploaded");
        String key = blobStorageRepository.upload(file);

        return HttpResponse.status(HttpStatus.OK).body(key);
    }
}
