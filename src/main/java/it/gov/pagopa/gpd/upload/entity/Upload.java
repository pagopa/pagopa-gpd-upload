package it.gov.pagopa.gpd.upload.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.micronaut.http.HttpStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonSerialize
@ToString
public class Upload {
    private int current;
    private int total;
    @Builder.Default
    private ResponseEntry ok = new ResponseEntry(HttpStatus.OK.getCode(), getDetail(HttpStatus.OK), new ArrayList<>());
    @Builder.Default
    private ResponseEntry created = new ResponseEntry(HttpStatus.CREATED.getCode(), getDetail(HttpStatus.CREATED), new ArrayList<>());
    @Builder.Default
    private ResponseEntry badRequest = new ResponseEntry(HttpStatus.BAD_REQUEST.getCode(), getDetail(HttpStatus.BAD_REQUEST), new ArrayList<>());
    @Builder.Default
    private ResponseEntry notFound = new ResponseEntry(HttpStatus.NOT_FOUND.getCode(), getDetail(HttpStatus.NOT_FOUND), new ArrayList<>());
    @Builder.Default
    private ResponseEntry conflict = new ResponseEntry(HttpStatus.CONFLICT.getCode(), getDetail(HttpStatus.CONFLICT), new ArrayList<>());
    @Builder.Default
    private ResponseEntry serverError = new ResponseEntry(HttpStatus.INTERNAL_SERVER_ERROR.getCode(), getDetail(HttpStatus.INTERNAL_SERVER_ERROR), new ArrayList<>());
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime start;
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime end;

    public static String getDetail(HttpStatus status) {
        return switch (status) {
            case CREATED -> "Debt position CREATED";
            case OK -> "Debt position operation OK";
            case NOT_FOUND -> "Debt position NOT FOUND";
            case CONFLICT -> "Debt position IUPD or NAV/IUV already exists for organization code";
            case UNAUTHORIZED -> "UNAUTHORIZED";
            case FORBIDDEN -> "FORBIDDEN";
            case INTERNAL_SERVER_ERROR -> "Internal Server Error: operation not completed";
            case BAD_REQUEST -> "Bad request";
            default -> status.toString();
        };
    }
}
