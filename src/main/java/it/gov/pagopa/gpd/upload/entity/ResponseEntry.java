package it.gov.pagopa.gpd.upload.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonSerialize
public class ResponseEntry {
    @Schema(example = "400")
    private Integer statusCode;
    @Schema(example = "Bad request caused by invalid email address")
    private String statusMessage;
    private List<String> requestIDs; // IUPDs
}
