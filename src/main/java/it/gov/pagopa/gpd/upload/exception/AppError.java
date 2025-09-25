package it.gov.pagopa.gpd.upload.exception;

import io.micronaut.http.HttpStatus;
import lombok.Getter;


@Getter
public enum AppError {

    ORGANIZATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Organization not found", "Not found the Organization Fiscal Code %s"),
    ORGANIZATION_DUPLICATED(HttpStatus.CONFLICT, "Organization with the specified organization fiscal code already exists in the system", "Already exists an organization with Organization Fiscal Code %s"),
    BLOB_NOT_FOUND(HttpStatus.NOT_FOUND, "Upload blob not found", "The blob for the given upload id does not exist"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Error enrollment processing ", "Error enrollment processing  [Organization Fiscal Code = %s]"),
    UNKNOWN(null, "UnknownError", "Unknown error");


    public final HttpStatus httpStatus;
    public final String title;
    public final String details;


    AppError(HttpStatus httpStatus, String title, String details) {
        this.httpStatus = httpStatus;
        this.title = title;
        this.details = details;
    }
}
